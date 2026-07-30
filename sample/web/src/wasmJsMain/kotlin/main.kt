import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.mkeeda.arranger.sample.shared.DocumentEditorSample
import dev.mkeeda.arranger.sample.shared.theme.ArrangerTheme

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "ComposeTarget") {
        ArrangerTheme {
            DocumentEditorSample()
        }
    }
}
