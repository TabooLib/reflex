package org.tabooproject.reflex.reflection

import org.tabooproject.reflex.*
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * @author 坏黑
 * @since 2022/1/21 7:11 PM
 */
@Internal
class InstantClassMethod(owner: LazyClass, private val method: Method) : JavaClassMethod(method.name, owner) {

    val annotationsLocal by lazy(LazyThreadSafetyMode.NONE) {
        method.declaredAnnotations.map { InstantAnnotation(it) }
    }

    val parameterLocal by lazy(LazyThreadSafetyMode.NONE) {
        val parameterAnnotations = method.parameterAnnotations
        method.parameterTypes.mapIndexed { idx, it -> LazyAnnotatedClass.of(it, parameterAnnotations[idx].map { i -> InstantAnnotation(i) }) }
    }

    override val result: LazyClass
        get() = LazyClass.of(method.returnType)

    override val parameter: List<LazyAnnotatedClass>
        get() = parameterLocal

    override val annotations: List<ClassAnnotation>
        get() = annotationsLocal

    override val isStatic: Boolean
        get() = Modifier.isStatic(method.modifiers)

    override val isFinal: Boolean
        get() = Modifier.isFinal(method.modifiers)

    override val isPublic: Boolean
        get() = Modifier.isPublic(method.modifiers)

    override val isProtected: Boolean
        get() = Modifier.isProtected(method.modifiers)

    override val isPrivate: Boolean
        get() = Modifier.isPrivate(method.modifiers)

    override val isNative: Boolean
        get() = Modifier.isNative(method.modifiers)

    override val isAbstract: Boolean
        get() = Modifier.isAbstract(method.modifiers)

    override val isVolatile: Boolean
        get() = Modifier.isVolatile(method.modifiers)

    override val isSynchronized: Boolean
        get() = Modifier.isSynchronized(method.modifiers)

    override fun toString(): String {
        return "InstantClassMethod(method=$method)"
    }
}