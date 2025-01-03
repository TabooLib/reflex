package org.tabooproject.reflex.serializer

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.function.Supplier

/**
 * 二进制读取器
 * 基于 ByteBuffer 实现高效读取
 */
class BinaryReader(bytes: ByteArray) {

    private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

    fun readNullableString(): String? {
        val size = buffer.int
        return if (size == -1) null else {
            val bytes = ByteArray(size)
            buffer.get(bytes)
            String(bytes)
        }
    }

    fun readInt(): Int = buffer.int

    fun readBoolean(): Boolean = buffer.get() != 0.toByte()

    fun <T : BinarySerializable> readList(factory: Supplier<T>): List<T> {
        val size = buffer.int
        return (0 until size).map { factory.get() }
    }

    companion object {

        fun from(bytes: ByteArray): BinaryReader = BinaryReader(bytes)
    }
}