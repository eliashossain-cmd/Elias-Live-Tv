package com.example

import com.example.data.parser.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uParserTest {

    @Test
    fun parseStandardM3uPlaylist_success() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="CNN.us" tvg-name="CNN HD" tvg-logo="https://example.com/cnn.png" group-title="News",CNN International
            https://stream.example.com/cnn/live.m3u8
            #EXTINF:-1 tvg-id="ESPN.us" tvg-name="ESPN Sports" tvg-logo="https://example.com/espn.png" group-title="Sports",ESPN 1
            https://stream.example.com/espn/live.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(2, result.validChannelsCount)
        assertEquals(2, result.totalEntriesParsed)
        assertEquals(0, result.skippedEntriesCount)

        val cnn = result.channels[0]
        assertEquals("CNN International", cnn.name)
        assertEquals("https://stream.example.com/cnn/live.m3u8", cnn.streamUrl)
        assertEquals("https://example.com/cnn.png", cnn.logoUrl)
        assertEquals("News", cnn.groupTitle)
        assertEquals("CNN.us", cnn.tvgId)

        val espn = result.channels[1]
        assertEquals("ESPN 1", espn.name)
        assertEquals("https://stream.example.com/espn/live.m3u8", espn.streamUrl)
        assertEquals("Sports", espn.groupTitle)
    }

    @Test
    fun parseUtf8Bom_handledCorrectly() {
        val content = "\uFEFF#EXTM3U\n#EXTINF:-1,Channel With BOM\nhttps://example.com/stream.m3u8"
        val result = M3uParser.parse(content)

        assertEquals(1, result.validChannelsCount)
        assertEquals("Channel With BOM", result.channels[0].name)
    }

    @Test
    fun parseEmptyPlaylist_returnsZeroChannels() {
        val content = "#EXTM3U\n\n   \n"
        val result = M3uParser.parse(content)

        assertEquals(0, result.validChannelsCount)
        assertEquals(0, result.totalEntriesParsed)
        assertTrue(result.channels.isEmpty())
    }

    @Test
    fun parseMissingAttributes_fallsBackSafely() {
        val content = """
            #EXTINF:-1,Simple Channel Without Attributes
            http://example.com/live.ts
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(1, result.validChannelsCount)
        val ch = result.channels[0]
        assertEquals("Simple Channel Without Attributes", ch.name)
        assertEquals("General", ch.groupTitle)
        assertNull(ch.logoUrl)
        assertNull(ch.tvgId)
    }

    @Test
    fun parseInterveningCommentsAndGroupTags() {
        val content = """
            #EXTM3U
            #EXTINF:-1 tvg-id="Test",Custom Channel
            #EXTGRP:Entertainment
            #EXTVLCOPT:network-caching=1000
            # Some arbitrary comment
            https://example.com/stream/index.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(1, result.validChannelsCount)
        val ch = result.channels[0]
        assertEquals("Custom Channel", ch.name)
        assertEquals("Entertainment", ch.groupTitle)
        assertEquals("https://example.com/stream/index.m3u8", ch.streamUrl)
    }

    @Test
    fun parseUrlsWithSpecialCharacters() {
        val content = """
            #EXTINF:-1,Stream With Token & Query
            https://cdn.provider.tv/live/auth.m3u8?token=xyz123_abc&channel=news%20live&exp=1700000000
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(1, result.validChannelsCount)
        assertEquals("https://cdn.provider.tv/live/auth.m3u8?token=xyz123_abc&channel=news%20live&exp=1700000000", result.channels[0].streamUrl)
    }

    @Test
    fun parseMalformedLines_skippedWithoutCrashing() {
        val content = """
            #EXTM3U
            This is random junk text
            #EXTINF:-1,Invalid Stream Line
            not-a-valid-url-candidate
            #EXTINF:-1,Good Channel
            https://example.com/good.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(1, result.validChannelsCount)
        assertEquals("Good Channel", result.channels[0].name)
        assertTrue(result.skippedEntriesCount >= 1)
    }

    @Test
    fun parseSingleQuotedAttributes() {
        val content = """
            #EXTINF:-1 tvg-id='BBC' tvg-logo='https://example.com/bbc.png' group-title='British News',BBC News
            https://example.com/bbc.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content)

        assertEquals(1, result.validChannelsCount)
        val ch = result.channels[0]
        assertEquals("BBC News", ch.name)
        assertEquals("BBC", ch.tvgId)
        assertEquals("https://example.com/bbc.png", ch.logoUrl)
        assertEquals("British News", ch.groupTitle)
    }

    @Test
    fun resolveRelativeUrl_withBaseUrl() {
        val content = """
            #EXTINF:-1,Relative Stream
            streams/channel1.m3u8
        """.trimIndent()

        val result = M3uParser.parse(content, baseUrl = "https://iptv.server.net/playlist/")

        assertEquals(1, result.validChannelsCount)
        assertEquals("https://iptv.server.net/playlist/streams/channel1.m3u8", result.channels[0].streamUrl)
    }
}
