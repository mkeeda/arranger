package dev.mkeeda.arranger.richtext

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HyperlinkAttributeTest {
    @Test
    fun apply_and_remove_link_key_on_rich_string() {
        val richString =
            buildRichString {
                append("Check out Google for search.")
                editAttributes(10..15) {
                    link("https://google.com")
                }
            }

        // Check runs/attributes
        val runs = richString.runs().toList()
        val googleRun = runs.firstOrNull { it.text == "Google" }
        assertEquals("https://google.com", googleRun?.attributes?.get(LinkKey))

        // Check removal
        val modifiedString =
            richString.edit {
                editAttributes(10..15) {
                    clearLink()
                }
            }
        val modifiedRuns = modifiedString.runs().toList()
        val modifiedGoogleRun = modifiedRuns.firstOrNull { it.text == "Google" }
        assertNull(modifiedGoogleRun?.attributes?.get(LinkKey))
    }

    @Test
    fun url_parser_finds_http_https_and_www_urls() {
        val text = "Visit https://kotlinlang.org or http://example.com/test?a=1 and www.github.com for details."
        val discovered = UrlParser.findUrls(text)

        assertEquals(3, discovered.size)

        assertEquals(6..27, discovered[0].range)
        assertEquals("https://kotlinlang.org", discovered[0].rawUrl)
        assertEquals("https://kotlinlang.org", discovered[0].url)

        assertEquals(32..58, discovered[1].range)
        assertEquals("http://example.com/test?a=1", discovered[1].rawUrl)
        assertEquals("http://example.com/test?a=1", discovered[1].url)

        assertEquals(63..76, discovered[2].range)
        assertEquals("www.github.com", discovered[2].rawUrl)
        assertEquals("https://www.github.com", discovered[2].url)
    }

    @Test
    fun url_parser_trims_trailing_punctuation() {
        val text = "Go to https://example.com., or (https://example.org/path)!"
        val discovered = UrlParser.findUrls(text)

        assertEquals(2, discovered.size)
        assertEquals("https://example.com", discovered[0].rawUrl)
        assertEquals("https://example.org/path", discovered[1].rawUrl)
    }

    @Test
    fun detect_and_apply_links_applies_link_spans() {
        val richString =
            buildRichString {
                append("Here is www.google.com link.")
            }

        val updated = richString.detectAndApplyLinks()
        val googleRun = updated.runs().firstOrNull { it.text == "www.google.com" }
        assertEquals("https://www.google.com", googleRun?.attributes?.get(LinkKey))
    }
}
