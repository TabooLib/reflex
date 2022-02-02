package org.tabooproject.reflex.asm

import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.signature.SignatureWriter
import org.tabooproject.reflex.*
import org.tabooproject.reflex.reflection.InstantAnnotatedClass
import org.tabooproject.reflex.reflection.InstantClass
import java.lang.reflect.Modifier

/**
 * @author 坏黑
 * @since 2022/1/21 6:34 PM
 */
@Internal
class AsmClassMethod(
    name: String,
    owner: Class<*>,
    val descriptor: String,
    val access: Int,
    val parameterAnnotations: Map<Int, ArrayList<AsmAnnotation>>,
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
                    localParameter.add(LazyAnnotatedClass(name, parameterAnnotations[localParameter.size] ?: emptyList()))
                }
                if (visitReturnType) {
                    localResult = LazyClass(name)
                }
                super.visitClassType(name)
            }

            override fun visitBaseType(descriptor: Char) {
                if (visitParameterType) {
                    localParameter += InstantAnnotatedClass(Reflection.getPrimitiveType(descriptor), parameterAnnotations[localParameter.size] ?: emptyList())
                }
                if (visitReturnType) {
                    localResult = InstantClass(Reflection.getPrimitiveType(descriptor))
                }
                super.visitBaseType(descriptor)
            }
        })
    }

    override fun toString(): String {
        return "AsmClassMethod(descriptor='$descriptor', access=$access) ${super.toString()}"
    }
}