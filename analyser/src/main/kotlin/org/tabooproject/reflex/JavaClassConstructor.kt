package org.tabooproject.reflex

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodType

/**
 * @author 坏黑
 * @since 2022/1/21 10:15 PM
 */
@Internal
abstract class JavaClassConstructor(name: String, owner: Class<*>) : ClassConstructor(name, owner) {

    private val handle: MethodHandle by lazy {
        UnsafeAccess.lookup.findConstructor(owner, MethodType.methodType(Void.TYPE, parameterTypes))
    }

    override fun instance(vararg values: Any?): Any? {
        if (parameterTypes.any { it == Unknown::class.java }) {
            throw NoClassDefFoundError(parameterTypes.joinToString(";") { it.name })
        }
        return handle.invokeWithArguments(*values)
    }
}