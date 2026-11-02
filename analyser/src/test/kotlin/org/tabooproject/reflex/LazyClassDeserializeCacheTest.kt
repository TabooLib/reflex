package org.tabooproject.reflex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tabooproject.reflex.asm.AsmSignature
import org.tabooproject.reflex.serializer.BinaryReader
import org.tabooproject.reflex.serializer.BinaryWriter

/**
 * 验证 LazyClass 反序列化缓存：同一类名在多次反序列化中只创建一个 LazyClass 实例（type 1）
 */
class LazyClassDeserializeCacheTest {

    @Deprecated("测试注解顺序")
    private class CollectionSample(private val number: Int) : Runnable {

        private val text = number.toString()

        override fun run() = Unit

        fun value(): String = text
    }

    @Test
    fun testSerializedIdentityFieldsAreNotMerged() {
        LazyClass.clearDeserializeCaches()
        val first = LazyClass.of(BinaryReader.from(makeBytes("sample.Outer\$Inner", "Inner", true)), null)
        val second = LazyClass.of(BinaryReader.from(makeBytes("sample.Outer\$Inner", "Outer\$Inner", false)), null)

        assertTrue(first !== second, "simpleName 或 isInstant 不同的对象不得复用")
        assertTrue(first.simpleName == "Inner")
        assertTrue(first.isInstant)
        assertTrue(second.simpleName == "Outer\$Inner")
        assertTrue(!second.isInstant)
    }

    @Test
    fun testDifferentFindersAreIsolated() {
        LazyClass.clearDeserializeCaches()
        val bytes = makeBytes("sample.Same", "Same", false)
        val stringFinder = ClassAnalyser.ClassFinder { String::class.java }
        val integerFinder = ClassAnalyser.ClassFinder { Integer::class.java }

        val first = LazyClass.of(BinaryReader.from(bytes), stringFinder)
        val second = LazyClass.of(BinaryReader.from(bytes), integerFinder)

        assertTrue(first !== second, "不同 finder 不得共享反序列化对象")
        assertTrue(first.instance === String::class.java)
        assertTrue(second.instance === Integer::class.java)
    }

    @Test
    fun testSignatureFindersAreIsolated() {
        AsmSignature.clearCache()
        val stringFinder = ClassAnalyser.ClassFinder { String::class.java }
        val integerFinder = ClassAnalyser.ClassFinder { Integer::class.java }

        val first = AsmSignature.signatureToClass("Lsample/Same;", stringFinder).single()
        val second = AsmSignature.signatureToClass("Lsample/Same;", integerFinder).single()

        assertNotSame(first, second, "不同 finder 不得共享签名解析结果")
        assertSame(String::class.java, first.instance)
        assertSame(Integer::class.java, second.instance)
    }

    @Test
    @Suppress("DEPRECATION")
    fun testCollectionOrderIsPreserved() {
        val source = CollectionSample::class.java
        val structure = ClassAnalyser.analyseByReflection(source)

        assertEquals(source.interfaces.map { it.name }, structure.interfaces.map { it.name })
        assertEquals(source.declaredAnnotations.map { it.annotationClass.java.name }, structure.annotations.map { it.source.name })
        assertEquals(source.declaredFields.map { it.name }, structure.fields.map { it.name })
        assertEquals(source.declaredMethods.map { it.name }, structure.methods.map { it.name })
        assertEquals(source.declaredConstructors.size, structure.constructors.size)
    }

    /**
     * 序列化一个 LazyClass（type 1），多次反序列化后应返回同一个实例
     */
    @Test
    fun testType1DeserializeCacheHit() {
        // 构造一个 type 1 的 LazyClass 字节流
        val original = LazyClass.of("java.lang.String", dimensions = 0, isPrimitive = false) { String::class.java }
        val writer = BinaryWriter()
        original.writeTo(writer)
        val bytes = writer.toByteArray()

        // 多次反序列化
        val first = LazyClass.of(BinaryReader.from(bytes), null)
        val second = LazyClass.of(BinaryReader.from(bytes), null)
        val third = LazyClass.of(BinaryReader.from(bytes), null)

        // 验证是同一个实例
        assertTrue(first === second, "第二次反序列化应命中缓存返回同一实例")
        assertTrue(first === third, "第三次反序列化应命中缓存返回同一实例")
        assertTrue(first.name == "java.lang.String", "类名应为 java.lang.String")
    }

    /**
     * 通过 JavaClassStructure 序列化/反序列化，验证内部所有 type 1 LazyClass 被去重
     */
    @Test
    fun testStructureDeserializeLazyClassDedup() {
        // 解析一个类并序列化
        val structure = ClassAnalyser.analyseByASM(LazyClassDeserializeCacheTest::class.java)
        val writer = BinaryWriter()
        structure.writeTo(writer)
        val bytes = writer.toByteArray()

        // 多次反序列化
        val s1 = JavaClassStructure.of(BinaryReader.from(bytes), null)
        val s2 = JavaClassStructure.of(BinaryReader.from(bytes), null)

        // owner（type 1）应该是同一个实例
        assertTrue(
            s1.owner === s2.owner,
            "多次反序列化后 owner LazyClass 应为同一实例，实际: ${s1.owner} vs ${s2.owner}"
        )

        // superclass（type 1，如果存在）应该是同一个实例
        val sc1 = s1.superclass
        val sc2 = s2.superclass
        if (sc1 != null && sc2 != null) {
            assertTrue(sc1 === sc2, "多次反序列化后 superclass LazyClass 应为同一实例")
        }
    }

    /**
     * 验证不同类名的 LazyClass 不会被错误地共享
     */
    @Test
    fun testDifferentClassesNotShared() {
        val lc1 = LazyClass.of(BinaryReader.from(makeBytes("java.lang.String")), null)
        val lc2 = LazyClass.of(BinaryReader.from(makeBytes("java.lang.Integer")), null)

        assertTrue(lc1 !== lc2, "不同类名的 LazyClass 不应为同一实例")
        assertTrue(lc1.name == "java.lang.String")
        assertTrue(lc2.name == "java.lang.Integer")
    }

    /**
     * 验证 type 2（LazyAnnotatedClass）注解为空时走缓存去重
     */
    @Test
    fun testType2EmptyAnnotationsCacheHit() {
        // 构造一个 type 2 的 LazyAnnotatedClass 字节流（空注解）
        val original = LazyAnnotatedClass(
            "java.util.List", 0, false, false,
            { List::class.java }, emptyList(), "java.util.List", "List"
        )
        val writer = BinaryWriter()
        original.writeTo(writer)
        val bytes = writer.toByteArray()

        // 多次反序列化
        val first = LazyClass.of(BinaryReader.from(bytes), null)
        val second = LazyClass.of(BinaryReader.from(bytes), null)

        assertTrue(first is LazyAnnotatedClass, "应为 LazyAnnotatedClass")
        assertTrue(first === second, "空注解的 type 2 多次反序列化应返回同一实例")
    }

    private fun makeBytes(name: String): ByteArray {
        val writer = BinaryWriter()
        val lc = LazyClass.of(name, dimensions = 0, isPrimitive = false) { Class.forName(name) }
        lc.writeTo(writer)
        return writer.toByteArray()
    }

    private fun makeBytes(name: String, simpleName: String, isInstant: Boolean): ByteArray {
        val writer = BinaryWriter()
        writer.writeInt(1)
        writer.writeNullableString(name)
        writer.writeNullableString(simpleName)
        writer.writeInt(0)
        writer.writeBoolean(isInstant)
        writer.writeBoolean(false)
        return writer.toByteArray()
    }
}
