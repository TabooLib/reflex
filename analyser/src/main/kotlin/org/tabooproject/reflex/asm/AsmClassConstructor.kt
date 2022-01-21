package org.tabooproject.reflex.asm

import org.tabooproject.reflex.reflection.InstantClass
import org.tabooproject.reflex.JavaClassConstructor
import org.tabooproject.reflex.LazyClass
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureWriter
import org.tabooproject.reflex.Reflection
import java.lang.reflect.Modifier

/**
 * @author 坏黑
 * @since 2022/1/21 6:34 PM
 */
class AsmClassConstructor(name: String, owner: Class<*>, val descriptor: String, val access: Int) : JavaClassConstructor(name, owner) {

    val localParameter = ArrayList<LazyClass>()

    override val parameter: List<LazyClass>
        get() = localParameter

    override val isStatic: Boolean
        get() = Modifier.isStatic(access)

    init {
        SignatureReader(descriptor).accept(object : SignatureWriter() {

            override fun visitClassType(name: String) {
                localParameter.add(LazyClass(name))
                super.visitClassType(name)
            }

            override fun visitBaseType(descriptor: Char) {
                localParameter.add(InstantClass(Reflection.getPrimitiveType(descriptor)))
                super.visitBaseType(descriptor)
            }
        })
        if (localParameter.lastOrNull()?.name == "void") {
            localParameter.removeLast()
        }
    }

    override fun toString(): String {
        return "AsmClassConstructor(descriptor='$descriptor', access=$access) ${super.toString()}"
    }
}