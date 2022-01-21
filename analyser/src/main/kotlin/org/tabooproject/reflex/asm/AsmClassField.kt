package org.tabooproject.reflex.asm

import org.tabooproject.reflex.JavaClassField
import org.tabooproject.reflex.LazyClass
import org.tabooproject.reflex.reflection.InstantClass
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureWriter
import org.tabooproject.reflex.Reflection
import java.lang.reflect.Modifier

/**
 * @author 坏黑
 * @since 2022/1/21 6:33 PM
 */
class AsmClassField(name: String, owner: Class<*>, val descriptor: String, val access: Int) : JavaClassField(name, owner) {

    lateinit var localType: LazyClass

    override val type: LazyClass
        get() = localType

    override val isStatic: Boolean
        get() = Modifier.isStatic(access)

    init {
        SignatureReader(descriptor).accept(object : SignatureWriter() {

            override fun visitClassType(name: String) {
                localType = LazyClass(name)
                super.visitClassType(name)
            }

            override fun visitBaseType(descriptor: Char) {
                localType = InstantClass(Reflection.getPrimitiveType(descriptor))
                super.visitBaseType(descriptor)
            }
        })
    }

    override fun toString(): String {
        return "AsmClassField(descriptor='$descriptor', access=$access) ${super.toString()}"
    }
}