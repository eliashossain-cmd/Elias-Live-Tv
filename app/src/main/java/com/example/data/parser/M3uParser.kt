package com.example.data.parser

import com.example.data.model.ParseResult
import com.example.data.model.ParsedChannel
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.nio.charset.StandardCharsets

object M3uParser {

    private val ATTRIBUTE_REGEX = Regex("""([a-zA-Z0-9_-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s,]+))""")

    /**
     * Parses M3U or M3U8 content from an InputStream with UTF-8 support and BOM handling.
     * @param inputStream The stream to read from
     * @param baseUrl Optional base URL for resolving relative stream URLs
     */
    fun parse(inputStream: InputStream, baseUrl: String? = null): ParseResult {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        return parseReader(reader, baseUrl)
    }

    /**
     * Parses M3U or M3U8 content from a raw text String.
     */
    fun parse(content: String, baseUrl: String? = null): ParseResult {
        // Strip BOM if present at start of string
        val cleaned = if (content.startsWith("\uFEFF")) {
            content.substring(1)
        } else {
            content
        }
        val reader = BufferedReader(cleaned.reader())
        return parseReader(reader, baseUrl)
    }

    private fun parseReader(reader: BufferedReader, baseUrl: String?): ParseResult {
        val channels = mutableListOf<ParsedChannel>()
        val detectedGroups = mutableSetOf<String>()
        val warnings = mutableListOf<String>()

        var totalEntriesParsed = 0
        var skippedCount = 0

        var currentExtinf: String? = null
        var currentTvgId: String? = null
        var currentTvgName: String? = null
        var currentLogoUrl: String? = null
        var currentGroupTitle: String? = null
        var currentChannelName: String? = null

        var line = reader.readLine()
        var isFirstLine = true

        while (line != null) {
            var trimmed = line.trim()

            // Remove UTF-8 BOM on first line if present
            if (isFirstLine) {
                if (trimmed.startsWith("\uFEFF")) {
                    trimmed = trimmed.substring(1).trim()
                }
                isFirstLine = false
            }

            if (trimmed.isEmpty()) {
                line = reader.readLine()
                continue
            }

            if (trimmed.startsWith("#EXTM3U", ignoreCase = true)) {
                // Header line, can contain global attributes or playlist title
                line = reader.readLine()
                continue
            }

            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                totalEntriesParsed++
                currentExtinf = trimmed

                val parsedMetadata = parseExtinfLine(trimmed)
                currentTvgId = parsedMetadata.tvgId
                currentTvgName = parsedMetadata.tvgName
                currentLogoUrl = parsedMetadata.logoUrl
                currentGroupTitle = parsedMetadata.groupTitle
                currentChannelName = parsedMetadata.channelName

                line = reader.readLine()
                continue
            }

            // Check for intervening group tags like #EXTGRP
            if (trimmed.startsWith("#EXTGRP:", ignoreCase = true)) {
                val grp = trimmed.substringAfter(":").trim()
                if (grp.isNotEmpty() && (currentGroupTitle == null || currentGroupTitle == "General")) {
                    currentGroupTitle = grp
                }
                line = reader.readLine()
                continue
            }

            // Other comment or metadata directives (e.g., #EXTVLCOPT, #KODIPROP, #EXT-X-...)
            if (trimmed.startsWith("#")) {
                line = reader.readLine()
                continue
            }

            // If we reached here, the line is a potential stream URL
            val streamCandidate = trimmed
            val resolvedUrl = resolveUrl(streamCandidate, baseUrl)

            if (isValidStreamUrl(resolvedUrl)) {
                val group = currentGroupTitle?.ifBlank { "General" } ?: "General"
                val name = (currentChannelName?.ifBlank { null }
                    ?: currentTvgName?.ifBlank { null }
                    ?: extractNameFromUrl(resolvedUrl)).trim()

                detectedGroups.add(group)

                channels.add(
                    ParsedChannel(
                        name = name,
                        streamUrl = resolvedUrl,
                        logoUrl = currentLogoUrl?.takeIf { it.isNotBlank() },
                        groupTitle = group,
                        tvgId = currentTvgId?.takeIf { it.isNotBlank() },
                        tvgName = currentTvgName?.takeIf { it.isNotBlank() },
                        rawExtinf = currentExtinf
                    )
                )

                // Reset per-channel state
                currentExtinf = null
                currentTvgId = null
                currentTvgName = null
                currentLogoUrl = null
                currentGroupTitle = null
                currentChannelName = null
            } else {
                skippedCount++
                if (warnings.size < 20) {
                    warnings.add("Skipped invalid stream URL: ${streamCandidate.take(80)}")
                }
            }

            line = reader.readLine()
        }

        return ParseResult(
            channels = channels,
            totalEntriesParsed = totalEntriesParsed,
            validChannelsCount = channels.size,
            skippedEntriesCount = skippedCount,
            detectedGroups = detectedGroups,
            warnings = warnings
        )
    }

    private data class ExtinfMetadata(
        val tvgId: String?,
        val tvgName: String?,
        val logoUrl: String?,
        val groupTitle: String?,
        val channelName: String?
    )

    private fun parseExtinfLine(line: String): ExtinfMetadata {
        val colonIdx = line.indexOf(':')
        if (colonIdx == -1) {
            return ExtinfMetadata(null, null, null, null, null)
        }

        val content = line.substring(colonIdx + 1).trim()
        val commaIdx = content.indexOf(',')

        val attributesPart = if (commaIdx != -1) content.substring(0, commaIdx) else content
        val channelNamePart = if (commaIdx != -1) content.substring(commaIdx + 1).trim() else null

        var tvgId: String? = null
        var tvgName: String? = null
        var logoUrl: String? = null
        var groupTitle: String? = null

        ATTRIBUTE_REGEX.findAll(attributesPart).forEach { match ->
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2].ifEmpty {
                match.groupValues[3].ifEmpty { match.groupValues[4] }
            }.trim()

            when (key) {
                "tvg-id" -> tvgId = value
                "tvg-name" -> tvgName = value
                "tvg-logo" -> logoUrl = value
                "group-title" -> groupTitle = value
            }
        }

        return ExtinfMetadata(
            tvgId = tvgId,
            tvgName = tvgName,
            logoUrl = logoUrl,
            groupTitle = groupTitle,
            channelName = channelNamePart
        )
    }

    private fun resolveUrl(url: String, baseUrl: String?): String {
        val trimmed = url.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("rtmp://", ignoreCase = true) ||
            trimmed.startsWith("rtsp://", ignoreCase = true) ||
            trimmed.startsWith("content://", ignoreCase = true) ||
            trimmed.startsWith("file://", ignoreCase = true)
        ) {
            return trimmed
        }

        if (baseUrl != null && baseUrl.isNotBlank()) {
            return try {
                val base = URI(baseUrl)
                base.resolve(trimmed).toString()
            } catch (_: Exception) {
                trimmed
            }
        }
        return trimmed
    }

    private fun isValidStreamUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val lower = url.lowercase()
        return lower.startsWith("http://") ||
                lower.startsWith("https://") ||
                lower.startsWith("rtmp://") ||
                lower.startsWith("rtsp://") ||
                lower.startsWith("content://") ||
                lower.startsWith("file://")
    }

    private fun extractNameFromUrl(url: String): String {
        return try {
            val path = URI(url).path ?: url
            val segment = path.substringAfterLast('/').substringBeforeLast('.')
            if (segment.isNotBlank()) segment else "Unknown Channel"
        } catch (_: Exception) {
            "Unknown Channel"
        }
    }
}
