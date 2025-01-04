package org.tabooproject.reflex

import org.junit.jupiter.api.Test
import org.tabooproject.reflex.serializer.BinaryReader
import org.tabooproject.reflex.serializer.BinarySerializer
import org.tabooproject.reflex.serializer.BinaryWriter
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class ReflexClassSerialization {

    @OptIn(ExperimentalTime::class)
    @Test
    fun testAnalyze() {
        // 解析
        println("解析 ${measureTime { ReflexClass.of(ReflexClassSerialization::class.java, saving = false) }}")
//        println("100000 次解析 ${measureTime { repeat(100000) { ReflexClass.of(ReflexClassSerialization::class.java, saving = false) } }}")

        // JFR 监控
//        val time: Duration
//        Recording(Configuration.getConfiguration("profile")).use { recording ->
//            recording.start()
//
//            time = measureTime { repeat(100000) { ReflexClass.of(LazyClassSerialize::class.java, saving = false) } }
//
//            // 停止记录并保存
//            recording.stop()
//            recording.dump(Path.of("analyze-profile.jfr"))
//        }
//        println("十万次解析完成 $time")
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testClass() {
        // 写入
        val lazyClass = ReflexClass.of(ReflexClassSerialization::class.java, saving = false)
        val writer = BinaryWriter()
        lazyClass.writeTo(writer)
        println("写入 ${measureTime { lazyClass.writeTo(BinaryWriter()) }}")
//        println("10000 次写入 ${measureTime { repeat(10000) { lazyClass.writeTo(BinaryWriter()) } }}")

        // 读取
        val byteArray = writer.toByteArray()
        println("读取 ${measureTime { ReflexClass.of(BinaryReader.from(byteArray)) }}")
//        println("10000 次读取 ${measureTime { repeat(10000) { ReflexClass.of(BinaryReader.from(byteArray)) } }}")
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testJfr() {
        // 写入
        val lazyClass = ReflexClass.of(ReflexClassSerialization::class.java, saving = false)
        val writer = BinaryWriter()
        lazyClass.writeTo(writer)
        val byteArray = writer.toByteArray()

        // JFR 监控
//        val time: Duration
//        Recording(Configuration.getConfiguration("profile")).use { recording ->
//            recording.start()
//
//            time = measureTime { repeat(1000000) { ReflexClass.of(BinaryReader.from(byteArray)) } }
//
//            // 停止记录并保存
//            recording.stop()
//            recording.dump(Path.of("serialization-profile.jfr"))
//        }
//        println("一百万次读取完成 $time")
    }

    @Test
    fun testClass1() {
        val lazyClass = ReflexClass.of(ReflexClassSerialization::class.java, saving = false)
        val first = lazyClass.structure.annotations.first()

        val writer = BinaryWriter()
        val properties = first.properties()
        writer.writeInt(properties.size)
        println("propertyMap size: ${properties.size}")
        properties.forEach { (k, v) ->
            writer.writeNullableString(k)
            println("propertyMap write key: $k")

            BinarySerializer.writeTo(writer.output, v)
            println("propertyMap write value: $v (${BinarySerializer.getSerializationType(v)})")
            if (v is List<*>) {
                println("  list size: ${v.size}")
                if (v[0] is String) {
                    println("  list first element len: ${(v[0] as String).length}")
                }
            }
        }

        println("-".repeat(30))

        val reader = BinaryReader.from(writer.toByteArray())
        val size = reader.readInt()
        println("propertyMap size: $size")
        repeat(size) {
            println("propertyMap key: ${reader.readNullableString()}")
            val value = BinarySerializer.readFrom(reader, null)
            println("propertyMap value: ${value}")
            if (value is List<*>) {
                println("  list size: ${value.size}")
                if (value[0] is String) {
                    println("  list first element len: ${(value[0] as String).length}")
                }
            }
        }
    }

    @Test
    fun testMap() {
        val map = mapOf("a" to listOf("a".repeat(60)), "b" to listOf("b".repeat(60)))
        val writer = BinaryWriter()
        writer.writeAnnotationProperties(map)
        val reader = BinaryReader.from(writer.toByteArray())
        val readMap = reader.readAnnotationProperties(null)
        println(readMap)
    }
}