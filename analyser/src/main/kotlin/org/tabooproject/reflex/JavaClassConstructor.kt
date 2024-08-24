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

    override fun instance(vararg values: Any?): Any? {
        // 检查无效参数
        if (parameterTypes.any { it == Unknown::class.java }) {
            throw NoClassDefFoundError(parameterTypes.joinToString(";") { it.name })
        }
        return handle.invokeWithArguments(*values)
    }
}