package dev.mkeeda.arranger.sampleApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.richtext.HeadingLevel
import dev.mkeeda.arranger.richtext.RichString
import dev.mkeeda.arranger.richtext.TextAlignment
import dev.mkeeda.arranger.richtext.blockquote
import dev.mkeeda.arranger.richtext.bold
import dev.mkeeda.arranger.richtext.editor.RichTextEditor
import dev.mkeeda.arranger.richtext.editor.RichTextState
import dev.mkeeda.arranger.richtext.editor.textColor
import dev.mkeeda.arranger.richtext.headingLevel
import dev.mkeeda.arranger.richtext.rangeOf
import dev.mkeeda.arranger.richtext.textAlignment
import dev.mkeeda.arranger.richtext.underline
import dev.mkeeda.arranger.sampleApp.theme.ArrangerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArrangerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RichTextSampleScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun RichTextSampleScreen(modifier: Modifier = Modifier) {
    val initialText =
        "Arranger RichText Editor\n" +
            "Welcome to Arranger! This is a sample.\n" +
            "You can mix colors, bold text, and underlines.\n\n" +
            "Paragraph Styles Demo\n" +
            "This paragraph is centered correctly.\n" +
            "> This is a blockquote with nice indents."

    val state =
        remember {
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        editAttributes(range = initialText.rangeOf("Arranger RichText Editor")) {
                            headingLevel(HeadingLevel.H1)
                        }
                        editAttributes(range = initialText.rangeOf("Paragraph Styles Demo")) {
                            headingLevel(HeadingLevel.H3)
                        }
                        editAttributes(range = initialText.rangeOf("This paragraph is centered correctly.")) {
                            textAlignment(TextAlignment.Center)
                        }
                        editAttributes(range = initialText.rangeOf("> This is a blockquote with nice indents.")) {
                            blockquote()
                        }
                        editAttributes(range = initialText.rangeOf("Arranger!")) {
                            bold()
                        }
                        editAttributes(range = initialText.rangeOf("Welcome to Arranger!")) {
                            textColor(Color(0xFF6200EA))
                        }
                        editAttributes(range = initialText.rangeOf("colors")) {
                            textColor(Color(0xFFD50000))
                        }
                        editAttributes(range = initialText.rangeOf("bold text")) {
                            bold()
                        }
                        editAttributes(range = initialText.rangeOf("underlines")) {
                            underline()
                        }
                    },
            )
        }

    Column(modifier = modifier.padding(16.dp)) {
        Text("Compose RichTextEditor Demo", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        RichTextEditor(
            state = state,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RichTextSamplePreview() {
    ArrangerTheme {
        RichTextSampleScreen()
    }
}
