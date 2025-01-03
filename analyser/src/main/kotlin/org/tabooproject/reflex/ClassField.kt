package org.tabooproject.reflex

import org.tabooproject.reflex.serializer.BinarySerializable

/**
 * @author 坏黑
 * @since 2022/1/21 6:41 PM
 */
abstract class ClassField(name: String, owner: LazyClass) : ClassMember(name, owner), BinarySerializable {

    abstract val type: LazyClass

    abstract val isTransient: Boolean

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