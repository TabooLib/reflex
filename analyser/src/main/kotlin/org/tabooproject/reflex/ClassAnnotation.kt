package org.tabooproject.reflex

/**
 * @author 坏黑
 * @since 2022/1/24 7:42 PM
 */
abstract class ClassAnnotation(val source: LazyClass) {

    abstract fun <T> property(name: String): T?

    abstract fun <T> property(name: String, def: T): T

    abstract fun properties(): Map<String, Any>

    abstract fun propertyKeys(): Set<String>

    @Suppress("UNCHECKED_CAST")
    fun <T> enum(name: String): T {
        val value = property<Any>(name)!!
        return if (value is LazyEnum) value.instance as T else value as T
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> enum(name: String, def: Any): T {
        val value = property(name, def)
        return if (value is LazyEnum) value.instance as T else value as T
    }

    override fun toString(): String {
        return "ClassAnnotation(source=$source, properties=${propertyKeys()})"
    }
}