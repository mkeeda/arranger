package dev.mkeeda.arranger.sampleApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.mkeeda.arranger.sampleApp.theme.ArrangerTheme
import kotlinx.serialization.Serializable

@Serializable
private data object SampleList : NavKey

@Serializable
private enum class SampleDestination(val title: String) : NavKey {
    BasicUsage("Basic Usage"),
    AdvancedFormatting("Advanced Formatting"),
    CustomAttribute("Custom Attribute"),
    HashtagHighlight("Hashtag Highlight"),
    AttributeBatchEdit("Attribute Batch Edit"),
    ChatInput("Chat Input"),
    ListFormatting("List Formatting"),
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArrangerTheme {
                val backStack = rememberNavBackStack(SampleList)

                val windowAdaptiveInfo = currentWindowAdaptiveInfo()
                val directive =
                    remember(windowAdaptiveInfo) {
                        calculatePaneScaffoldDirective(windowAdaptiveInfo)
                            .copy(horizontalPartitionSpacerSize = 0.dp)
                    }
                val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    sceneStrategies = listOf(listDetailStrategy),
                    entryProvider =
                        entryProvider {
                            entry<SampleList>(
                                metadata =
                                    ListDetailSceneStrategy.listPane(
                                        detailPlaceholder = {
                                            Scaffold { innerPadding ->
                                                Box(
                                                    modifier =
                                                        Modifier
                                                            .fillMaxSize()
                                                            .padding(innerPadding),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        text = "Select a sample from the list",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                        },
                                    ),
                            ) {
                                SampleListScreen(
                                    onSampleSelected = { destination ->
                                        if (backStack.lastOrNull() != destination) {
                                            backStack.add(destination)
                                        }
                                    },
                                )
                            }

                            entry<SampleDestination>(
                                metadata = ListDetailSceneStrategy.detailPane(),
                            ) { detail ->
                                SampleDetailScreen(
                                    destination = detail,
                                    onBack = { backStack.removeLastOrNull() },
                                )
                            }
                        },
                )
            }
        }
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
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            items(SampleDestination.entries) { destination ->
                OutlinedCard(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { onSampleSelected(destination) },
                    colors =
                        CardDefaults.outlinedCardColors(
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
                        Icon(
                            painter = painterResource(R.drawable.baseline_arrow_back_24),
                            contentDescription = "back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
        ) {
            when (destination) {
                SampleDestination.BasicUsage -> BasicUsageSample()
                SampleDestination.AdvancedFormatting -> AdvancedFormattingSample()
                SampleDestination.CustomAttribute -> CustomAttributeSample()
                SampleDestination.HashtagHighlight -> HashtagHighlightSample()
                SampleDestination.AttributeBatchEdit -> AttributeBatchEditSample()
                SampleDestination.ChatInput -> ChatInputSample()
                SampleDestination.ListFormatting -> ListFormattingSample()
            }
        }
    }
}
