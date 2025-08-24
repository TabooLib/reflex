package org.tabooproject.reflex

import org.tabooproject.reflex.serializer.BinaryReader
import java.io.File

class BinaryTest {

    // @Test
    fun test() {
        val reader = BinaryReader(File("platform.cache").readBytes())
        println("env: ${reader.readList { reader.readString() }}")
    }
}