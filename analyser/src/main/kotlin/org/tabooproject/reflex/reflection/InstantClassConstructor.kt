package org.tabooproject.reflex.reflection

import org.tabooproject.reflex.*
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

/**
 * @author 坏黑
 * @since 2022/1/21 7:11 PM
 */
@Internal
class InstantClassConstructor(owner: LazyClass, private val constructor: Constructor<*>) : JavaClassConstructor("<init>", owner) {

    val annotationsLocal by lazy(LazyThreadSafetyMode.NONE) {
        constructor.declaredAnnotations.map { InstantAnnotation(it) }
    }

    val parameterLocal by lazy(LazyThreadSafetyMode.NONE) {
        val parameterAnnotations = constructor.parameterAnnotations
        constructor.parameterTypes.mapIndexed { idx, it -> LazyAnnotatedClass.of(it, parameterAnnotations[idx].map { i -> InstantAnnotation(i) }) }
    }

    override val isStatic: Boolean
        get() = true

    override val isFinal: Boolean
        get() = true

    override val isPublic: Boolean
        get() = Modifier.isPublic(constructor.modifiers)

    override val isProtected: Boolean
        get() = Modifier.isProtected(constructor.modifiers)

    override val isPrivate: Boolean
        get() = Modifier.isPrivate(constructor.modifiers)

    override val parameter: List<LazyAnnotatedClass>
        get() = parameterLocal

    override val annotations: List<ClassAnnotation>
        get() = annotationsLocal

    override fun toString(): String {
        return "InstantClassConstructor(constructor=$constructor)"
    }
}