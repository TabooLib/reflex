package org.tabooproject.reflex

import org.junit.jupiter.api.Test
import org.tabooproject.reflex.serializer.BinaryReader
import org.tabooproject.reflex.serializer.BinaryWriter

class LazyClassSerialize {

    @Test
    fun testClass() {
        val lazyClass = ReflexClass.of(LazyClassSerialize::class.java)
        val buffer = BinaryWriter()
        lazyClass.writeTo(buffer)
        val byteArray = buffer.toByteArray()
        println("序列化后占 ${byteArray.size} 字节")

        val wrap = BinaryReader.from(byteArray)
    }
}