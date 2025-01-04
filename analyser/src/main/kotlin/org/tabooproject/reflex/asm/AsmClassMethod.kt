package org.tabooproject.reflex.asm

import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.signature.SignatureWriter
import org.tabooproject.reflex.*
import org.tabooproject.reflex.serializer.BinaryReader
import org.tabooproject.reflex.serializer.BinaryWriter
import java.lang.reflect.Modifier

/**
 * @author 坏黑
 * @since 2022/1/21 6:34 PM
 */
@Internal
class AsmClassMethod(
    name: String,
    owner: LazyClass,
    val descriptor: String,
    val access: Int,
    val parameterAnnotations: Map<Int, List<ClassAnnotation>>,
    val classFinder: ClassAnalyser.ClassFinder,
    override val annotations: List<ClassAnnotation>,
    // 延迟加载的数据
    private var localResult: LazyClass? = null,
    private val localParameter: MutableList<LazyAnnotatedClass> = ArrayList<LazyAnnotatedClass>(),
) : JavaClassMethod(name, owner) {

    override val result: LazyClass
        get() = localResult!!

    override val parameter: List<LazyAnnotatedClass>
        get() = localParameter

    override val isStatic: Boolean
        get() = Modifier.isStatic(access)

    override val isFinal: Boolean
        get() = Modifier.isFinal(access)

    override val isPublic: Boolean
        get() = Modifier.isPublic(access)

    override val isProtected: Boolean
        get() = Modifier.isProtected(access)

    override val isPrivate: Boolean
        get() = Modifier.isPrivate(access)

    override val isNative: Boolean
        get() = Modifier.isNative(access)

    override val isAbstract: Boolean
        get() = Modifier.isAbstract(access)

    override val isVolatile: Boolean
        get() = Modifier.isVolatile(access)

    override val isSynchronized: Boolean
        get() = Modifier.isSynchronized(access)

    fun read() {
        var visitParameterType = false
        var visitReturnType = false
        var dimensions = 0
        SignatureReader(descriptor).accept(object : SignatureWriter() {

            override fun visitParameterType(): SignatureVisitor {
                // println("  visitParameterType")
                visitParameterType = true
                visitReturnType = false
                return super.visitParameterType()
            }

            override fun visitReturnType(): SignatureVisitor {
                // println("  visitReturnType")
                visitParameterType = false
                visitReturnType = true
                return super.visitReturnType()
            }

            override fun visitClassType(name: String) {
                // println("  visitClassType $name")
                if (visitParameterType) {
                    val annotations = parameterAnnotations[localParameter.size] ?: emptyList()
                    localParameter.add(LazyAnnotatedClass.of(name, dimensions, annotations = annotations, classFinder = classFinder))
                }
                if (visitReturnType) {
                    localResult = LazyClass.of(name, dimensions, classFinder = classFinder)
                }
                dimensions = 0
                super.visitClassType(name)
            }

            override fun visitBaseType(descriptor: Char) {
                // println("  visitBaseType $descriptor")
                if (visitParameterType) {
                    val annotations = parameterAnnotations[localParameter.size] ?: emptyList()
                    localParameter += LazyAnnotatedClass.of(descriptor.toString(), dimensions, isPrimitive = true, annotations) { Reflection.getPrimitiveType(descriptor) }
                }
                if (visitReturnType) {
                    localResult = LazyClass.of(descriptor.toString(), dimensions, isPrimitive = true) { Reflection.getPrimitiveType(descriptor) }
                }
                dimensions = 0
                super.visitBaseType(descriptor)
            }

            override fun visitArrayType(): SignatureVisitor {
                // println("  visitArrayType")
                dimensions++
                return super.visitArrayType()
            }
        })
    }

    override fun toString(): String {
        return "AsmClassMethod(descriptor='$descriptor', access=$access) ${super.toString()}"
    }

    override fun writeTo(writer: BinaryWriter) {
        writer.writeInt(1) // 1: ASM MODE
        // 函数信息
        writeTo(writer, name, owner, descriptor, access, parameterAnnotations, annotations, parameter)
        // 返回值
        writer.writeObj(result)
    }

    companion object {

        fun writeTo(
            writer: BinaryWriter,
            name: String,
            owner: LazyClass,
            descriptor: String,
            access: Int,
            parameterAnnotations: Map<Int, List<ClassAnnotation>>,
            annotations: List<ClassAnnotation>,
            parameter: List<LazyAnnotatedClass>,
        ) {
            writer.writeNullableString(name)
            writer.writeObj(owner)
            writer.writeNullableString(descriptor)
            writer.writeInt(access)
            // 参数注解
            writer.writeInt(parameterAnnotations.size)
            parameterAnnotations.forEach { (k, v) ->
                writer.writeInt(k)
                writer.writeList(v)
            }
            // 注解
            writer.writeList(annotations)
            // 参数
            writer.writeList(parameter)
        }

        fun readFrom(reader: BinaryReader, classFinder: ClassAnalyser.ClassFinder?): Method {
            val name = reader.readNullableString()!!
            val owner = LazyClass.of(reader, classFinder)
            val descriptor = reader.readNullableString()!!
            val access = reader.readInt()
            // 参数注解
            val parameterAnnotations = HashMap<Int, List<ClassAnnotation>>()
            val size = reader.readInt()
            for (i in 0 until size) {
                val index = reader.readInt()
                val annotations = reader.readAnnotationList(classFinder)
                parameterAnnotations[index] = annotations
            }
            // 注解
            val annotations = reader.readAnnotationList(classFinder)
            // 参数
            val parameter = reader.readAnnotationClassList(classFinder)
            return Method(name, owner, descriptor, access, parameterAnnotations, annotations, parameter)
        }

        class Method(
            val name: String,
            val owner: LazyClass,
            val descriptor: String,
            val access: Int,
            val parameterAnnotations: Map<Int, List<ClassAnnotation>>,
            val annotations: List<ClassAnnotation>,
            val parameter: MutableList<LazyAnnotatedClass>,
        )
    }
}