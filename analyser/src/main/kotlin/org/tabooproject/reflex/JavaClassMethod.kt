package org.tabooproject.reflex

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodType

/**
 * @author 坏黑
 * @since 2022/1/21 10:17 PM
 */
@Internal
abstract class JavaClassMethod(name: String, owner: Class<*>) : ClassMethod(name, owner) {

    private val handle: MethodHandle by lazy {
        if (isStatic) {
            UnsafeAccess.lookup.findStatic(owner, name, MethodType.methodType(returnType, parameterTypes))
        } else {
            UnsafeAccess.lookup.findVirtual(owner, name, MethodType.methodType(returnType, parameterTypes))
        }
    }

    override fun invoke(src: Any, vararg values: Any?): Any? {
        if (returnType == Unknown::class.java) {
            throw NoClassDefFoundError(result.name)
        }
        if (parameterTypes.any { it == Unknown::class.java }) {
            throw NoClassDefFoundError(parameterTypes.joinToString(";") { it.name })
        }
        return if (isStatic) {
            handle.invokeWithArguments(*values)
        } else {
            try {
                handle.bindTo(src).invokeWithArguments(*values)
            } catch (ex: ClassCastException) {
                if (src == StaticSrc) {
                    throw IllegalStateException("$name is not a static method", ex)
                } else {
                    throw IllegalStateException("${src.javaClass.name} is not an instance of ${owner.name}", ex)
                }
            }
        }
    }

    override fun invokeStatic(vararg values: Any?): Any? {
        return invoke(StaticSrc, *values)
    }
}