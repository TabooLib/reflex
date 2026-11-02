package org.tabooproject.reflex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.LinkedList

/**
 * 验证 ClassStructure 的集合字段使用不可变 List 实现。
 *
 * 这些集合在构造后不再修改，使用 LinkedList 会为每个元素额外分配一个节点对象，
 * 且随机访问是 O(n)。改为不可变 List 后语义更明确，也减少了内存开销。
 */
class ClassStructureCollectionsTest {

    private interface SampleInterface

    @Suppress("unused")
    private class Sample : SampleInterface {
        val fieldA: String = "a"
        val fieldB: Int = 1

        fun methodA(): String = "a"

        fun methodB(value: Int): Int = value
    }

    @Test
    fun testStructuralCollectionsAreNotLinkedList() {
        val structure = ClassAnalyser.analyseByReflection(Sample::class.java)
        val collections = mapOf(
            "interfaces" to structure.interfaces,
            "annotations" to structure.annotations,
            "fields" to structure.fields,
            "methods" to structure.methods,
            "constructors" to structure.constructors,
        )
        collections.forEach { (name, collection) ->
            assertFalse(
                collection is LinkedList<*>,
                "$name 不应为每个元素分配 LinkedList 节点",
            )
        }
    }

    @Test
    fun testStructuralCollectionsPreserveOrderAndContent() {
        val source = Sample::class.java
        val structure = ClassAnalyser.analyseByReflection(source)

        assertEquals(source.interfaces.map { it.name }, structure.interfaces.map { it.name })
        assertEquals(source.declaredAnnotations.map { it.annotationClass.java.name }, structure.annotations.map { it.source.name })
        assertEquals(source.declaredFields.map { it.name }, structure.fields.map { it.name })
        assertEquals(source.declaredMethods.map { it.name }, structure.methods.map { it.name })
        assertEquals(source.declaredConstructors.size, structure.constructors.size)
    }
}
