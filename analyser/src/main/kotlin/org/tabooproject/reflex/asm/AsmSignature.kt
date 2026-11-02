package org.tabooproject.reflex.asm

import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import org.objectweb.asm.signature.SignatureWriter
import org.tabooproject.reflex.ClassAnalyser
import org.tabooproject.reflex.Internal
import org.tabooproject.reflex.LazyClass
import org.tabooproject.reflex.Reflection
import org.tabooproject.reflex.WeakCache
import java.util.concurrent.ConcurrentHashMap

@Internal
object AsmSignature {

    /** 保留旧公开字段的二进制兼容性，默认 finder 继续使用该缓存。 */
    val cacheMap = ConcurrentHashMap<String, List<LazyClass>>()

    /** 显式 finder 必须按对象身份隔离。 */
    private val finderCache = WeakCache<SignatureCacheKey, List<LazyClass>>()

    fun signatureToClass(signature: String, classFinder: ClassAnalyser.ClassFinder? = null): List<LazyClass> {
        return if (classFinder == null) {
            cacheMap.computeIfAbsent(signature) { parse(signature, null) }
        } else {
            finderCache.computeIfAbsent(SignatureCacheKey(signature, classFinder)) {
                parse(signature, classFinder)
            }
        }
    }

    fun clearCache() {
        cacheMap.clear()
        finderCache.clear()
    }

    private fun parse(signature: String, classFinder: ClassAnalyser.ClassFinder?): List<LazyClass> {
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
            list.removeAt(list.size - 1)
        }
        return list
    }

    private class SignatureCacheKey(
        private val signature: String,
        private val finder: ClassAnalyser.ClassFinder,
    ) {

        override fun equals(other: Any?): Boolean {
            return other is SignatureCacheKey && signature == other.signature && finder === other.finder
        }

        override fun hashCode(): Int {
            return 31 * signature.hashCode() + System.identityHashCode(finder)
        }
    }
}
