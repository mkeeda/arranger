package dev.mkeeda.arranger.richtext

/**
 * Interface defining a key for attributes held within RichText.
 * By having a type-safe [defaultValue], it clearly defines the fallback value when an attribute is not set.
 */
public interface AttributeKey<T> {
    public val name: String
    public val defaultValue: T
}

/**
 * A marker interface indicating that the attribute applies to character spans.
 */
public interface SpanAttributeKey<T> : AttributeKey<T>

/**
 * A marker interface indicating that the attribute applies to whole paragraphs.
 */
public interface ParagraphAttributeKey<T> : AttributeKey<T>
