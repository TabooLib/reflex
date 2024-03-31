package org.tabooproject.reflex

/**
 * @author 坏黑
 * @since 2022/1/21 6:31 PM
 */
abstract class ClassMember(val name: String, val owner: Class<*>) {

    abstract val annotations: List<ClassAnnotation>

    abstract val isStatic: Boolean

    abstract val isFinal: Boolean

    abstract val isPublic: Boolean

    abstract val isProtected: Boolean

    abstract val isPrivate: Boolean

    fun getAnnotation(annotation: Class<out Annotation>): ClassAnnotation {
        return annotations.first { it.source.name == annotation.name }
    }

    fun isAnnotationPresent(annotation: Class<out Annotation>): Boolean {
        return annotations.any { it.source.name == annotation.name }
    }

    override fun toString(): String {
        return "ClassMember(name='$name', owner=$owner, annotations=$annotations, isStatic=$isStatic)"
    }
}