package org.tabooproject.reflex

/**
 * @author 坏黑
 * @since 2022/1/21 6:31 PM
 */
abstract class ClassMember(val name: String, val owner: Class<*>) {

    abstract val annotations: List<ClassAnnotation>

    abstract val isStatic: Boolean

    fun getAnnotation(annotation: Class<out Annotation>): ClassAnnotation? {
        return annotations.firstOrNull { it.source.name == annotation.name }
    }

    override fun toString(): String {
        return "ClassMember(name='$name', owner=$owner, annotations=$annotations, isStatic=$isStatic)"
    }
}