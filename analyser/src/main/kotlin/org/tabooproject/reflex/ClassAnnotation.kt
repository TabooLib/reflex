package org.tabooproject.reflex

/**
 * @author 坏黑
 * @since 2022/1/24 7:42 PM
 */
abstract class ClassAnnotation(val source: LazyClass) {

    abstract fun <T> property(name: String): T?

    abstract fun properties(): Map<String, Any>

    abstract fun propertyKeys(): Set<String>

    override fun toString(): String {
        return "ClassAnnotation(source=$source, properties=${propertyKeys()})"
    }
}