package org.tabooproject.reflex.asm

import org.tabooproject.reflex.ClassAnnotation

/**
 * @author 坏黑
 * @since 2022/1/24 8:48 PM
 */
class AsmAnnotation(val annotationVisitor: AsmClassAnnotationVisitor) : ClassAnnotation(annotationVisitor.source) {

    @Suppress("UNCHECKED_CAST")
    override fun <T> property(name: String): T? {
        return annotationVisitor.map[name] as? T?
    }

    override fun properties(): Map<String, Any> {
        return annotationVisitor.map
    }

    override fun propertyKeys(): Set<String> {
        return annotationVisitor.map.keys
    }

    override fun toString(): String {
        return "AsmAnnotation() ${super.toString()}"
    }

    companion object {

        private val internalMethods = arrayOf("equals", "hashCode", "toString")
    }
}