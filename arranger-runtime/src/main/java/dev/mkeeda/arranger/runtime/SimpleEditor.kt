package dev.mkeeda.arranger.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import dev.mkeeda.arranger.runtime.node.RootNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SimpleEditor(
    clock: MonotonicFrameClock
) {
    private val rootNode = RootNode()
    private val editorScope = CoroutineScope(
        NonCancellable + clock
    )
    private val recomposer = Recomposer(editorScope.coroutineContext)

    suspend fun launch(initialDocument: @Composable DocumentScope.() -> Unit) {
        val recomposerJob = editorScope.launch {
            recomposer.runRecomposeAndApplyChanges()
        }

        var applyScheduled = false
        val snapshotHandle = Snapshot.registerGlobalWriteObserver {
            if (!applyScheduled) {
                applyScheduled = true
                editorScope.launch {
                    applyScheduled = false
                    Snapshot.sendApplyNotifications()
                }
            }
        }

        val scope = object : DocumentScope {}
        val composition = rootNode.setContent(recomposer) {
            scope.initialDocument()
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

    fun insert(text: String, index: Int) {
        // TODO
    }

    fun dispose() {
        editorScope.cancel()
    }
}

suspend fun SimpleEditor.launch(initialText: String) = launch {
    Text(text = initialText)
}
