package org.tabooproject.reflex.asm

import org.tabooproject.reflex.*
import org.tabooproject.reflex.serializer.BinaryWriter
import java.lang.reflect.Modifier

/**
 * @author 坏黑
 * @since 2022/1/21 6:33 PM
 */
@Internal
class AsmClassField(
    name: String,
    owner: LazyClass,
    val descriptor: String,
    val access: Int,
    val classFinder: ClassAnalyser.ClassFinder,
    override val annotations: List<ClassAnnotation>,
    override val type: LazyClass = AsmSignature.signatureToClass(descriptor, classFinder).first(),
) : JavaClassField(name, owner) {

    override val isStatic: Boolean
        get() = Modifier.isStatic(access)

    override val isTransient: Boolean
        get() = Modifier.isTransient(access)

    override val isFinal: Boolean
        get() = Modifier.isFinal(access)

    override val isPublic: Boolean
        get() = Modifier.isPublic(access)

    override val isProtected: Boolean
        get() = Modifier.isProtected(access)

    override val isPrivate: Boolean
        get() = Modifier.isPrivate(access)

    override fun toString(): String {
        return "AsmClassField(descriptor='$descriptor', access=$access) ${super.toString()}"
    }

    override fun writeTo(writer: BinaryWriter) {
        writer.writeInt(1) // 1: ASM MODE
        writer.writeNullableString(name)
        writer.writeObj(owner)
        writer.writeNullableString(descriptor)
        writer.writeInt(access)
        writer.writeList(annotations)
        writer.writeObj(type)
    }
}