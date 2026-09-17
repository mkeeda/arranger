package dev.mkeeda.arranger.sample.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.LinkKey
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.SpanAttributeKey
import dev.mkeeda.arranger.richtext.editor.AttributeStyleResolver
import dev.mkeeda.arranger.richtext.editor.DefaultAttributeStyleResolver
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.rangeOf
import kotlinx.coroutines.launch

public object MentionKey : SpanAttributeKey<String> {
    override val name: String = "mention"
    override val defaultValue: String = ""
}

public object HashtagKey : SpanAttributeKey<String> {
    override val name: String = "hashtag"
    override val defaultValue: String = ""
}

private val interactiveStyleResolver =
    AttributeStyleResolver(base = DefaultAttributeStyleResolver) {
        spanStyle(MentionKey) {
            SpanStyle(
                color = Color(0xFF1E88E5),
                fontWeight = FontWeight.Bold,
            )
        }
        spanStyle(HashtagKey) {
            SpanStyle(
                color = Color(0xFF00897B),
                fontWeight = FontWeight.SemiBold,
            )
        }
        spanStyle(LinkKey) {
            SpanStyle(
                color = Color(0xFF7B1FA2),
                textDecoration = TextDecoration.Underline,
            )
        }
    }

@Composable
public fun InteractiveSpanSample(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
) {
    val initialText =
        "Welcome to Arranger!\n\n" +
            "Tap interactive spans below:\n" +
            "• Mention: @alice or @bob\n" +
            "• Hashtag: #compose and #kmp\n" +
            "• Link: https://github.com\n\n" +
            "Tapping any interactive span triggers onSpanClick, displays feedback, and calls event.consume() " +
            "to suppress cursor relocation. Plain text taps move the cursor normally."

    val state =
        remember {
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setSpanAttribute(MentionKey, "alice", initialText.rangeOf("@alice"))
                        setSpanAttribute(MentionKey, "bob", initialText.rangeOf("@bob"))
                        setSpanAttribute(HashtagKey, "compose", initialText.rangeOf("#compose"))
                        setSpanAttribute(HashtagKey, "kmp", initialText.rangeOf("#kmp"))
                        setSpanAttribute(LinkKey, "https://github.com", initialText.rangeOf("https://github.com"))
                    },
            )
        }

    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var lastInteraction by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Text(
            text = "Interactive Spans Sample",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (lastInteraction != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            ) {
                Text(
                    text = "Last Event: $lastInteraction",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        RichTextEditor(
            state = state,
            styleResolver = interactiveStyleResolver,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            onSpanClick = { event ->
                val span = event.span
                when {
                    span.attributes.containsKey(MentionKey) -> {
                        val user = span.attributes[MentionKey].orEmpty()
                        val message = "Mention clicked: @$user"
                        lastInteraction = message
                        coroutineScope.launch {
                            snackbarHostState?.showSnackbar(message)
                        }
                        event.consume()
                    }

                    span.attributes.containsKey(HashtagKey) -> {
                        val tag = span.attributes[HashtagKey].orEmpty()
                        val message = "Hashtag clicked: #$tag"
                        lastInteraction = message
                        coroutineScope.launch {
                            snackbarHostState?.showSnackbar(message)
                        }
                        event.consume()
                    }

                    span.attributes.containsKey(LinkKey) -> {
                        val url = span.attributes[LinkKey].orEmpty()
                        val message = "Opening link: $url"
                        lastInteraction = message
                        uriHandler.openUri(url)
                        coroutineScope.launch {
                            snackbarHostState?.showSnackbar(message)
                        }
                        event.consume()
                    }
                }
            },
        )
    }
}
