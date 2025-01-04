package org.tabooproject.reflex.serializer

import org.tabooproject.reflex.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.function.Supplier

/**
 * 二进制读取器
 * 基于 ByteBuffer 实现高效读取
 */
class BinaryReader(bytes: ByteArray) {

    val buffer: ByteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

    fun readNullableString(): String? {
        val size = buffer.int
        return if (size == -1) null else {
            val bytes = ByteArray(size)
            buffer.get(bytes, 0, size)
            String(bytes, StandardCharsets.UTF_8)
        }
    }

    fun readString(): String {
        val size = buffer.int
        val bytes = ByteArray(size)
        buffer.get(bytes, 0, size)
        return String(bytes, StandardCharsets.UTF_8)
    }

    fun readByte(): Byte = buffer.get()

    fun readShort(): Short = buffer.short

    fun readInt(): Int = buffer.int

    fun readLong(): Long = buffer.long

    fun readFloat(): Float = buffer.float

    fun readDouble(): Double = buffer.double

    fun readChar(): Char = buffer.char

    fun readBoolean(): Boolean = buffer.get() != 0.toByte()

    inline fun <reified T> readList(factory: () -> T): MutableList<T> {
        val size = buffer.int
        return (0 until size).mapTo(ArrayList()) { factory() }
    }

    inline fun <reified T> readArray(factory: () -> T): Array<T> {
        val size = buffer.int
        return Array(size) { factory() }
    }

    fun readClassList(classFinder: ClassAnalyser.ClassFinder?): MutableList<LazyClass> {
        val size = buffer.int
        return (0 until size).mapTo(ArrayList()) { LazyClass.of(this, classFinder) }
    }

    fun readAnnotationClassList(classFinder: ClassAnalyser.ClassFinder?): MutableList<LazyAnnotatedClass> {
        val size = buffer.int
        return (0 until size).mapTo(ArrayList()) { LazyClass.of(this, classFinder) as LazyAnnotatedClass }
    }

    fun readAnnotationList(classFinder: ClassAnalyser.ClassFinder?): MutableList<ClassAnnotation> {
        val size = buffer.int
        return (0 until size).mapTo(ArrayList()) { ClassAnnotation.of(this, classFinder) }
    }

    fun readFieldList(classFinder: ClassAnalyser.ClassFinder?): MutableList<ClassField> {
        val size = buffer.int
        return (0 until size).mapTo(ArrayList()) { ClassField.of(this, classFinder) }
    }

    fun readMethodList(classFinder: ClassAnalyser.ClassFinder?): MutableList<ClassMethod> {
        val size = buffer.int
        return (0 until size).mapTo(ArrayList()) { ClassMethod.of(this, classFinder) }
    }

    fun readConstructorList(classFinder: ClassAnalyser.ClassFinder?): MutableList<ClassConstructor> {
        val size = buffer.int
        return (0 until size).mapTo(ArrayList()) { ClassConstructor.of(this, classFinder) }
    }

    fun <T> readNullableObj(factor: Supplier<T>): T? {
        val size = buffer.int
        return if (size == -1) null else factor.get()
    }

    fun readNullableClass(classFinder: ClassAnalyser.ClassFinder?): LazyClass? {
        val size = buffer.int
        return if (size == -1) null else LazyClass.of(this, classFinder)
    }

    fun readAnnotationProperties(classFinder: ClassAnalyser.ClassFinder?): Map<String, Any> {
        val size = buffer.int
        return (0 until size).associate {
            val key = readNullableString()!!
            val value = BinarySerializer.readFrom(this, classFinder)
            key to value
        }
    }

    companion object {

        fun from(bytes: ByteArray): BinaryReader = BinaryReader(bytes)
    }
}