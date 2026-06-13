package dev.mkeeda.arranger.sample.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
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

// Disable PlistSanityCheck to prevent crashes related to implicit Info.plist checks 
// for 120Hz displays on ProMotion devices when using SwiftPM.
fun MainViewController() = ComposeUIViewController(
    configure = { enforceStrictPlistSanityCheck = false }
) {
    ArrangerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            IosArrangerSampleApp()
        }
    }
}

@Composable
private fun IosArrangerSampleApp() {
    var currentDestination by remember { mutableStateOf<SampleDestination?>(null) }
    val destination = currentDestination

    if (destination == null) {
        SampleListScreen(
            onSampleSelected = { currentDestination = it }
        )
    } else {
        SampleDetailScreen(
            destination = destination,
            onBack = { currentDestination = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SampleListScreen(onSampleSelected: (SampleDestination) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Arranger Samples") })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            items(SampleDestination.entries) { destination ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onSampleSelected(destination) },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Text(
                        text = destination.title,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SampleDetailScreen(destination: SampleDestination, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(destination.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("<")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            when (destination) {
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
