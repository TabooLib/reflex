package org.tabooproject.reflex

import org.tabooproject.reflex.asm.AsmClassField
import org.tabooproject.reflex.serializer.BinaryReader
import org.tabooproject.reflex.serializer.BinarySerializable

/**
 * @author 坏黑
 * @since 2022/1/21 6:41 PM
 */
abstract class ClassField(name: String, owner: LazyClass) : ClassMember(name, owner), BinarySerializable {

    abstract val type: LazyClass

    abstract val isTransient: Boolean

    abstract fun get(src: Any? = null): Any?

    abstract fun set(src: Any? = null, value: Any?)

    fun setStatic(value: Any?) {
        set(StaticSrc, value)
    }

    val fieldType: Class<*>
        get() = type.instance ?: Unknown::class.java

    override fun toString(): String {
        return "ClassField(type=$type) ${super.toString()}"
    }

    companion object {

        fun of(reader: BinaryReader, classFinder: ClassAnalyser.ClassFinder?): ClassField {
            val type = reader.readInt()
            if (type == 1) {
                val name = reader.readNullableString()!!
                val owner = LazyClass.of(reader, classFinder)
                val descriptor = reader.readNullableString()!!
                val access = reader.readInt()
                val annotations = reader.readAnnotationList(classFinder)
                val type = LazyClass.of(reader, classFinder)
                return AsmClassField(
                    name,
                    owner,
                    descriptor,
                    access,
                    classFinder ?: ClassAnalyser.ClassFinder.default,
                    annotations,
                    type
                )
            } else {
                error("Unknown type: $type")
            }
        }
    }
}