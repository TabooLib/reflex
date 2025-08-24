package org.tabooproject.reflex

import org.tabooproject.reflex.asm.AsmClassMethod
import org.tabooproject.reflex.serializer.BinaryReader
import org.tabooproject.reflex.serializer.BinarySerializable

/**
 * @author 坏黑
 * @since 2022/1/21 6:41 PM
 */
abstract class ClassMethod(name: String, owner: LazyClass) : ClassMember(name, owner), BinarySerializable {

    abstract val result: LazyClass

    abstract val parameter: List<LazyAnnotatedClass>

    abstract val isNative: Boolean

    abstract val isAbstract: Boolean

    abstract val isVolatile: Boolean

    abstract val isSynchronized: Boolean

    abstract fun invoke(src: Any, vararg values: Any?): Any?

    abstract fun invokeStatic(vararg values: Any?): Any?

    val returnType by lazy(LazyThreadSafetyMode.NONE) {
        try {
            result.instance ?: Unknown::class.java
        } catch (ex: Throwable) {
            println("Field to get return type of $this")
            throw ex
        }
    }

    val parameterTypes by lazy(LazyThreadSafetyMode.NONE) {
        parameter.map { p -> p.instance ?: Unknown::class.java }.toTypedArray()
    }

    override fun toString(): String {
        return "ClassMethod(result=$result)"
    }

    companion object {

        fun of(reader: BinaryReader, classFinder: ClassAnalyser.ClassFinder?): ClassMethod {
            val type = reader.readInt()
            if (type == 1) {
                val method = AsmClassMethod.readFrom(reader, classFinder)
                // 返回值
                val result = LazyClass.of(reader, classFinder)
                return AsmClassMethod(
                    method.name,
                    method.owner,
                    method.descriptor,
                    method.access,
                    method.parameterAnnotations,
                    classFinder ?: ClassAnalyser.ClassFinder.default,
                    method.annotations,
                    result,
                    method.parameter
                )
            } else {
                error("Unknown type: $type")
            }
        }
    }
}