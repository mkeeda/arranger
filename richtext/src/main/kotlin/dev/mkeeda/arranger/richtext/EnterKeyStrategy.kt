package dev.mkeeda.arranger.richtext

/**
 * Defines the strategy for how a paragraph should handle an Enter key press (newline insertion).
 * This strategy determines the attributes of the newly created paragraph or modifies the current paragraph's attributes.
 */
public interface EnterKeyStrategy {
    public fun execute(context: EnterKeyContext): EnterKeyResult
}

/**
 * Provides the context necessary for an [EnterKeyStrategy] to make a decision.
 *
 * @property text The entire text before the newline is inserted.
 * @property cursorPosition The index where the newline was inserted.
 * @property paragraphRange The range of the paragraph where the cursor is currently located.
 * @property currentAttributes The block attributes applied to the current paragraph.
 */
public data class EnterKeyContext(
    val text: String,
    val cursorPosition: Int,
    val paragraphRange: IntRange,
    val currentAttributes: AttributeContainer,
)

/**
 * Represents the outcome of an [EnterKeyStrategy] execution.
 */
public sealed interface EnterKeyResult {
    /**
     * Inherit the specified attributes for the newly created paragraph.
     * The newline is preserved.
     */
    public data class InheritAttributes(val attributes: AttributeContainer) : EnterKeyResult

    /**
     * Clear the block attributes for the newly created paragraph, resulting in a standard paragraph.
     * The newline is preserved.
     */
    public data object ClearAttributes : EnterKeyResult

    /**
     * Do not insert a newline. Instead, modify the attributes of the current paragraph.
     * This is typically used for outdenting empty list items.
     */
    public data class Outdent(val attributes: AttributeContainer) : EnterKeyResult
}
