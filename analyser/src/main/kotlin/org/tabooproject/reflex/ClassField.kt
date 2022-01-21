package org.tabooproject.reflex

/**
 * @author 坏黑
 * @since 2022/1/21 6:41 PM
 */
abstract class ClassField(name: String, owner: Class<*>) : ClassMember(name, owner) {

    abstract val type: LazyClass

    abstract fun get(src: Any? = null): Any?

    abstract fun set(src: Any? = null, value: Any?)

    fun setStatic(value: Any?) {
        set(StaticSrc, value)
    }

    val fieldType: Class<*>
        get() = type.instance ?: Unknown::class.java

    override fun toString(): String {
        return "ClassField(type=$type) ${super.toString()}"
    }
}