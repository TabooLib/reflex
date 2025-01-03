package org.tabooproject.reflex.serializer

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.function.Consumer

/**
 * 二进制写入器
 * 基于 ByteArrayOutputStream 实现自动扩容
 */
class BinaryWriter {

    private val byteArrayOutputStream = ByteArrayOutputStream()
    private val output = DataOutputStream(byteArrayOutputStream)

    /**
     * 写入可为空的字符串
     */
    fun writeNullableString(str: String?) {
        if (str == null) {
            output.writeInt(-1)
        } else {
            val bytes = str.toByteArray()
            output.writeInt(bytes.size)
            output.write(bytes)
        }
    }

    fun writeInt(value: Int) {
        output.writeInt(value)
    }

    fun writeBoolean(value: Boolean) {
        output.writeBoolean(value)
    }

    fun writeList(list: List<BinarySerializable>) {
        output.writeInt(list.size)
        list.forEach { it.writeTo(this) }
    }

    fun <T> writeList(list: List<T>, writer: Consumer<T>) {
        output.writeInt(list.size)
        list.forEach { writer.accept(it) }
    }

    fun writeObj(obj: BinarySerializable) {
        obj.writeTo(this)
    }

    fun writeNullableObj(obj: BinarySerializable?) {
        if (obj == null) {
            output.writeInt(-1)
        } else {
            output.writeInt(0)
            obj.writeTo(this)
        }
    }

    /**
     * 写入注解属性
     */
    fun writeAnnotationProperties(properties: Map<String, Any>) {
        output.writeInt(properties.size)
        properties.forEach { (key, value) ->
            writeNullableString(key)
            BinarySerializer.writeTo(output, value)
        }
    }

    /**
     * 获取写入的字节数组
     */
    fun toByteArray(): ByteArray {
        return byteArrayOutputStream.toByteArray()
    }
}