package org.tabooproject.reflex.asm

import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureWriter
import org.tabooproject.reflex.LazyClass
import org.tabooproject.reflex.Reflection
import org.tabooproject.reflex.reflection.InstantClass

object AsmSignature {

    fun signatureToClass(signature: String): List<LazyClass> {
        val list = ArrayList<LazyClass>()
        SignatureReader(signature).accept(object : SignatureWriter() {

            override fun visitClassType(name: String) {
                list.add(LazyClass(name))
                super.visitClassType(name)
            }

            override fun visitBaseType(descriptor: Char) {
                list.add(InstantClass(Reflection.getPrimitiveType(descriptor)))
                super.visitBaseType(descriptor)
            }
        })
        if (list.lastOrNull()?.name == "void") {
            list.removeLast()
        }
        return list
    }
}