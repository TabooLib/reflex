package org.tabooproject.reflex

import org.tabooproject.reflex.serializer.BinaryWriter
import java.util.function.Supplier

/**
 * @author 坏黑
 * @since 2022/1/21 6:47 PM
 */
@Internal
open class LazyAnnotatedClass internal constructor(
    source: String,
    dimensions: Int,
    isInstant: Boolean,
    isPrimitive: Boolean,
    classGetter: Supplier<Class<*>?>,
    val annotations: List<ClassAnnotation>,
    name: String = source.replace('/', '.'),
    simpleName: String = name.substringAfterLast('.'),
) : LazyClass(source, dimensions, isInstant, isPrimitive, classGetter, name, simpleName) {

    fun getAnnotation(annotation: Class<out Annotation>): ClassAnnotation {
        return annotations.first { it.source.name == annotation.name }
    }

    fun isAnnotationPresent(annotation: Class<out Annotation>): Boolean {
        return annotations.any { it.source.name == annotation.name }
    }

    override fun toString(): String {
        return "LazyAnnotatedClass(${"[".repeat(dimensions)}$name,@${annotations})"
    }

    override fun writeTo(writer: BinaryWriter) {
        writer.writeInt(2) // 2：表示 LazyAnnotatedClass
        writer.writeNullableString(name)
        writer.writeNullableString(simpleName)
        writer.writeInt(dimensions)
        writer.writeBoolean(isInstant)
        writer.writeBoolean(isPrimitive)
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

        fun of(clazz: Class<*>, dimensions: Int = clazz.getArrayDimensions(), annotations: List<ClassAnnotation>): LazyAnnotatedClass {
            return LazyAnnotatedClass(clazz.name, dimensions, isInstant = true, clazz.isPrimitive, classGetter = { clazz }, annotations = annotations)
        }

        fun of(source: String, dimensions: Int = 0, isPrimitive: Boolean = false, annotations: List<ClassAnnotation>): LazyAnnotatedClass {
            return LazyAnnotatedClass(
                source,
                dimensions,
                isInstant = false,
                isPrimitive,
                classGetter = { runCatching { Class.forName(source) }.getOrNull() },
                annotations
            )
        }

        fun of(source: String, dimensions: Int = 0, isPrimitive: Boolean = false, annotations: List<ClassAnnotation>, classFinder: ClassAnalyser.ClassFinder?): LazyAnnotatedClass {
            val finder = classFinder ?: ClassAnalyser.ClassFinder.default
            return LazyAnnotatedClass(
                source,
                dimensions = 0,
                isInstant = false,
                isPrimitive,
                classGetter = { finder.findClass(source.replace('/', '.')) },
                annotations
            )
        }

        fun of(source: String, getter: Supplier<Class<*>?>, annotations: List<ClassAnnotation>): LazyAnnotatedClass {
            return LazyAnnotatedClass(source, dimensions = 0, isInstant = false, isPrimitive = false, getter, annotations)
        }
    }
}