package org.tabooproject.reflex

import org.tabooproject.reflex.serializer.BinarySerializable

/**
 * @author 坏黑
 * @since 2022/1/21 6:41 PM
 */
abstract class ClassMethod(name: String, owner: LazyClass) : ClassMember(name, owner), BinarySerializable {

    abstract val result: LazyClass

    abstract val parameter: List<LazyAnnotatedClass>

    abstract val isNative: Boolean

    abstract val isAbstract: Boolean

    abstract val isVolatile: Boolean

    abstract val isSynchronized: Boolean

    abstract fun invoke(src: Any, vararg values: Any?): Any?

    abstract fun invokeStatic(vararg values: Any?): Any?

    val returnType: Class<*>
        get() = result.instance ?: Unknown::class.java

    val parameterTypes by lazy(LazyThreadSafetyMode.NONE) {
        parameter.map { p -> p.instance ?: Unknown::class.java }.toTypedArray()
    }

    override fun toString(): String {
        return "ClassMethod(result=$result)"
    }
}