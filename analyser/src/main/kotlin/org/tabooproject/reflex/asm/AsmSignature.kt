package org.tabooproject.reflex.asm

import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.signature.SignatureWriter
import org.tabooproject.reflex.ClassAnalyser
import org.tabooproject.reflex.Internal
import org.tabooproject.reflex.LazyClass
import org.tabooproject.reflex.Reflection

@Internal
object AsmSignature {

    fun signatureToClass(signature: String, classFinder: ClassAnalyser.ClassFinder): List<LazyClass> {
        val list = ArrayList<LazyClass>()
        var visitArrayType = false
        SignatureReader(signature).accept(object : SignatureWriter() {

            override fun visitClassType(name: String) {
                super.visitClassType(name)
                list.add(LazyClass.of(name, visitArrayType, classFinder))
            }

            override fun visitBaseType(descriptor: Char) {
                super.visitBaseType(descriptor)
                list.add(LazyClass.of(Reflection.getPrimitiveType(descriptor), visitArrayType))
            }

            override fun visitArrayType(): SignatureVisitor {
                super.visitArrayType()
                visitArrayType = true
                return this
            }
        })
        if (list.lastOrNull()?.name == "void") {
            // Caused by: java.lang.NoSuchMethodError: 'java.lang.Object java.util.ArrayList.removeLast()'
            // 你在逗我玩吗兄弟？
            list.removeAt(list.size - 1)
        }
        return list
    }
}