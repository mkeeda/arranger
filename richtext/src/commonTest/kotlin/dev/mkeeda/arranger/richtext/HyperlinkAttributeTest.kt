package dev.mkeeda.arranger.richtext

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class HyperlinkAttributeTest {
    @Test
    fun `apply and remove LinkKey on RichString`() {
        val richString =
            RichString("Check out Google for search.").edit {
                editAttributes(10..15) {
                    link("https://google.com")
                }
            }

        // Check runs/attributes
        val runs = richString.runs(LinkKey).toList()
        val googleRun = runs.firstOrNull { it.text == "Google" }
        googleRun?.value shouldBe "https://google.com"

        // Check removal
        val modifiedString =
            richString.edit {
                editAttributes(10..15) {
                    clearLink()
                }
            }
        val modifiedRuns = modifiedString.runs(LinkKey).toList()
        val modifiedGoogleRun = modifiedRuns.firstOrNull { it.text == "Google" }
        modifiedGoogleRun.shouldBeNull()
    }

    @Test
    fun `UrlParser finds http https and www urls`() {
        val text = "Visit https://kotlinlang.org or http://example.com/test?a=1 and www.github.com for details."
        val discovered = UrlParser.findUrls(text)

        discovered.size shouldBe 3

        discovered[0].range shouldBe 6..27
        discovered[0].rawUrl shouldBe "https://kotlinlang.org"
        discovered[0].url shouldBe "https://kotlinlang.org"

        discovered[1].range shouldBe 32..58
        discovered[1].rawUrl shouldBe "http://example.com/test?a=1"
        discovered[1].url shouldBe "http://example.com/test?a=1"

        discovered[2].range shouldBe 64..77
        discovered[2].rawUrl shouldBe "www.github.com"
        discovered[2].url shouldBe "https://www.github.com"
    }

    @Test
    fun `UrlParser trims trailing punctuation`() {
        val text = "Go to https://example.com., or (https://example.org/path)!"
        val discovered = UrlParser.findUrls(text)

        discovered.size shouldBe 2
        discovered[0].rawUrl shouldBe "https://example.com"
        discovered[1].rawUrl shouldBe "https://example.org/path"
    }
}
