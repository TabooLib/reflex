package org.tabooproject.reflex

import org.junit.jupiter.api.Test
import org.tabooproject.reflex.serializer.BinaryReader
import org.tabooproject.reflex.serializer.BinaryWriter

class LazyClassSerialize {

    @Test
    fun testClass() {
        val lazyClass = LazyClass.of(LazyClassSerialize::class.java)
        val buffer = BinaryWriter()
        lazyClass.writeTo(buffer)
        val byteArray = buffer.toByteArray()

        val wrap = BinaryReader.from(byteArray)
        assert(wrap.readInt() == 1)
        assert(wrap.readNullableString() == "org.tabooproject.reflex.LazyClassSerialize")
        assert(wrap.readNullableString() == "LazyClassSerialize")
        assert(wrap.readInt() == 0)         // dimensions
        assert(wrap.readBoolean() == true)  // isInstant
        assert(wrap.readBoolean() == false) // isPrimitive
    }
}