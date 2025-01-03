package org.tabooproject.reflex.serializer

interface BinarySerializable {

    /**
     * 写入到 ByteBuffer
     */
    fun writeTo(writer: BinaryWriter)
}