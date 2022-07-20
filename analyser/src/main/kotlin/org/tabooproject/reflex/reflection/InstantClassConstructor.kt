package org.tabooproject.reflex.reflection

import org.tabooproject.reflex.ClassAnnotation
import org.tabooproject.reflex.Internal
import org.tabooproject.reflex.JavaClassConstructor
import org.tabooproject.reflex.LazyAnnotatedClass
import java.lang.reflect.Constructor

/**
 * @author 坏黑
 * @since 2022/1/21 7:11 PM
 */
@Internal
class InstantClassConstructor(owner: Class<*>, private val constructor: Constructor<*>) : JavaClassConstructor("<init>", owner) {

    val annotationsLocal by lazy(LazyThreadSafetyMode.NONE) {
        constructor.declaredAnnotations.map { InstantAnnotation(it) }
    }

    val parameterLocal by lazy(LazyThreadSafetyMode.NONE) {
        val parameterAnnotations = constructor.parameterAnnotations
        constructor.parameterTypes.mapIndexed { idx, it -> InstantAnnotatedClass(it, parameterAnnotations[idx].map { i -> InstantAnnotation(i) }) }
    }

    override val isStatic: Boolean
        get() = true

    override val parameter: List<LazyAnnotatedClass>
        get() = parameterLocal

    override val annotations: List<ClassAnnotation>
        get() = annotationsLocal

    override fun toString(): String {
        return "InstantClassConstructor(constructor=$constructor)"
    }
}