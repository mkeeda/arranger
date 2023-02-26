package dev.mkeeda.arranger.runtime

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import dev.mkeeda.arranger.runtime.node.DocumentNode
import dev.mkeeda.arranger.runtime.node.RootNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch


fun DocumentNode.setContent(
    parent: CompositionContext,
    content: @Composable () -> Unit
): Composition {
    return Composition(DocumentNodeApplier(this), parent).apply {
        setContent(content)
    }
}

suspend fun CoroutineScope.document(content: @Composable DocumentScope.() -> Unit) {
    val rootNode = RootNode()
    // Require MonotonicFrameClock for Recomposer
    val clock = BroadcastFrameClock()
    val composeContext = coroutineContext + clock
    launch(composeContext) {
        while(true) {
            clock.sendFrame(0L)
            println("-------output--------")
            println(rootNode.toSemanticText())
            println("-------output end--------")
            delay(100)
        }
    }
    coroutineScope {
        val recomposer = Recomposer(composeContext)
        val scope = object : DocumentScope {}
        val composition = rootNode.setContent(recomposer) {
            scope.content()
        }
        var applyScheduled = false
        val snapshotHandle = Snapshot.registerGlobalWriteObserver {
            if (!applyScheduled) {
                applyScheduled = true
                launch {
                    applyScheduled = false
                    Snapshot.sendApplyNotifications()
                }
            }
        }

        launch(composeContext) {
            recomposer.runRecomposeAndApplyChanges()
        }

        composeContext.job.invokeOnCompletion {
            println("cancel")
            composition.dispose()
            snapshotHandle.dispose()
        }
    }
}
