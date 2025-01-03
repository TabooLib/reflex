package org.tabooproject.reflex

import org.tabooproject.reflex.serializer.BinaryWriter
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
    classFinder: ClassAnalyser.ClassFinder?,
    getter: Supplier<Class<*>?>,
    val annotations: List<ClassAnnotation>,
) : LazyClass(source, isArray, isInstant, classFinder, getter) {

    fun getAnnotation(annotation: Class<out Annotation>): ClassAnnotation {
        return annotations.first { it.source.name == annotation.name }
    }

    fun isAnnotationPresent(annotation: Class<out Annotation>): Boolean {
        return annotations.any { it.source.name == annotation.name }
    }

    override fun toString(): String {
        return "LazyAnnotatedClass(${if (isArray) "Array[$name]" else name},@${annotations})"
    }

    override fun writeTo(writer: BinaryWriter) {
        writer.writeInt(2) // 2：表示 LazyAnnotatedClass
        writer.writeNullableString(name)
        writer.writeBoolean(isArray)
        writer.writeBoolean(isInstant)
        writer.writeList(annotations)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LazyAnnotatedClass) return false
        if (!super.equals(other)) return false
        if (annotations != other.annotations) return false
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + annotations.hashCode()
        return result
    }

    companion object {

        fun of(clazz: Class<*>, annotations: List<ClassAnnotation>): LazyAnnotatedClass {
            return LazyAnnotatedClass(clazz.name, isArray = false, isInstant = true, classFinder = null, getter = { clazz }, annotations = annotations)
        }

        fun of(clazz: Class<*>, annotations: List<ClassAnnotation>, isArray: Boolean): LazyAnnotatedClass {
            return LazyAnnotatedClass(clazz.name, isArray, true, classFinder = null, { clazz }, annotations)
        }

        fun of(source: String, annotations: List<ClassAnnotation>): LazyAnnotatedClass {
            return LazyAnnotatedClass(
                source,
                isArray = false,
                isInstant = false,
                classFinder = null,
                getter = { runCatching { Class.forName(source) }.getOrNull() },
                annotations
            )
        }

        fun of(source: String, annotations: List<ClassAnnotation>, classFinder: ClassAnalyser.ClassFinder): LazyAnnotatedClass {
            return LazyAnnotatedClass(
                source,
                isArray = false,
                isInstant = false,
                classFinder,
                getter = { classFinder.findClass(source.replace('/', '.')) },
                annotations
            )
        }

        fun of(source: String, annotations: List<ClassAnnotation>, isArray: Boolean, classFinder: ClassAnalyser.ClassFinder): LazyAnnotatedClass {
            return LazyAnnotatedClass(
                source,
                isArray,
                isInstant = false,
                classFinder,
                getter = { classFinder.findClass(source.replace('/', '.')) },
                annotations
            )
        }

        fun of(source: String, getter: Supplier<Class<*>?>, annotations: List<ClassAnnotation>): LazyAnnotatedClass {
            return LazyAnnotatedClass(source, isArray = false, isInstant = false, classFinder = null, getter, annotations)
        }
    }
}