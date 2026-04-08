package dev.mkeeda.arranger.core.text

/**
 * An immutable container that holds attribute keys and their values in a type-safe manner.
 */
public class AttributeContainer private constructor(
    private val attributes: Map<RichAttributeKey<*>, Any?>,
) {
    /**
     * Retrieves the attribute value for the specified key.
     * If not set, it returns the [RichAttributeKey.defaultValue] defined in the key.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T> getOrDefault(key: RichAttributeKey<T>): T {
        return if (attributes.containsKey(key)) {
            attributes[key] as T
        } else {
            key.defaultValue
        }
    }

    /**
     * Retrieves the attribute value for the specified key.
     * If not set, it returns null.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T> getOrNull(key: RichAttributeKey<T>): T? {
        return attributes[key] as T?
    }

    /**
     * Returns a new [AttributeContainer] containing the specified key and value.
     * If a key with the same name already exists, it is overwritten with the new value.
     */
    public fun <T> with(key: RichAttributeKey<T>, value: T): AttributeContainer {
        val newAttributes = attributes.toMutableMap()
        newAttributes[key] = value
        return AttributeContainer(newAttributes)
    }

    /**
     * Returns a new [AttributeContainer] containing all attributes from this and the [other].
     * If keys collide, values from [other] take precedence (overwrite).
     */
    public operator fun plus(other: AttributeContainer): AttributeContainer {
        if (other.attributes.isEmpty()) return this
        if (this.attributes.isEmpty()) return other
        val newAttributes = attributes.toMutableMap()
        newAttributes.putAll(other.attributes)
        return AttributeContainer(newAttributes)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttributeContainer

        return attributes == other.attributes
    }

    override fun hashCode(): Int {
        return attributes.hashCode()
    }

    public companion object {
        private val EMPTY = AttributeContainer(emptyMap())

        /**
         * Gets an empty [AttributeContainer].
         */
        public fun empty(): AttributeContainer = EMPTY
    }
}
