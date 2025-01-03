package org.tabooproject.reflex.serializer

import org.tabooproject.reflex.LazyClass
import org.tabooproject.reflex.LazyEnum
import java.io.DataOutputStream

object BinarySerializer {

    fun writeTo(output: DataOutputStream, value: Any?) {
        when (value) {
            is Byte -> {
                output.writeInt(0)
                output.writeByte(value.toInt())
            }

            is Short -> {
                output.writeInt(1)
                output.writeShort(value.toInt())
            }

            is Int -> {
                output.writeInt(2)
                output.writeInt(value)
            }

            is Long -> {
                output.writeInt(3)
                output.writeLong(value)
            }

            is Float -> {
                output.writeInt(4)
                output.writeFloat(value)
            }

            is Double -> {
                output.writeInt(5)
                output.writeDouble(value)
            }

            is Boolean -> {
                output.writeInt(6)
                output.writeBoolean(value)
            }

            is Char -> {
                output.writeInt(7)
                output.writeChar(value.toInt())
            }

            is String -> {
                output.writeInt(8)
                output.writeUTF(value)
            }

            is List<*> -> {
                output.writeInt(9)
                output.writeInt(value.size)
                value.forEach { writeTo(output, it) }
            }

            is Class<*> -> {
                output.writeInt(10)
                output.writeUTF(value.name)
            }

            is LazyClass -> {
                output.writeInt(11)
                output.write(BinaryWriter().also { value.writeTo(it) }.toByteArray())
            }

            is Enum<*> -> {
                output.writeInt(12)
                output.writeInt(value.ordinal)
            }

            is LazyEnum -> {
                output.writeInt(13)
                output.writeUTF(value.source.name)
                output.writeUTF(value.name)
            }

            else -> {
                output.writeInt(-1)
            }
        }
    }

    fun readFrom(reader: BinaryReader): Any? {
        TODO()
    }
}