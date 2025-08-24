package org.tabooproject.reflex

import java.lang.invoke.MethodType

/**
 * @author 坏黑
 * @since 2022/1/21 10:15 PM
 */
@Internal
abstract class JavaClassConstructor(name: String, owner: LazyClass) : ClassConstructor(name, owner) {

    private val handle by lazy(LazyThreadSafetyMode.NONE) {
        UnsafeAccess.lookup.findConstructor(owner.instance, MethodType.methodType(Void.TYPE, parameterTypes))
    }

    private val optimizedInvoker by lazy(LazyThreadSafetyMode.NONE) {
        if (parameterTypes.isEmpty()) {
            handle.asType(handle.type().changeReturnType(Any::class.java))
        } else {
            val spreader = handle.asSpreader(Array<Any?>::class.java, parameterTypes.size)
            spreader.asType(spreader.type().wrap().changeReturnType(Any::class.java))
        }
    }

    override fun instance(vararg values: Any?): Any? {
        // 检查无效参数
        if (parameterTypes.any { it == Unknown::class.java }) {
            throw NoClassDefFoundError(parameterTypes.joinToString(";") { it.name })
        }
        return try {
            if (parameterTypes.isEmpty()) {
                optimizedInvoker.invoke()
            } else {
                optimizedInvoker.invoke(values as Array<Any?>)
            }
        } catch (ex: Throwable) {
            if (ex is RuntimeException || ex is Error) {
                throw ex
            }
            throw RuntimeException(ex)
        }
    }
}