package dev.mkeeda.arranger.richtext

/**
 * An interface for exporting a [RichString] into a specific representation of type [T].
 *
 * @param T The output type representing the formatted text (e.g., [String], HTML, JSON).
 */
public interface RichTextExporter<T> {
    /**
     * Converts the given [richString] into the target format of type [T].
     */
    public fun export(richString: RichString): T
}

/**
 * An interface for importing data of type [T] into a [RichString].
 *
 * @param T The input type representing formatted text (e.g., [String], HTML, JSON).
 */
public interface RichTextImporter<T> {
    /**
     * Converts the given formatted [input] into a [RichString].
     */
    public fun import(input: T): RichString
}

/**
 * A bi-directional format that can both export a [RichString] to type [T]
 * and import type [T] into a [RichString].
 *
 * @param T The intermediate format type (e.g., [String]).
 */
public interface RichTextFormat<T> : RichTextExporter<T>, RichTextImporter<T>

/**
 * Exports this [RichString] using the specified [exporter].
 */
public fun <T> RichString.export(exporter: RichTextExporter<T>): T = exporter.export(this)

/**
 * Imports formatted [input] using the specified [importer] to create a [RichString].
 */
public fun <T> RichString.Companion.import(input: T, importer: RichTextImporter<T>): RichString = importer.import(input)
