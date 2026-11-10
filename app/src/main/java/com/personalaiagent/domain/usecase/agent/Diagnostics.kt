package com.personalaiagent.domain.usecase.agent

import kotlinx.serialization.json.Json

/**
 * On-device checks for a file the agent just wrote or is about to hand back.
 *
 * There is **no compiler and no linter on a phone**, so this is deliberately modest:
 * it catches the mistakes that actually break a file in this app — illegal JSON,
 * unbalanced brackets, unterminated strings, duplicate resource names, broken
 * placeholders — and it says plainly that it is not a build.
 */
object Diagnostics {

    data class Issue(val line: Int, val severity: String, val message: String)

    data class Report(
        val path: String,
        val kind: String,
        val lines: Int,
        val characters: Int,
        val issues: List<Issue>,
    ) {
        val ok: Boolean get() = issues.none { it.severity == ERROR }

        fun render(): String = buildString {
            append("Static check of $path ($kind, $lines lines, $characters chars)\n")
            append("note: this is not a compiler — it checks shape, not meaning.\n")
            if (issues.isEmpty()) {
                append("No structural problems found.")
            } else {
                issues.take(40).forEach { issue ->
                    append("- ${issue.severity} line ${issue.line}: ${issue.message}\n")
                }
                if (issues.size > 40) append("- … ${issues.size - 40} more\n")
            }
        }
    }

    private const val ERROR = "error"
    private const val WARN = "warning"

    fun check(path: String, content: String, json: Json = Json): Report {
        val kind = kindOf(path)
        val issues = mutableListOf<Issue>()
        when (kind) {
            "json" -> checkJson(content, json, issues)
            "xml" -> checkXml(content, issues)
            "kotlin", "java", "javascript", "typescript", "css" -> checkBrackets(content, issues)
            else -> checkBrackets(content, issues)
        }
        issueUnterminatedStrings(content, kind, issues)
        if (content.isEmpty()) {
            issues += Issue(0, WARN, "the file is empty")
        }
        if (content.contains('\u0000')) {
            issues += Issue(0, ERROR, "the file contains NUL bytes — it is not text")
        }
        if (content.isNotEmpty() && !content.endsWith("\n") && kind in CODE_KINDS) {
            issues += Issue(content.count { it == '\n' } + 1, WARN, "no trailing newline")
        }
        return Report(
            path = path,
            kind = kind,
            lines = if (content.isEmpty()) 0 else content.count { it == '\n' } + 1,
            characters = content.length,
            issues = issues.sortedBy { it.line },
        )
    }

    fun kindOf(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
        "json" -> "json"
        "xml" -> "xml"
        "kt", "kts" -> "kotlin"
        "java" -> "java"
        "js", "jsx", "mjs" -> "javascript"
        "ts", "tsx" -> "typescript"
        "css" -> "css"
        "md", "markdown" -> "markdown"
        "csv" -> "csv"
        "yml", "yaml" -> "yaml"
        else -> "text"
    }

    private fun checkJson(content: String, json: Json, issues: MutableList<Issue>) {
        if (content.isBlank()) {
            issues += Issue(1, ERROR, "empty JSON document")
            return
        }
        val result = runCatching {
            json.parseToJsonElement(content)
        }
        result.exceptionOrNull()?.let { error ->
            issues += Issue(1, ERROR, "invalid JSON: ${error.message?.take(200)}")
        }
    }

    private fun checkXml(content: String, issues: MutableList<Issue>) {
        val stack = ArrayDeque<String>()
        var line = 1
        var index = 0
        while (index < content.length) {
            val char = content[index]
            if (char == '\n') line++
            if (char != '<') {
                index++
                continue
            }
            val end = content.indexOf('>', index)
            if (end < 0) {
                issues += Issue(line, ERROR, "tag is not closed")
                return
            }
            val tag = content.substring(index + 1, end).trim()
            when {
                tag.startsWith("?") || tag.startsWith("!") || tag.endsWith("/") -> Unit
                tag.startsWith("/") -> {
                    val name = tag.removePrefix("/").trim()
                    if (stack.isEmpty()) {
                        issues += Issue(line, ERROR, "closing </$name> without an opening tag")
                    } else if (stack.last() != name) {
                        issues += Issue(line, ERROR, "</$name> closes <${stack.last()}>")
                        stack.removeLastOrNull()
                    } else {
                        stack.removeLastOrNull()
                    }
                }
                else -> stack.addLast(tag.substringBefore(' ').substringBefore('/'))
            }
            index = end + 1
        }
        stack.lastOrNull()?.let { issues += Issue(line, ERROR, "<$it> is never closed") }
        val names = Regex("<string name=\"([^\"]+)\"").findAll(content)
            .groupingBy { it.groupValues[1] }.eachCount()
        names.filter { it.value > 1 }.forEach { (name, count) ->
            issues += Issue(1, ERROR, "duplicate string resource \"$name\" ($count times)")
        }
    }

    private fun checkBrackets(content: String, issues: MutableList<Issue>) {
        val stack = ArrayDeque<Pair<Char, Int>>()
        var line = 1
        var index = 0
        var inString = false
        var inLineComment = false
        var inBlockComment = false
        while (index < content.length) {
            val char = content[index]
            val next = content.getOrNull(index + 1)
            when {
                char == '\n' -> {
                    line++
                    inLineComment = false
                }
                inLineComment -> Unit
                inBlockComment -> if (char == '*' && next == '/') {
                    inBlockComment = false
                    index++
                }
                inString -> when {
                    char == '\\' -> index++
                    char == '"' -> inString = false
                }
                char == '/' && next == '/' -> inLineComment = true
                char == '/' && next == '*' -> {
                    inBlockComment = true
                    index++
                }
                char == '"' -> inString = true
                char in "([{" -> stack.addLast(char to line)
                char in ")]}" -> {
                    val expected = when (char) {
                        ')' -> '('
                        ']' -> '['
                        else -> '{'
                    }
                    val last = stack.removeLastOrNull()
                    if (last == null) {
                        issues += Issue(line, ERROR, "unexpected '$char'")
                    } else if (last.first != expected) {
                        issues += Issue(line, ERROR, "'$char' closes '${last.first}' opened on line ${last.second}")
                    }
                }
            }
            index++
        }
        stack.lastOrNull()?.let { issues += Issue(it.second, ERROR, "'${it.first}' is never closed") }
        if (inString) issues += Issue(line, ERROR, "a string is never closed")
    }

    private fun issueUnterminatedStrings(content: String, kind: String, issues: MutableList<Issue>) {
        if (kind !in CODE_KINDS) return
        val quotes = content.count { it == '"' }
        if (quotes % 2 != 0) {
            issues += Issue(content.count { it == '\n' } + 1, ERROR, "an odd number of quotes (unterminated string)")
        }
    }

    private val CODE_KINDS = setOf("kotlin", "java", "javascript", "typescript", "json", "css", "yaml")
}
