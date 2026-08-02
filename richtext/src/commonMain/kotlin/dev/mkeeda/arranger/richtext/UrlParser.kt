package dev.mkeeda.arranger.richtext

/**
 * Represents a URL discovered within a text sequence.
 */
public data class DiscoveredUrl(
    public val range: IntRange,
    public val rawUrl: String,
    public val url: String,
)

/**
 * Utility object to parse URLs from text.
 */
public object UrlParser {
    private val urlRegex = Regex("""(?:https?://|www\.)[^\s<>"'{}|\^~\[\]`]+""", RegexOption.IGNORE_CASE)

    /**
     * Finds all URLs in the provided [text].
     */
    public fun findUrls(text: CharSequence): List<DiscoveredUrl> {
        val results = mutableListOf<DiscoveredUrl>()
        for (match in urlRegex.findAll(text)) {
            var raw = match.value
            var startIndex = match.range.first
            var endIndex = match.range.last

            val trailingPunctuation = setOf('.', ',', '!', '?', ')', ']', ':')
            while (raw.isNotEmpty() && raw.last() in trailingPunctuation) {
                if (raw.last() == ')' && raw.count { it == '(' } >= raw.count { it == ')' }) {
                    break
                }
                raw = raw.dropLast(1)
                endIndex--
            }

            if (raw.isNotEmpty()) {
                val normalizedUrl =
                    if (raw.startsWith("www.", ignoreCase = true)) {
                        "https://$raw"
                    } else {
                        raw
                    }
                results.add(
                    DiscoveredUrl(
                        range = startIndex..endIndex,
                        rawUrl = raw,
                        url = normalizedUrl,
                    ),
                )
            }
        }
        return results
    }
}

/**
 * Convenience extension function to scan [RichString] text for URLs and apply [LinkKey] attributes.
 */
public fun RichString.detectAndApplyLinks(): RichString {
    val urls = UrlParser.findUrls(text)
    if (urls.isEmpty()) return this
    return edit {
        for (discovered in urls) {
            editAttributes(discovered.range) {
                link(discovered.url)
            }
        }
    }
}
