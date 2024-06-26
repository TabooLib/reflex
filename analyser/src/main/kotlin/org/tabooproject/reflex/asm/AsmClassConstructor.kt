package org.tabooproject.reflex.asm

import org.tabooproject.reflex.ClassAnnotation
import org.tabooproject.reflex.Internal
import org.tabooproject.reflex.JavaClassConstructor
import org.tabooproject.reflex.LazyAnnotatedClass
import org.tabooproject.reflex.reflection.InstantAnnotatedClass
import org.tabooproject.reflex.reflection.InstantClass
import java.lang.reflect.Modifier

/**
 * @author 坏黑
 * @since 2022/1/21 6:34 PM
 */
@Internal
class AsmClassConstructor(
    name: String,
    owner: Class<*>,
    val descriptor: String,
    val access: Int,
    val parameterAnnotations: Map<Int, ArrayList<AsmAnnotation>>,
    override val annotations: List<ClassAnnotation>,
) : JavaClassConstructor(name, owner) {

    val localParameter = AsmSignature.signatureToClass(descriptor).mapIndexed { idx, it ->
        if (it is InstantClass) {
            InstantAnnotatedClass(it.instance, parameterAnnotations[idx] ?: emptyList())
        } else {
            LazyAnnotatedClass(it.name, parameterAnnotations[idx] ?: emptyList())
        }
    }

    override val parameter: List<LazyAnnotatedClass>
        get() = localParameter

    override val isStatic: Boolean
        get() = true

    override val isFinal: Boolean
        get() = true

    override val isPublic: Boolean
        get() = Modifier.isPublic(access)

    override val isProtected: Boolean
        get() = Modifier.isProtected(access)

    override val isPrivate: Boolean
        get() = Modifier.isPrivate(access)

    override fun toString(): String {
        return "AsmClassConstructor(descriptor='$descriptor', access=$access) ${super.toString()}"
    }
}