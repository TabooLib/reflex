package org.tabooproject.reflex

/**
 * @author 坏黑
 * @since 2022/1/21 6:47 PM
 */
@Internal
open class LazyAnnotatedClass(name: String, val annotations: List<ClassAnnotation>): LazyClass(name) {

    fun getAnnotation(annotation: Class<out Annotation>): ClassAnnotation {
        return annotations.first { it.source.name == annotation.name }
    }

    fun isAnnotationPresent(annotation: Class<out Annotation>): Boolean {
        return annotations.any { it.source.name == annotation.name }
    }

    override fun toString(): String {
        return "LazyAnnotatedClass(annotations=$annotations) ${super.toString()}"
    }
}