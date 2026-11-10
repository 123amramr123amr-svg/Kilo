package com.personalaiagent.platform

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Inflater

/**
 * Pulls the text layer out of a PDF **on the phone**, with no library and no upload.
 *
 * This is what makes a normal (typed) PDF readable by *any* model, whatever its
 * page count: the text is extracted once, cheaply, and sent as text instead of
 * images. A scanned PDF has no text layer, so it is detected here and handed to
 * the rasteriser instead — the caller decides, this class only reports.
 *
 * It reads the file itself (bounded), inflates the content streams with the JDK's
 * zlib and decodes the text operators (`Tj`, `TJ`, `'`, `"`). It is deliberately
 * forgiving: an unknown filter is skipped, not fatal.
 */
object PdfTextExtractor {

    data class Result(
        val blocks: List<String>,
        /** How many content streams were found in the file. */
        val streams: Int,
        /** Fraction (0..1) of the extracted characters that look like real text. */
        val quality: Double,
        /** True when the file was larger than the read limit. */
        val truncated: Boolean,
    ) {
        val characters: Int get() = blocks.sumOf { it.length }

        /**
         * A scanned PDF (or one with broken encodings) scores low here: its
         * streams hold no readable text, only drawing operators.
         */
        val usable: Boolean
            get() = characters >= PdfTextExtractor.MIN_USABLE_CHARS &&
                quality >= PdfTextExtractor.MIN_QUALITY

        /** `--- page 1 ---` blocks, capped at [maxChars]. */
        fun asDocumentText(maxChars: Int = PdfTextExtractor.MAX_CHARS): String {
            val out = StringBuilder()
            blocks.forEachIndexed { index, block ->
                if (out.length >= maxChars) return@forEachIndexed
                val clean = block.trim()
                if (clean.isEmpty()) return@forEachIndexed
                val room = maxChars - out.length
                out.append("--- page ").append(index + 1).append(" ---\n")
                out.append(if (clean.length <= room) clean else clean.take(room))
                out.append('\n')
            }
            return out.toString().trim()
        }
    }

    fun extract(file: File, maxBytes: Int = MAX_BYTES): Result {
        if (!file.isFile) return Result(emptyList(), 0, 0.0, truncated = false)
        val truncated = file.length() > maxBytes
        val bytes = runCatching {
            file.inputStream().use { stream ->
                // readNBytes is API 33+; this works from minSdk 26.
                val buffer = ByteArray(maxBytes)
                var total = 0
                while (total < buffer.size) {
                    val read = stream.read(buffer, total, buffer.size - total)
                    if (read <= 0) break
                    total += read
                }
                buffer.copyOf(total)
            }
        }.getOrNull() ?: return Result(emptyList(), 0, 0.0, truncated)

        val blocks = mutableListOf<String>()
        var streams = 0
        var index = 0
        while (index < bytes.size) {
            val start = indexOf(bytes, STREAM, index)
            if (start < 0) break
            var cursor = start + STREAM.size
            // Skip the EOL that must follow the keyword.
            if (cursor < bytes.size && bytes[cursor] == '\r'.code.toByte()) cursor++
            if (cursor < bytes.size && bytes[cursor] == '\n'.code.toByte()) cursor++
            val end = indexOf(bytes, END_STREAM, cursor)
            if (end < 0) break
            streams++
            val raw = bytes.copyOfRange(cursor, end)
            val decoded = inflate(raw) ?: raw
            val text = textOf(decoded)
            if (text.isNotBlank()) blocks += text
            index = end + END_STREAM.size
        }

        val all = blocks.joinToString("")
        return Result(
            blocks = blocks,
            streams = streams,
            quality = qualityOf(all),
            truncated = truncated,
        )
    }

    // ------------------------------------------------------------------ internals

