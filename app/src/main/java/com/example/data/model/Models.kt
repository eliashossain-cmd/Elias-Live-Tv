package com.example.data.model

data class ParsedChannel(
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String = "General",
    val tvgId: String? = null,
    val tvgName: String? = null,
    val rawExtinf: String? = null
)

data class ParseResult(
    val channels: List<ParsedChannel>,
    val totalEntriesParsed: Int,
    val validChannelsCount: Int,
    val skippedEntriesCount: Int,
    val detectedGroups: Set<String>,
    val warnings: List<String> = emptyList()
)
