package org.tabooproject.reflex.asm

import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.signature.SignatureWriter
import org.tabooproject.reflex.ClassAnalyser
import org.tabooproject.reflex.Internal
import org.tabooproject.reflex.LazyClass
import org.tabooproject.reflex.Reflection
import java.util.concurrent.ConcurrentHashMap

@Internal
object AsmSignature {

    val cacheMap = ConcurrentHashMap<String, List<LazyClass>>()

    fun signatureToClass(signature: String, classFinder: ClassAnalyser.ClassFinder? = null): List<LazyClass> {
        return cacheMap.getOrPut(signature) {
            val list = ArrayList<LazyClass>()
            var dimensions = 0
            SignatureReader(signature).accept(object : SignatureWriter() {

                override fun visitClassType(name: String) {
                    super.visitClassType(name)
                    list.add(LazyClass.of(name, dimensions, isPrimitive = false, classFinder))
                }

                override fun visitBaseType(descriptor: Char) {
                    super.visitBaseType(descriptor)
                    list.add(LazyClass.of(descriptor.toString(), dimensions, isPrimitive = true) { Reflection.getPrimitiveType(descriptor) })
                }

                override fun visitArrayType(): SignatureVisitor {
                    super.visitArrayType()
                    dimensions++
                    return this
                }
            })
            if (list.lastOrNull()?.name == "V") {
                // Caused by: java.lang.NoSuchMethodError: 'java.lang.Object java.util.ArrayList.removeLast()'
                // 你在逗我玩吗兄弟？
                list.removeAt(list.size - 1)
            }
            return list
        }
    }
}