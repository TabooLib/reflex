package org.tabooproject.reflex.reflection

import org.tabooproject.reflex.ClassAnnotation
import org.tabooproject.reflex.Internal
import org.tabooproject.reflex.LazyClass

/**
 * @author 坏黑
 * @since 2022/1/24 8:48 PM
 */
@Internal
class InstantAnnotation(val annotation: Annotation) : ClassAnnotation(LazyClass.of(annotation.annotationClass.java)) {

    val methods = source.instance!!.methods.filter { it.name !in internalMethods }.associateBy { it.name }

    @Suppress("UNCHECKED_CAST")
    override fun <T> property(name: String): T? {
        return methods[name]?.invoke(annotation) as? T
    }

    override fun <T> property(name: String, def: T): T {
        return property(name) ?: def
    }

    override fun properties(): Map<String, Any> {
        return methods.mapValues { it.value.invoke(annotation) }
    }

    override fun propertyKeys(): Set<String> {
        return methods.keys
    }

    override fun toString(): String {
        return "InstantAnnotation() ${super.toString()}"
    }

    companion object {

        private val internalMethods = arrayOf("equals", "hashCode", "toString", "annotationType")
    }
}