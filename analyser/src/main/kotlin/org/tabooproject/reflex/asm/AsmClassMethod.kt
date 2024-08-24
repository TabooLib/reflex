package org.tabooproject.reflex.asm

import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.signature.SignatureWriter
import org.tabooproject.reflex.*
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
    val parameterAnnotations: Map<Int, ArrayList<AsmAnnotation>>,
    val classFinder: ClassAnalyser.ClassFinder,
    override val annotations: List<ClassAnnotation>,
) : JavaClassMethod(name, owner) {

    lateinit var localResult: LazyClass

    val localParameter = ArrayList<LazyAnnotatedClass>()

    override val result: LazyClass
        get() = localResult

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
        SignatureReader(descriptor).accept(object : SignatureWriter() {

            override fun visitParameterType(): SignatureVisitor {
                visitParameterType = true
                return super.visitParameterType()
            }

            override fun visitReturnType(): SignatureVisitor {
                visitParameterType = false
                visitReturnType = true
                return super.visitReturnType()
            }

            override fun visitClassType(name: String) {
                if (visitParameterType) {
                    localParameter.add(LazyAnnotatedClass.of(name, parameterAnnotations[localParameter.size] ?: emptyList(), classFinder))
                }
                if (visitReturnType) {
                    localResult = LazyClass.of(name, classFinder)
                }
                super.visitClassType(name)
            }

            override fun visitBaseType(descriptor: Char) {
                if (visitParameterType) {
                    localParameter += LazyAnnotatedClass.of(Reflection.getPrimitiveType(descriptor), parameterAnnotations[localParameter.size] ?: emptyList())
                }
                if (visitReturnType) {
                    localResult = LazyClass.of(Reflection.getPrimitiveType(descriptor))
                }
                super.visitBaseType(descriptor)
            }
        })
    }

    override fun toString(): String {
        return "AsmClassMethod(descriptor='$descriptor', access=$access) ${super.toString()}"
    }
}