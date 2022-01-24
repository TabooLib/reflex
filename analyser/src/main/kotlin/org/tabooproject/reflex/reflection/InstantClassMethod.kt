package org.tabooproject.reflex.reflection

import org.tabooproject.reflex.ClassAnnotation
import org.tabooproject.reflex.JavaClassMethod
import org.tabooproject.reflex.LazyAnnotatedClass
import org.tabooproject.reflex.LazyClass
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * @author 坏黑
 * @since 2022/1/21 7:11 PM
 */
class InstantClassMethod(owner: Class<*>, private val method: Method) : JavaClassMethod(method.name, owner) {

    val annotationsLocal by lazy {
        method.declaredAnnotations.map { InstantAnnotation(it) }
    }

    val parameterLocal by lazy {
        val parameterAnnotations = method.parameterAnnotations
        method.parameterTypes.mapIndexed { idx, it -> InstantAnnotatedClass(it, parameterAnnotations[idx].map { i -> InstantAnnotation(i) }) }
    }

    override val result: LazyClass
        get() = InstantClass(method.returnType)

    override val parameter: List<LazyAnnotatedClass>
        get() = parameterLocal

    override val annotations: List<ClassAnnotation>
        get() = annotationsLocal

    override val isStatic: Boolean
        get() = Modifier.isStatic(method.modifiers)

    override fun toString(): String {
        return "InstantClassMethod(method=$method)"
    }
}