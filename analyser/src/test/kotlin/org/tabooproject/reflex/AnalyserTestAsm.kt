package org.tabooproject.reflex

import org.apache.commons.lang3.BitField
import org.apache.commons.lang3.Range
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestTemplate
import org.tabooproject.reflex.asm.AsmAnnotation
import org.tabooproject.reflex.asm.AsmClassConstructor
import kotlin.reflect.full.memberProperties

/**
 * @author 坏黑
 * @since 2022/1/21 11:49 PM
 */
class AnalyserTestAsm {

    private val analyse = ClassAnalyser.analyse(TestTargetAsm::class.java)

    @AnalyserAnnotation("test1", type = AnalyserAnnotation.Test.C)
    private class TestTargetAsm(private val intVal: Int) {

        @AnalyserAnnotation("test2")
        var range: Range<Int>? = null // NoClassDefFoundError

        var stringVar = "test"

        @AnalyserAnnotation("test3")
        constructor() : this(0)

        fun method() {
        }

        @AnalyserAnnotation("test4")
        private fun method(@AnalyserAnnotation("test5") value: Int): Int {
            return value
        }

        companion object {

            @JvmField
            var bitField: BitField? = null // NoClassDefFoundError

            @JvmField
            val intRangeVal = IntRange.EMPTY

            @JvmStatic
            fun methodStatic(value: Int): Int {
                return value
            }
        }
    }

    @Test
    fun testKotlinReflect() {
        val target = TestTargetAsm(10)
        val find = TestTargetAsm::class.memberProperties.first { it.name == "intVal" }
        try {
            find.get(target)
            throw IllegalStateException()
        } catch (_: NoClassDefFoundError) {
        }
    }

    @Test
    fun testEnv() {
        try {
            Range.between(0, 10)
            throw IllegalStateException()
        } catch (_: NoClassDefFoundError) {
        }
    }

    @Test
    fun testAsm() {
        assert(analyse.constructors[0] is AsmClassConstructor)
    }

    @Test
    fun testAnalyse() {
        // intVal, range, bitField, stringVar, intRangeVal, Companion == 6
        assert(analyse.fields.size == 6)
        // getRange, setRange, getStringVar, setStringVar, method, method, methodStatic == 7
        assert(analyse.methods.size == 7)
        assert(analyse.constructors.size == 2)
    }

    @Test
    fun testInstance() {
        analyse.getConstructor().instance()!!
        analyse.getConstructor(10).instance(10)!!
        analyse.getConstructorByType(Integer::class.java).instance(10)!!
    }

    @Test
    fun testGetVal() {
        val target = TestTargetAsm(10)
        assert(analyse.getField("intVal").get(target) == 10)
    }

    @Test
    fun testSetVal() {
        val target = TestTargetAsm(10)
        analyse.getField("intVal").set(target, 20)
        assert(analyse.getField("intVal").get(target) == 20)
    }

    @Test
    fun testGetVar() {
        val target = TestTargetAsm()
        assert(analyse.getField("stringVar").get(target) == "test")
    }

    @Test
    fun testSetVar() {
        val target = TestTargetAsm()
        analyse.getField("stringVar").set(target, "update")
        assert(target.stringVar == "update")
    }

    @Test
    fun testGetStatic() {
        analyse.getField("intRangeVal").get()!!
    }

    @Test
    fun testSetStatic() {
        analyse.getField("intRangeVal").setStatic(IntRange(1, 10))
        assert(TestTargetAsm.intRangeVal == IntRange(1, 10))
    }

    @Test
    fun testInvokeMethod() {
        val target = TestTargetAsm()
        analyse.getMethod("method").invoke(target)
        assert(analyse.getMethod("method", 10).invoke(target, 10) == 10)
        assert(analyse.getMethodByType("method", Int::class.java).invoke(target, 10) == 10)
    }

    @Test
    fun testInvokeStaticMethod() {
        assert(analyse.getMethod("methodStatic", 10).invokeStatic(10) == 10)
        assert(analyse.getMethodByType("methodStatic", Int::class.java).invokeStatic(10) == 10)
    }

    @Test
    fun testClassAnnotation() {
        val annotations = analyse.annotations
        assert(annotations.size == 2)
        assert(annotations[0] is AsmAnnotation)
        assert(annotations[0].source.name == "org.tabooproject.reflex.AnalyserAnnotation")
    }

    @Test
    fun testClassAnnotationGet1() {
        val annotation = analyse.getAnnotation(AnalyserAnnotation::class.java)
        assert(annotation is AsmAnnotation)
        assert(annotation.property<String>("value") == "test1")
    }

    @Test
    fun testClassAnnotationGet2() {
        val annotation = analyse.getField("range").getAnnotation(AnalyserAnnotation::class.java)
        assert(annotation is AsmAnnotation)
        assert(annotation.property<String>("value") == "test2")
    }

    @Test
    fun testClassAnnotationGet3() {
        val annotation = analyse.getConstructor().getAnnotation(AnalyserAnnotation::class.java)
        assert(annotation is AsmAnnotation)
        assert(annotation.property<String>("value") == "test3")
    }

    @Test
    fun testClassAnnotationGet4() {
        val annotation = analyse.getMethod("method", 0).getAnnotation(AnalyserAnnotation::class.java)
        assert(annotation is AsmAnnotation)
        assert(annotation.property<String>("value") == "test4")
    }

    @Test
    fun testClassAnnotationGet5() {
        val annotation = analyse.getMethod("method", 0).parameter[0].getAnnotation(AnalyserAnnotation::class.java)
        assert(annotation is AsmAnnotation)
        assert(annotation.property<String>("value") == "test5")
    }

    @Test
    fun testClassAnnotationGetEnum() {
        val annotation = analyse.getAnnotation(AnalyserAnnotation::class.java)
        assert(annotation is AsmAnnotation)
        assert(annotation.enum<AnalyserAnnotation.Test>("type") == AnalyserAnnotation.Test.C)
    }
}