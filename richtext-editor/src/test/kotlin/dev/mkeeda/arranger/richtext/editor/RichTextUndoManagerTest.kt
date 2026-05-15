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
            selection = TextRange.Zero,
        )
    }

    @Test
    fun `canUndo returns false when stack is empty`() {
        val manager = RichTextUndoManager()
        manager.canUndo.shouldBeFalse()
    }

    @Test
    fun `canRedo returns false when stack is empty`() {
        val manager = RichTextUndoManager()
        manager.canRedo.shouldBeFalse()
    }

    @Test
    fun `pushSnapshot with Separate makes canUndo true`() {
        val manager = RichTextUndoManager()
        manager.pushSnapshot(createSnapshot("A"), UndoMergePolicy.Separate)
        manager.canUndo.shouldBeTrue()
    }

    @Test
    fun `pushSnapshot with Merge adds new entry if last was Separate`() {
        val manager = RichTextUndoManager()
        manager.pushSnapshot(createSnapshot("A"), UndoMergePolicy.Separate)
        manager.pushSnapshot(createSnapshot("AB"), UndoMergePolicy.Merge)

        val undone = manager.undo(createSnapshot("ABC"))
        undone?.text shouldBe "AB"
    }

    @Test
    fun `pushSnapshot with Merge ignores if last was Merge`() {
        val manager = RichTextUndoManager()
        manager.pushSnapshot(createSnapshot("A"), UndoMergePolicy.Separate)
        manager.pushSnapshot(createSnapshot("AB"), UndoMergePolicy.Merge)
        manager.pushSnapshot(createSnapshot("ABC"), UndoMergePolicy.Merge) // Should be ignored

        val undone = manager.undo(createSnapshot("ABCD"))
        // It pops "AB" because "ABC" was ignored
        undone?.text shouldBe "AB"
    }

    @Test
    fun `pushSnapshot with Merge on empty stack adds entry`() {
        val manager = RichTextUndoManager()
        manager.pushSnapshot(createSnapshot("A"), UndoMergePolicy.Merge)
        manager.canUndo.shouldBeTrue()

        val undone = manager.undo(createSnapshot("AB"))
        undone?.text shouldBe "A"
    }

    @Test
    fun `undo returns the last pushed snapshot`() {
        val manager = RichTextUndoManager()
        val snapshot1 = createSnapshot("1")
        val snapshot2 = createSnapshot("2")
        manager.pushSnapshot(snapshot1, UndoMergePolicy.Separate)
        manager.pushSnapshot(snapshot2, UndoMergePolicy.Separate)

        val undone = manager.undo(createSnapshot("3"))
        undone shouldBe snapshot2
    }

    @Test
    fun `undo moves current snapshot to redo stack`() {
        val manager = RichTextUndoManager()
        manager.pushSnapshot(createSnapshot("1"), UndoMergePolicy.Separate)

        val current = createSnapshot("2")
        manager.undo(current)

        manager.canRedo.shouldBeTrue()
        val redone = manager.redo(createSnapshot("1"))
        redone shouldBe current
    }

    @Test
    fun `redo returns the undone snapshot`() {
        val manager = RichTextUndoManager()
        val original = createSnapshot("1")
        manager.pushSnapshot(original, UndoMergePolicy.Separate)

        val current = createSnapshot("2")
        manager.undo(current)

        val redone = manager.redo(createSnapshot("1"))
        redone shouldBe current
    }

    @Test
    fun `push after undo clears redo stack`() {
        val manager = RichTextUndoManager()
        manager.pushSnapshot(createSnapshot("1"), UndoMergePolicy.Separate)
        manager.undo(createSnapshot("2"))

        manager.canRedo.shouldBeTrue()

        manager.pushSnapshot(createSnapshot("3"), UndoMergePolicy.Separate)
        manager.canRedo.shouldBeFalse()
    }

    @Test
    fun `undo returns null when stack is empty`() {
        val manager = RichTextUndoManager()
        val undone = manager.undo(createSnapshot("1"))
        undone.shouldBeNull()
    }

    @Test
    fun `redo returns null when stack is empty`() {
        val manager = RichTextUndoManager()
        val redone = manager.redo(createSnapshot("1"))
        redone.shouldBeNull()
    }

    @Test
    fun `stack is capped at maxSize`() {
        val manager = RichTextUndoManager()
        for (i in 1..105) {
            manager.pushSnapshot(createSnapshot(i.toString()), UndoMergePolicy.Separate)
        }

        // 100 entries should be kept, so the oldest 5 (1, 2, 3, 4, 5) are dropped.
        // We do 100 undos and the last one should be snapshot "6"
        var lastUndone: EditorSnapshot? = null
        val current = createSnapshot("106")
        for (i in 1..100) {
            val prev = if (i == 1) current else requireNotNull(lastUndone) { "undo stack exhausted earlier than expected" }
            lastUndone = manager.undo(prev)
        }

        lastUndone?.text shouldBe "6"
        manager.canUndo.shouldBeFalse() // Stack should be empty now
    }

    @Test
    fun `clear resets both stacks`() {
        val manager = RichTextUndoManager()
        manager.pushSnapshot(createSnapshot("1"), UndoMergePolicy.Separate)
        manager.undo(createSnapshot("2"))

        manager.clear()

        manager.canUndo.shouldBeFalse()
        manager.canRedo.shouldBeFalse()
    }
}
