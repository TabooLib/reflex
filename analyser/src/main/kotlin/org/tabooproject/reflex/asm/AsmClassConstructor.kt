package org.tabooproject.reflex.asm

import org.tabooproject.reflex.*
import org.tabooproject.reflex.serializer.BinaryWriter
import java.lang.reflect.Modifier

/**
 * @author 坏黑
 * @since 2022/1/21 6:34 PM
 */
@Internal
class AsmClassConstructor(
    name: String,
    owner: LazyClass,
    val descriptor: String,
    val access: Int,
    val parameterAnnotations: Map<Int, List<ClassAnnotation>>,
    val classFinder: ClassAnalyser.ClassFinder,
    override val annotations: List<ClassAnnotation>,
    override val parameter: List<LazyAnnotatedClass> = AsmSignature.signatureToClass(descriptor, classFinder)
        .mapIndexed { idx, it ->
            LazyAnnotatedClass(
                it.name,
                it.dimensions,
                it.isInstant,
                it.isPrimitive,
                it.classGetter,
                annotations = parameterAnnotations[idx] ?: emptyList(),
                it.name,
                it.simpleName
            )
        },
) : JavaClassConstructor(name, owner) {

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

    override fun writeTo(writer: BinaryWriter) {
        writer.writeInt(1) // 1: ASM MODE
        // 函数信息
        AsmClassMethod.writeTo(writer, name, owner, descriptor, access, parameterAnnotations, annotations, parameter)
    }
}