package org.tabooproject.reflex.reflection

import org.tabooproject.reflex.JavaClassMethod
import org.tabooproject.reflex.LazyClass
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * @author 坏黑
 * @since 2022/1/21 7:11 PM
 */
class InstantClassMethod(owner: Class<*>, private val method: Method) : JavaClassMethod(method.name, owner) {

    override val result: LazyClass
        get() = InstantClass(method.returnType)

    override val parameter: List<LazyClass>
        get() = method.parameterTypes.map { InstantClass(it) }

    override val isStatic: Boolean
        get() = Modifier.isStatic(method.modifiers)

    override fun toString(): String {
        return "InstantClassMethod(method=$method)"
    }
}