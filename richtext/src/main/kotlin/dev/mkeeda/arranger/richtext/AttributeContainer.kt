package dev.mkeeda.arranger.richtext

/**
 * An immutable container that holds attribute keys and their values in a type-safe manner.
 */
public class AttributeContainer private constructor(
    private val attributes: Map<AttributeKey<*>, Any?>,
) {
    /**
     * Retrieves the attribute value for the specified key.
     * If not set, it returns the [AttributeKey.defaultValue] defined in the key.
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T> getOrDefault(key: AttributeKey<T>): T {
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
    public fun <T> getOrNull(key: AttributeKey<T>): T? {
        return attributes[key] as T?
    }

    /**
     * Returns true if this container contains no attributes.
     */
    public fun isEmpty(): Boolean = attributes.isEmpty()

    /**
     * Returns true if this container has at least one attribute.
     */
    public fun isNotEmpty(): Boolean = attributes.isNotEmpty()

    /**
     * Returns the number of attributes stored in this container.
     */
    public val size: Int
        get() = attributes.size

    /**
     * Returns the set of attribute keys currently stored in this container.
     */
    public val keys: Set<AttributeKey<*>>
        get() = attributes.keys.toSet()

    /**
     * Returns true if this container contains the specified [key].
     */
    public fun containsKey(key: AttributeKey<*>): Boolean = attributes.containsKey(key)

    /**
     * Returns true if this container contains all of the specified [keys].
     * If no keys are specified, this returns true (vacuous truth).
     */
    public fun containsAll(vararg keys: AttributeKey<*>): Boolean = keys.all { containsKey(it) }

    /**
     * Returns true if this container contains any of the specified [keys].
     * If no keys are specified, this returns false.
     */
    public fun containsAny(vararg keys: AttributeKey<*>): Boolean = keys.any { containsKey(it) }

    /**
     * Returns a new [AttributeContainer] containing only the paragraph attributes.
     */
    internal fun filterParagraphAttributes(): AttributeContainer {
        return AttributeContainer(attributes = attributes.filterKeys { it is ParagraphAttributeKey<*> })
    }

    /**
     * Returns a new [AttributeContainer] containing only the non-paragraph (span) attributes.
     */
    internal fun filterSpanAttributes(): AttributeContainer {
        return AttributeContainer(attributes = attributes.filterKeys { it !is ParagraphAttributeKey<*> })
    }

    /**
     * Returns a new [AttributeContainer] with the specified [key] mapped to the [value].
     */
    public fun <T> plus(
        key: AttributeKey<T>,
        value: T,
    ): AttributeContainer {
        return plusAny(key, value)
    }

    private fun plusAny(key: AttributeKey<*>, value: Any?): AttributeContainer {
        val keysToRemove =
            if (key is ParagraphAttributeKey<*>) {
                when (key) {
                    is BlockTypeAttributeKey<*> -> keys.filterIsInstance<BlockTypeAttributeKey<*>>()
                    is AlignmentAttributeKey<*> -> keys.filterIsInstance<AlignmentAttributeKey<*>>()
                }
            } else {
                emptyList()
            }

        if (keysToRemove.isEmpty()) {
            return AttributeContainer(attributes = attributes + (key to value))
        }

        val keysToRemoveSet = keysToRemove.toSet()
        val filteredAttributes = attributes.filterKeys { it !in keysToRemoveSet }
        return AttributeContainer(attributes = filteredAttributes + (key to value))
    }

    /**
     * Returns a new [AttributeContainer] with the specified key-value [pair].
     */
    public operator fun <T> plus(pair: Pair<AttributeKey<T>, T>): AttributeContainer = plus(pair.first, pair.second)

    /**
     * Returns a new [AttributeContainer] containing all attributes from this and the [other].
     * If keys collide, values from [other] take precedence (overwrite).
     */
    public operator fun plus(other: AttributeContainer): AttributeContainer {
        if (other.attributes.isEmpty()) return this
        if (this.attributes.isEmpty()) return other

        var result = this
        for ((key, value) in other.attributes) {
            result = result.plusAny(key, value)
        }
        return result
    }

    /**
     * Returns a new [AttributeContainer] with the specified [key] removed.
     */
    public operator fun <T> minus(key: AttributeKey<T>): AttributeContainer {
        if (!attributes.containsKey(key)) return this
        return AttributeContainer(attributes = attributes - key)
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
        private val EMPTY = AttributeContainer(attributes = emptyMap())

        /**
         * Gets an empty [AttributeContainer].
         */
        public fun empty(): AttributeContainer = EMPTY
    }
}

/**
 * Returns an empty [AttributeContainer].
 */
public fun attributeContainerOf(): AttributeContainer = AttributeContainer.empty()

/**
 * Returns a new [AttributeContainer] containing the specified key-value [p1].
 */
public fun <T> attributeContainerOf(
    p1: Pair<AttributeKey<T>, T>,
): AttributeContainer = AttributeContainer.empty() + p1

/**
 * Returns a new [AttributeContainer] containing the specified key-value pairs.
 */
public fun <T1, T2> attributeContainerOf(
    p1: Pair<AttributeKey<T1>, T1>,
    p2: Pair<AttributeKey<T2>, T2>,
): AttributeContainer = AttributeContainer.empty() + p1 + p2

/**
 * Returns a new [AttributeContainer] containing the specified key-value pairs.
 */
public fun <T1, T2, T3> attributeContainerOf(
    p1: Pair<AttributeKey<T1>, T1>,
    p2: Pair<AttributeKey<T2>, T2>,
    p3: Pair<AttributeKey<T3>, T3>,
): AttributeContainer = AttributeContainer.empty() + p1 + p2 + p3

/**
 * Returns a new [AttributeContainer] containing the specified key-value pairs.
 */
public fun <T1, T2, T3, T4> attributeContainerOf(
    p1: Pair<AttributeKey<T1>, T1>,
    p2: Pair<AttributeKey<T2>, T2>,
    p3: Pair<AttributeKey<T3>, T3>,
    p4: Pair<AttributeKey<T4>, T4>,
): AttributeContainer = AttributeContainer.empty() + p1 + p2 + p3 + p4
