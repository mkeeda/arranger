package dev.mkeeda.arranger.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import dev.mkeeda.arranger.runtime.node.RootNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SimpleEditor(
    clock: MonotonicFrameClock
) {
    private val rootNode = RootNode()
    private val editorScope = CoroutineScope(clock)
    private val recomposer = Recomposer(editorScope.coroutineContext)

    suspend fun launch(document: @Composable DocumentScope.() -> Unit) {
        val recomposerJob = editorScope.launch {
            recomposer.runRecomposeAndApplyChanges()
        }

        val snapshotHandle = Snapshot.registerGlobalWriteObserver {
            Snapshot.sendApplyNotifications()
        }

        val composition = rootNode.setContent(recomposer) {
            DocumentScope().document()
        }

        recomposerJob.invokeOnCompletion {
            println("cancel")
            composition.dispose()
            snapshotHandle.dispose()
        }

        recomposer.awaitIdle()
    }

    suspend fun currentText(): String {
        recomposer.awaitIdle()
        return rootNode.toSemanticText()
    }

    fun dispose() {
        editorScope.cancel()
    }
}
