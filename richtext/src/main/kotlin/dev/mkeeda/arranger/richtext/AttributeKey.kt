package dev.mkeeda.arranger.richtext

/**
 * Interface defining a key for attributes held within RichText.
 * By having a type-safe [defaultValue], it clearly defines the fallback value when an attribute is not set.
 */
public interface AttributeKey<T> {
    public val name: String
    public val defaultValue: T
}
