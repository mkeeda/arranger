package dev.mkeeda.arranger.core.text

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
     * Returns a new [AttributeContainer] with the specified [key] mapped to the [value].
     */
    public fun <T> plus(
        key: AttributeKey<T>,
        value: T,
    ): AttributeContainer {
        return AttributeContainer(attributes = attributes + (key to value))
    }

    /**
     * Returns a new [AttributeContainer] with the specified key-value [pair].
     */
    public operator fun <T> plus(pair: Pair<AttributeKey<T>, T>): AttributeContainer = plus(pair.first, pair.second)

    /**
     * Returns a new [AttributeContainer] with the specified [key] removed.
     */
    public operator fun <T> minus(key: AttributeKey<T>): AttributeContainer {
        if (!attributes.containsKey(key)) return this
        return AttributeContainer(attributes = attributes - key)
    }

    /**
     * Returns a new [AttributeContainer] containing all attributes from this and the [other].
     * If keys collide, values from [other] take precedence (overwrite).
     */
    public operator fun plus(other: AttributeContainer): AttributeContainer {
        if (other.attributes.isEmpty()) return this
        if (this.attributes.isEmpty()) return other
        return AttributeContainer(attributes = attributes + other.attributes)
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
