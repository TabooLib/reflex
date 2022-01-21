package org.tabooproject.reflex

/**
 * @author 坏黑
 * @since 2022/1/21 6:41 PM
 */
abstract class ClassMethod(name: String, owner: Class<*>) : ClassMember(name, owner) {

    abstract val result: LazyClass
    abstract val parameter: List<LazyClass>

    abstract fun invoke(src: Any, vararg values: Any?): Any?

    abstract fun invokeStatic(vararg values: Any?): Any?

    val returnType: Class<*>
        get() = result.instance ?: Unknown::class.java

    val parameterTypes: Array<Class<*>>
        get() = parameter.map { p -> p.instance ?: Unknown::class.java }.toTypedArray()

    override fun toString(): String {
        return "ClassMethod(result=$result)"
    }
}