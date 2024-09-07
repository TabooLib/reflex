package org.tabooproject.reflex

import java.util.function.Supplier

/**
 * @author 坏黑
 * @since 2022/1/21 6:47 PM
 */
@Internal
open class LazyAnnotatedClass protected constructor(
    source: String,
    isArray: Boolean,
    isInstant: Boolean,
    getter: Supplier<Class<*>?>,
    val annotations: List<ClassAnnotation>
) : LazyClass(source, isArray, isInstant, getter) {

    fun getAnnotation(annotation: Class<out Annotation>): ClassAnnotation {
        return annotations.first { it.source.name == annotation.name }
    }

    fun isAnnotationPresent(annotation: Class<out Annotation>): Boolean {
        return annotations.any { it.source.name == annotation.name }
    }

    override fun toString(): String {
        return "LazyAnnotatedClass(${if (isArray) "Array[$name]" else name},@${annotations})"
    }

    companion object {

        fun of(clazz: Class<*>, annotations: List<ClassAnnotation>): LazyAnnotatedClass {
            return LazyAnnotatedClass(clazz.name, isArray = false, isInstant = true, getter = { clazz }, annotations = annotations)
        }

        fun of(clazz: Class<*>, annotations: List<ClassAnnotation>, isArray: Boolean): LazyAnnotatedClass {
            return LazyAnnotatedClass(clazz.name, isArray, true, { clazz }, annotations)
        }

        fun of(source: String, annotations: List<ClassAnnotation>): LazyAnnotatedClass {
            return LazyAnnotatedClass(source, isArray = false, isInstant = false, getter = { runCatching { Class.forName(source) }.getOrNull() }, annotations = annotations)
        }

        fun of(source: String, annotations: List<ClassAnnotation>, classFinder: ClassAnalyser.ClassFinder): LazyAnnotatedClass {
            return LazyAnnotatedClass(source, isArray = false, isInstant = false, getter = { classFinder.findClass(source.replace('/', '.')) }, annotations = annotations)
        }

        fun of(source: String, annotations: List<ClassAnnotation>, isArray: Boolean, classFinder: ClassAnalyser.ClassFinder): LazyAnnotatedClass {
            return LazyAnnotatedClass(source, isArray, false, { classFinder.findClass(source.replace('/', '.')) }, annotations)
        }

        fun of(source: String, getter: Supplier<Class<*>?>, annotations: List<ClassAnnotation>): LazyAnnotatedClass {
            return LazyAnnotatedClass(source, isArray = false, isInstant = false, getter = getter, annotations = annotations)
        }
    }
}