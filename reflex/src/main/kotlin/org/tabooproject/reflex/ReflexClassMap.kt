package org.tabooproject.reflex

import org.tabooproject.reflex.serializer.BinaryReader
import org.tabooproject.reflex.serializer.BinaryWriter

/**
 * 基于 ByteBuffer 的二进制序列化工具
 * 以避免每次启动时重新加载所有类导致的性能问题
 *
 * 目前仅支持对 ASM 模式进行序列化
 */
object ReflexClassMap {

    const val VERSION = 0

    fun serializeToBytes(map: Map<String, ReflexClass>): ByteArray {
        val writer = BinaryWriter()
        writer.writeInt(VERSION)
        writer.writeInt(map.size)
        map.forEach { (k, v) ->
            try {
                writer.writeNullableString(k)
                writer.writeObj(v)
            } catch (ex: Throwable) {
                println("Failed to serialize class $k")
                throw ex
            }
        }
        return writer.toByteArray()
    }

    fun deserializeFromBytes(bytes: ByteArray, classFinder: ClassAnalyser.ClassFinder?): Map<String, ReflexClass> {
        val map = mutableMapOf<String, ReflexClass>()
        val reader = BinaryReader(bytes)
        // 读取版本号
        val version = reader.readInt()
        if (version != VERSION) {
            throw IllegalArgumentException("Unsupported version: $version")
        }
        repeat(reader.readInt()) {
            val name = reader.readNullableString()!!
            try {
                map[name] = ReflexClass.of(reader, classFinder)
            } catch (ex: Throwable) {
                println("Failed to deserialize class $name")
                throw ex
            }
        }
        return map
    }
}