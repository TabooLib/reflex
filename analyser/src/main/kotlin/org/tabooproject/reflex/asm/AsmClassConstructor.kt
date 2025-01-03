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
    val parameterAnnotations: Map<Int, ArrayList<AsmAnnotation>>,
    val classFinder: ClassAnalyser.ClassFinder,
    override val annotations: List<ClassAnnotation>,
) : JavaClassConstructor(name, owner) {

    val localParameter = AsmSignature.signatureToClass(descriptor, classFinder).mapIndexed { idx, it ->
        if (it.isInstant) {
            LazyAnnotatedClass.of(it.instance!!, parameterAnnotations[idx] ?: emptyList())
        } else {
            LazyAnnotatedClass.of(it.name, parameterAnnotations[idx] ?: emptyList(), classFinder)
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

    override fun writeTo(writer: BinaryWriter) {
        writer.writeNullableString(name)
        writer.writeObj(owner)
        writer.writeNullableString(descriptor)
        writer.writeInt(access)
        // 参数注解
        writer.writeInt(parameter.size)
        parameterAnnotations.forEach { (k, v) ->
            writer.writeInt(k)
            writer.writeList(v)
        }
        // 注解
        writer.writeList(annotations)
        // 参数
        writer.writeList(parameter)
    }
}