    /** Inflates a Flate stream; null when the stream is not compressed. */
    private fun inflate(raw: ByteArray): ByteArray? {
        if (raw.isEmpty()) return null
        val inflater = Inflater()
        return try {
            inflater.setInput(raw)
            val out = ByteArrayOutputStream(maxOf(raw.size * 3, 1_024))
            val buffer = ByteArray(8_192)
            var guard = 0
            while (!inflater.finished() && guard < MAX_INFLATE_CHUNKS) {
                val read = inflater.inflate(buffer)
                if (read == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) break
                    guard++
                    continue
                }
                out.write(buffer, 0, read)
                guard++
            }
            out.toByteArray().takeIf { it.isNotEmpty() }
        } catch (error: Exception) {
            null
        } finally {
            inflater.end()
        }
    }

    /** Decodes the text-showing operators of a content stream. */
    private fun textOf(content: ByteArray): String {
        val out = StringBuilder()
        var index = 0
        var pendingLineBreak = false

        fun flushBreak() {
            if (pendingLineBreak && out.isNotEmpty() && out.last() != '\n') out.append('\n')
            pendingLineBreak = false
        }

        while (index < content.size && out.length < MAX_BLOCK_CHARS) {
            val byte = content[index].toInt() and 0xFF
            when {
                byte == '('.code -> {
                    val (text, next) = readLiteralString(content, index + 1)
                    flushBreak()
                    out.append(text)
                    index = next
                }

                byte == '<'.code && index + 1 < content.size && content[index + 1] != '<'.code.toByte() -> {
                    val (text, next) = readHexString(content, index + 1)
                    flushBreak()
                    out.append(text)
                    index = next
                }

                // Td, TD, T*, ', " all move to a new line; ET ends the block.
                byte == 'T'.code && index + 1 < content.size &&
                    (content[index + 1] == 'd'.code.toByte() ||
                        content[index + 1] == 'D'.code.toByte() ||
                        content[index + 1] == '*'.code.toByte()) -> {
                    pendingLineBreak = true
                    index += 2
                }

                byte == 'E'.code && index + 1 < content.size &&
                    content[index + 1] == 'T'.code.toByte() -> {
                    pendingLineBreak = true
                    index += 2
                }

                else -> index++
            }
        }
        return out.toString()
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun readLiteralString(content: ByteArray, from: Int): Pair<String, Int> {
        val text = StringBuilder()
        var index = from
        var depth = 1
        while (index < content.size) {
            val byte = content[index].toInt() and 0xFF
            when {
                byte == '\\'.code -> {
                    index++
                    if (index >= content.size) break
                    when (val escape = content[index].toInt() and 0xFF) {
                        'n'.code -> text.append('\n')
                        'r'.code -> text.append('\r')
                        't'.code -> text.append('\t')
                        'b'.code, 'f'.code -> Unit
                        '('.code -> text.append('(')
                        ')'.code -> text.append(')')
                        '\\'.code -> text.append('\\')
                        in '0'.code..'7'.code -> {
                            // Octal escape: up to three digits.
                            var value = 0
                            var digits = 0
                            var cursor = index
                            while (cursor < content.size && digits < 3) {
                                val digit = content[cursor].toInt() and 0xFF
                                if (digit !in '0'.code..'7'.code) break
                                value = value * 8 + (digit - '0'.code)
                                cursor++
                                digits++
                            }
                            if (digits > 0) {
                                text.append(value.toChar())
                                index = cursor - 1
                            }
                        }
                        else -> text.append(escape.toChar())
                    }
                    index++
                }

                byte == '('.code -> {
                    depth++
                    text.append('(')
                    index++
                }

                byte == ')'.code -> {
                    depth--
                    index++
                    if (depth == 0) break
                    text.append(')')
                }

                byte == '\r'.code || byte == '\n'.code -> {
                    text.append('\n')
                    index++
                }

                else -> {
                    if (byte in 32..126 || byte in 0xA0..0xFF || byte >= 0x80) {
                        text.append(byte.toChar())
                    }
                    index++
                }
            }
        }
        return text.toString() to index
    }

    private fun readHexString(content: ByteArray, from: Int): Pair<String, Int> {
        val hex = StringBuilder()
        var index = from
        while (index < content.size && content[index] != '>'.code.toByte()) {
            val char = (content[index].toInt() and 0xFF).toChar()
            if (char.isLetterOrDigit()) hex.append(char)
            index++
        }
        if (index < content.size) index++
        if (hex.length < 2 || hex.length % 2 != 0) return "" to index
        val bytes = ByteArray(hex.length / 2) { position ->
            hex.substring(position * 2, position * 2 + 2).toInt(16).toByte()
        }
        // Two-byte CID text is common in CJK/Arabic PDFs: keep the low byte.
        return String(bytes, Charsets.ISO_8859_1) to index
    }

    /** How much of the output looks like words rather than operators. */
    private fun qualityOf(text: String): Double {
        if (text.isEmpty()) return 0.0
        val good = text.count { char ->
            char.isLetterOrDigit() || char.isWhitespace() ||
                char in ".,;:!?()[]{}'\"-/—–،؛؟«»"
        }
        return good.toDouble() / text.length
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray, from: Int): Int {
        if (needle.isEmpty() || haystack.size < needle.size) return -1
        var index = from.coerceAtLeast(0)
        val last = haystack.size - needle.size
        outer@ while (index <= last) {
            for (offset in needle.indices) {
                if (haystack[index + offset] != needle[offset]) {
                    index++
                    continue@outer
                }
            }
            return index
        }
        return -1
    }

    private val STREAM = "stream".toByteArray()
    private val END_STREAM = "endstream".toByteArray()

    /** Read limit: a 1 GB scan is still handled, its text layer is at the front. */
    const val MAX_BYTES = 64 * 1024 * 1024
    const val MAX_CHARS = 180_000
    private const val MAX_BLOCK_CHARS = 60_000
    private const val MAX_INFLATE_CHUNKS = 4_000
    private const val MIN_USABLE_CHARS = 24
    private const val MIN_QUALITY = 0.75
}
