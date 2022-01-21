package org.tabooproject.reflex.reflection

import org.tabooproject.reflex.JavaClassConstructor
import org.tabooproject.reflex.LazyClass
import java.lang.reflect.Constructor

/**
 * @author 坏黑
 * @since 2022/1/21 7:11 PM
 */
class InstantClassConstructor(owner: Class<*>, private val constructor: Constructor<*>) : JavaClassConstructor("<init>", owner) {

    override val isStatic: Boolean
        get() = true

    override val parameter: List<LazyClass>
        get() = constructor.parameterTypes.map { InstantClass(it) }

    override fun toString(): String {
        return "InstantClassConstructor(constructor=$constructor)"
    }
}