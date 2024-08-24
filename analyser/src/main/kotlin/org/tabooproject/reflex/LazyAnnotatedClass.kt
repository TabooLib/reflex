package org.tabooproject.reflex

import java.util.function.Supplier

/**
 * @author 坏黑
 * @since 2022/1/21 6:47 PM
 */
@Internal
open class LazyAnnotatedClass protected constructor(
    source: String,
    isInstant: Boolean,
    getter: Supplier<Class<*>?>,
    val annotations: List<ClassAnnotation>
) : LazyClass(source, isInstant, getter) {

    fun getAnnotation(annotation: Class<out Annotation>): ClassAnnotation {
        return annotations.first { it.source.name == annotation.name }
    }

    fun isAnnotationPresent(annotation: Class<out Annotation>): Boolean {
        return annotations.any { it.source.name == annotation.name }
    }

    override fun toString(): String {
        return "LazyAnnotatedClass(annotations=$annotations)"
    }

    companion object {

        fun of(clazz: Class<*>, annotations: List<ClassAnnotation>): LazyAnnotatedClass {
            return LazyAnnotatedClass(clazz.name, true, { clazz }, annotations)
        }

        fun of(source: String, annotations: List<ClassAnnotation>): LazyAnnotatedClass {
            return LazyAnnotatedClass(source, false, { runCatching { Class.forName(source) }.getOrNull() }, annotations)
        }

        fun of(source: String, annotations: List<ClassAnnotation>, classFinder: ClassAnalyser.ClassFinder): LazyAnnotatedClass {
            return LazyAnnotatedClass(source, false, { classFinder.findClass(source) }, annotations)
        }

        fun of(source: String, getter: Supplier<Class<*>?>, annotations: List<ClassAnnotation>): LazyAnnotatedClass {
            return LazyAnnotatedClass(source, false, getter, annotations)
        }
    }
}