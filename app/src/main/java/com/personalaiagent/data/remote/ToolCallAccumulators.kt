package com.personalaiagent.data.remote

import com.personalaiagent.domain.model.ToolCall
import java.util.SortedMap
import java.util.TreeMap

/**
 * Accumulates streamed OpenAI `delta.tool_calls` fragments.
 * OpenAI sends the id/name once and the JSON arguments in many small pieces.
 */
class OpenAiToolCallAccumulator {

    private data class Builder(
        var id: String = "",
        var name: String = "",
        val arguments: StringBuilder = StringBuilder(),
    )

    private val builders: SortedMap<Int, Builder> = TreeMap()

    fun add(index: Int?, id: String?, name: String?, argumentsFragment: String?) {
        val key = index ?: builders.keys.maxOrNull() ?: 0
        val builder = builders.getOrPut(key) { Builder() }
        if (!id.isNullOrBlank()) builder.id = id
        if (!name.isNullOrBlank()) builder.name = name
        if (!argumentsFragment.isNullOrBlank()) builder.arguments.append(argumentsFragment)
    }

    fun isEmpty(): Boolean = builders.isEmpty()

    fun build(): List<ToolCall> = builders.entries.mapNotNull { (index, builder) ->
        if (builder.name.isBlank()) return@mapNotNull null
        ToolCall(
            id = builder.id.ifBlank { "call_$index" },
            name = builder.name,
            arguments = builder.arguments.toString().ifBlank { "{}" },
        )
    }
}

/** Accumulates Anthropic `content_block_start` + `input_json_delta` fragments. */
class AnthropicToolCallAccumulator {

    private data class Builder(
        val id: String,
        val name: String,
        val arguments: StringBuilder = StringBuilder(),
    )

    private val builders = mutableMapOf<Int, Builder>()

    fun start(index: Int, id: String?, name: String?) {
        if (name.isNullOrBlank()) return
        builders[index] = Builder(id = id ?: "tool_$index", name = name)
    }

    fun appendJson(index: Int, fragment: String?) {
        if (fragment.isNullOrEmpty()) return
        builders[index]?.arguments?.append(fragment)
    }

    fun isEmpty(): Boolean = builders.isEmpty()

    fun build(): List<ToolCall> = builders.entries
        .sortedBy { it.key }
        .map { (_, builder) ->
            ToolCall(
                id = builder.id,
                name = builder.name,
                arguments = builder.arguments.toString().ifBlank { "{}" },
            )
        }
}
