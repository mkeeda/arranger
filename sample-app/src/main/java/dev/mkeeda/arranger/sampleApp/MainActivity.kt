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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.mkeeda.arranger.core.text.AttributeKey
import dev.mkeeda.arranger.core.text.RichString
import dev.mkeeda.arranger.core.text.rangeOf
import dev.mkeeda.arranger.sampleApp.theme.ArrangerTheme
import dev.mkeeda.arranger.ui.AttributeStyleResolver
import dev.mkeeda.arranger.ui.RichTextEditor
import dev.mkeeda.arranger.ui.RichTextState

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

private object BoldKey : AttributeKey<Unit> {
    override val name: String = "Bold"
    override val defaultValue: Unit = Unit
}

private object ColorKey : AttributeKey<Color> {
    override val name: String = "Color"
    override val defaultValue: Color = Color.Unspecified
}

private object UnderlineKey : AttributeKey<Unit> {
    override val name: String = "Underline"
    override val defaultValue: Unit = Unit
}

@Composable
fun RichTextSampleScreen(modifier: Modifier = Modifier) {
    val initialText = "Welcome to Arranger!\nThis is a RichTextEditor sample.\nYou can mix colors, bold text, and underlines."

    val state =
        remember {
            RichTextState(
                initialText =
                    RichString(text = initialText).edit {
                        setAttribute(BoldKey, Unit, range = initialText.rangeOf("Arranger!"))
                        setAttribute(ColorKey, Color(0xFF6200EA), range = initialText.rangeOf("Welcome to Arranger!"))

                        setAttribute(BoldKey, Unit, range = initialText.rangeOf("RichTextEditor"))
                        setAttribute(ColorKey, Color(0xFF00C853), range = initialText.rangeOf("RichTextEditor"))

                        setAttribute(UnderlineKey, Unit, range = initialText.rangeOf("underlines"))
                        setAttribute(ColorKey, Color(0xFFD50000), range = initialText.rangeOf("colors"))
                    },
            )
        }

    val styleResolver =
        remember {
            AttributeStyleResolver {
                spanStyle(BoldKey) { SpanStyle(fontWeight = FontWeight.Bold) }
                spanStyle(ColorKey) { color -> SpanStyle(color = color) }
                spanStyle(UnderlineKey) { SpanStyle(textDecoration = TextDecoration.Underline) }
            }
        }

    Column(modifier = modifier.padding(16.dp)) {
        Text("Compose RichTextEditor Demo", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        RichTextEditor(
            state = state,
            styleResolver = styleResolver,
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
