import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import dev.mkeeda.arranger.sample.shared.AdvancedFormattingSample
import dev.mkeeda.arranger.sample.shared.AttributeBatchEditSample
import dev.mkeeda.arranger.sample.shared.CustomAttributeSample
import dev.mkeeda.arranger.sample.shared.DocumentEditorSample
import dev.mkeeda.arranger.sample.shared.DynamicEditingSample
import dev.mkeeda.arranger.sample.shared.HashtagHighlightSample
import dev.mkeeda.arranger.sample.shared.ListFormattingSample
import dev.mkeeda.arranger.sample.shared.UndoRedoSample
import dev.mkeeda.arranger.sample.shared.theme.ArrangerTheme

private enum class SampleDestination(val title: String) {
    DynamicEditing("Dynamic Editing"),
    AdvancedFormatting("Advanced Formatting"),
    CustomAttribute("Custom Attribute"),
    HashtagHighlight("Hashtag Highlight"),
    AttributeBatchEdit("Attribute Batch Edit"),
    ListFormatting("List Formatting"),
    UndoRedo("Undo / Redo"),
    DocumentEditor("Document Editor"),
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "ComposeTarget") {
        ArrangerTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                SampleApp()
            }
        }
    }
}

@Composable
private fun SampleApp() {
    var currentDestination by remember { mutableStateOf(SampleDestination.DocumentEditor) }

    Row(modifier = Modifier.fillMaxSize()) {
        SampleListPane(
            modifier = Modifier.weight(1f),
            currentDestination = currentDestination,
            onSampleSelected = { currentDestination = it },
        )
        VerticalDivider()
        SampleDetailPane(
            modifier = Modifier.weight(2f),
            currentDestination = currentDestination,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SampleListPane(
    modifier: Modifier = Modifier,
    currentDestination: SampleDestination?,
    onSampleSelected: (SampleDestination) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(text = "Samples") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues = innerPadding)
                    .padding(horizontal = 16.dp),
        ) {
            item { Spacer(modifier = Modifier.height(height = 8.dp)) }
            items(items = SampleDestination.entries) { destination ->
                val isSelected = currentDestination == destination
                OutlinedCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { onSampleSelected(destination) },
                    colors =
                        CardDefaults.outlinedCardColors(
                            containerColor =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                        ),
                ) {
                    Text(
                        text = destination.title,
                        modifier = Modifier.padding(all = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(height = 8.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SampleDetailPane(
    modifier: Modifier = Modifier,
    currentDestination: SampleDestination,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = currentDestination.title) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues = innerPadding)
                    .padding(all = 16.dp),
        ) {
            when (currentDestination) {
                SampleDestination.DynamicEditing -> DynamicEditingSample()
                SampleDestination.AdvancedFormatting -> AdvancedFormattingSample()
                SampleDestination.CustomAttribute -> CustomAttributeSample()
                SampleDestination.HashtagHighlight -> HashtagHighlightSample()
                SampleDestination.AttributeBatchEdit -> AttributeBatchEditSample()
                SampleDestination.ListFormatting -> ListFormattingSample()
                SampleDestination.UndoRedo -> UndoRedoSample()
                SampleDestination.DocumentEditor -> DocumentEditorSample()
            }
        }
    }
}
