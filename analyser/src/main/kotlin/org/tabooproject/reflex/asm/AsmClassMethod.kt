package org.tabooproject.reflex.asm

import org.tabooproject.reflex.reflection.InstantClass
import org.tabooproject.reflex.JavaClassMethod
import org.tabooproject.reflex.LazyClass
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.signature.SignatureWriter
import org.tabooproject.reflex.Reflection
import java.lang.reflect.Modifier

/**
 * @author 坏黑
 * @since 2022/1/21 6:34 PM
 */
class AsmClassMethod(name: String, owner: Class<*>, val descriptor: String, val access: Int) : JavaClassMethod(name, owner) {

    lateinit var localResult: LazyClass
    val localParameter = ArrayList<LazyClass>()

    override val result: LazyClass
        get() = localResult

    override val parameter: List<LazyClass>
        get() = localParameter

    override val isStatic: Boolean
        get() = Modifier.isStatic(access)

    init {
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
                visit(LazyClass(name))
                super.visitClassType(name)
            }

            override fun visitBaseType(descriptor: Char) {
                visit(InstantClass(Reflection.getPrimitiveType(descriptor)))
                super.visitBaseType(descriptor)
            }

            fun visit(lazyClass: LazyClass) {
                if (visitParameterType) {
                    localParameter.add(lazyClass)
                }
                if (visitReturnType) {
                    localResult = lazyClass
                }
            }
        })
    }

    override fun toString(): String {
        return "AsmClassMethod(descriptor='$descriptor', access=$access) ${super.toString()}"
    }
}