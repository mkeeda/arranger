package dev.mkeeda.arranger.richtext.editor

import androidx.compose.ui.text.TextRange
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RichTextUndoManagerTest {

    private fun createSnapshot(text: String): EditorSnapshot {
        return EditorSnapshot(
            text = text,
            spans = emptyList(),
            selection = TextRange.Zero
        )
    }

    @Test
    fun canUndo_returnsFalse_whenStackIsEmpty() {
        val manager = RichTextUndoManager()
        manager.canUndo.shouldBeFalse()
    }

    @Test
    fun canRedo_returnsFalse_whenStackIsEmpty() {
        val manager = RichTextUndoManager()
        manager.canRedo.shouldBeFalse()
    }

    @Test
    fun pushSnapshot_SEPARATE_makesCanUndoTrue() {
        val manager = RichTextUndoManager()
        manager.pushSnapshot(createSnapshot("A"), UndoMergePolicy.SEPARATE)
        manager.canUndo.shouldBeTrue()
    }

    @Test
    fun pushSnapshot_MERGE_addsNewEntryIfLastWasSeparate() {
        val manager = RichTextUndoManager()
        manager.pushSnapshot(createSnapshot("A"), UndoMergePolicy.SEPARATE)
        manager.pushSnapshot(createSnapshot("AB"), UndoMergePolicy.MERGE)
        
        val undone = manager.undo(createSnapshot("ABC"))
        undone?.text shouldBe "AB"
    }

    @Test
    fun pushSnapshot_MERGE_ignoresIfLastWasMerge() {
        val manager = RichTextUndoManager()
        manager.pushSnapshot(createSnapshot("A"), UndoMergePolicy.SEPARATE)
        manager.pushSnapshot(createSnapshot("AB"), UndoMergePolicy.MERGE)
        manager.pushSnapshot(createSnapshot("ABC"), UndoMergePolicy.MERGE) // Should be ignored
        
        val undone = manager.undo(createSnapshot("ABCD"))
        // It pops "AB" because "ABC" was ignored
        undone?.text shouldBe "AB"
    }

    @Test
    fun pushSnapshot_MERGE_onEmptyStack_addsEntry() {
        val manager = RichTextUndoManager()
        manager.pushSnapshot(createSnapshot("A"), UndoMergePolicy.MERGE)
        manager.canUndo.shouldBeTrue()
        
        val undone = manager.undo(createSnapshot("AB"))
        undone?.text shouldBe "A"
    }

    @Test
    fun undo_returnsTheLastPushedSnapshot() {
        val manager = RichTextUndoManager()
        val snapshot1 = createSnapshot("1")
        val snapshot2 = createSnapshot("2")
        manager.pushSnapshot(snapshot1, UndoMergePolicy.SEPARATE)
        manager.pushSnapshot(snapshot2, UndoMergePolicy.SEPARATE)

        val undone = manager.undo(createSnapshot("3"))
        undone shouldBe snapshot2
    }

    @Test
    fun undo_movesCurrentSnapshotToRedoStack() {
        val manager = RichTextUndoManager()
        manager.pushSnapshot(createSnapshot("1"), UndoMergePolicy.SEPARATE)
        
        val current = createSnapshot("2")
        manager.undo(current)
        
        manager.canRedo.shouldBeTrue()
        val redone = manager.redo(createSnapshot("1"))
        redone shouldBe current
    }

    @Test
    fun redo_returnsTheUndoneSnapshot() {
        val manager = RichTextUndoManager()
        val original = createSnapshot("1")
        manager.pushSnapshot(original, UndoMergePolicy.SEPARATE)
        
        val current = createSnapshot("2")
        manager.undo(current)
        
        val redone = manager.redo(createSnapshot("1"))
        redone shouldBe current
    }

    @Test
    fun pushAfterUndo_clearsRedoStack() {
        val manager = RichTextUndoManager()
        manager.pushSnapshot(createSnapshot("1"), UndoMergePolicy.SEPARATE)
        manager.undo(createSnapshot("2"))
        
        manager.canRedo.shouldBeTrue()
        
        manager.pushSnapshot(createSnapshot("3"), UndoMergePolicy.SEPARATE)
        manager.canRedo.shouldBeFalse()
    }

    @Test
    fun undo_returnsNull_whenStackIsEmpty() {
        val manager = RichTextUndoManager()
        val undone = manager.undo(createSnapshot("1"))
        undone.shouldBeNull()
    }

    @Test
    fun redo_returnsNull_whenStackIsEmpty() {
        val manager = RichTextUndoManager()
        val redone = manager.redo(createSnapshot("1"))
        redone.shouldBeNull()
    }

    @Test
    fun stackIsCappedAtMaxSize() {
        val manager = RichTextUndoManager()
        for (i in 1..105) {
            manager.pushSnapshot(createSnapshot(i.toString()), UndoMergePolicy.SEPARATE)
        }
        
        // 100 entries should be kept, so the oldest 5 (1, 2, 3, 4, 5) are dropped.
        // We do 100 undos and the last one should be snapshot "6"
        var lastUndone: EditorSnapshot? = null
        val current = createSnapshot("106")
        for (i in 1..100) {
            lastUndone = manager.undo(if (i == 1) current else lastUndone!!)
        }
        
        lastUndone?.text shouldBe "6"
        manager.canUndo.shouldBeFalse() // Stack should be empty now
    }

    @Test
    fun clear_resetsBothStacks() {
        val manager = RichTextUndoManager()
        manager.pushSnapshot(createSnapshot("1"), UndoMergePolicy.SEPARATE)
        manager.undo(createSnapshot("2"))
        
        manager.clear()
        
        manager.canUndo.shouldBeFalse()
        manager.canRedo.shouldBeFalse()
    }
}
