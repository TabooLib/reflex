package org.tabooproject.reflex

import java.lang.invoke.MethodType

/**
 * @author 坏黑
 * @since 2022/1/21 10:17 PM
 */
@Internal
abstract class JavaClassMethod(name: String, owner: LazyClass) : ClassMethod(name, owner) {

    private val handle by lazy(LazyThreadSafetyMode.NONE) {
        if (isStatic) {
            UnsafeAccess.lookup.findStatic(owner.instance, name, MethodType.methodType(returnType, parameterTypes))
        } else {
            UnsafeAccess.lookup.findVirtual(owner.instance, name, MethodType.methodType(returnType, parameterTypes))
        }
    }

    private val optimizedInvoker by lazy(LazyThreadSafetyMode.NONE) {
        when {
            isStatic -> {
                if (parameterTypes.isEmpty()) {
                    handle.asType(handle.type().generic())
                } else {
                    val spreader = handle.asSpreader(Array<Any?>::class.java, parameterTypes.size)
                    spreader.asType(spreader.type().generic())
                }
            }
            parameterTypes.isEmpty() -> {
                handle.asType(handle.type().generic())
            }
            else -> {
                val spreader = handle.asSpreader(Array<Any?>::class.java, parameterTypes.size)
                spreader.asType(spreader.type().generic())
            }
        }
    }

    override fun invoke(src: Any, vararg values: Any?): Any? {
        if (returnType == Unknown::class.java) {
            throw NoClassDefFoundError(result.name)
        }
        if (parameterTypes.any { it == Unknown::class.java }) {
            throw NoClassDefFoundError(parameterTypes.joinToString(";") { it.name })
        }
        return try {
            when {
                isStatic -> {
                    if (parameterTypes.isEmpty()) {
                        optimizedInvoker.invoke()
                    } else {
                        optimizedInvoker.invoke(values as Array<Any?>)
                    }
                }
                parameterTypes.isEmpty() -> {
                    optimizedInvoker.invoke(src)
                }
                else -> {
                    val boundHandle = optimizedInvoker.bindTo(src)
                    boundHandle.invoke(values as Array<Any?>)
                }
            }
        } catch (ex: ClassCastException) {
            if (!isStatic && src == StaticSrc) {
                throw IllegalStateException("$name is not a static method", ex)
            } else if (!isStatic) {
                throw IllegalStateException("${src.javaClass.name} is not an instance of ${owner.name}", ex)
            } else {
                throw ex
            }
        } catch (ex: Throwable) {
            if (ex is RuntimeException || ex is Error) {
                throw ex
            }
            throw RuntimeException(ex)
        }
    }

    override fun invokeStatic(vararg values: Any?): Any? {
        return invoke(StaticSrc, *values)
    }
}