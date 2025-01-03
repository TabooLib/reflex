package org.tabooproject.reflex

import org.tabooproject.reflex.serializer.BinarySerializable

/**
 * @author 坏黑
 * @since 2022/1/21 6:41 PM
 */
abstract class ClassConstructor(name: String, owner: LazyClass) : ClassMember(name, owner), BinarySerializable {

    abstract val parameter: List<LazyAnnotatedClass>

    abstract fun instance(vararg values: Any?): Any?

    val parameterTypes by lazy(LazyThreadSafetyMode.NONE) {
        parameter.map { p -> p.instance ?: Unknown::class.java }.toTypedArray()
    }

    override fun toString(): String {
        return "ClassConstructor(parameter=$parameter)"
    }
}