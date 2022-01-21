package org.tabooproject.reflex

/**
 * @author 坏黑
 * @since 2022/1/21 6:31 PM
 */
abstract class ClassMember(val name: String, val owner: Class<*>) {

    abstract val isStatic: Boolean

    override fun toString(): String {
        return "ClassMember(name='$name', isStatic=$isStatic)"
    }
}