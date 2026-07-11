package org.tabooproject.reflex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.tabooproject.reflex.serializer.BinaryReader
import org.tabooproject.reflex.serializer.BinarySerializer
import org.tabooproject.reflex.serializer.BinaryWriter
import java.io.File

class BinaryTest {

    @Test
    fun `serialize unicode string`() {
        val writer = BinaryWriter()
        BinarySerializer.writeTo(writer.output, "是否关闭虚拟方块")

        val value = BinarySerializer.readFrom(BinaryReader.from(writer.toByteArray()), null)
        assertEquals("是否关闭虚拟方块", value)
    }

    // @Test
    fun test() {
        val reader = BinaryReader(File("platform.cache").readBytes())
        println("env: ${reader.readList { reader.readString() }}")
    }
}
