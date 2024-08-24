package org.tabooproject.reflex

import org.junit.jupiter.api.Test
import org.tabooproject.reflex.asm.AsmAnnotation
import org.tabooproject.reflex.reflection.InstantAnnotation
import org.tabooproject.reflex.reflection.InstantClassConstructor

/**
 * @author 坏黑
 * @since 2022/1/21 11:49 PM
 */
class AnalyserTestReflection {

    private val analyse = ClassAnalyser.analyse(TestTargetReflection::class.java)

    @AnalyserAnnotation("test1", type = AnalyserAnnotation.Test.C)
    private class TestTargetReflection(val intVal: Int) {

        @AnalyserAnnotation("test2")
        private var stringVar = "test"

        @AnalyserAnnotation("test3")
        constructor() : this(0)

        fun method() {
        }

        @AnalyserAnnotation("test4")
        private fun method(@AnalyserAnnotation("test5") value1: Int, value2: Int): Int {
            return value1 + value2
        }

        companion object {

            @JvmField
            val intRangeVal = IntRange.EMPTY

            @JvmStatic
            fun methodStatic(value: Int): Int {
                return value
            }
        }
    }

    @Test
    fun testInstant() {
        assert(analyse.constructors[0] is InstantClassConstructor)
    }

    @Test
    fun testAnalyse() {
        // intVal, stringVar, intRangeVal, Companion == 4
        assert(analyse.fields.size == 4)
        // method, method, getIntVal, methodStatic == 4
        assert(analyse.methods.size == 4)
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
        val target = TestTargetReflection(10)
        assert(analyse.getField("intVal").get(target) == 10)
    }

    @Test
    fun testSetVal() {
        val target = TestTargetReflection(10)
        analyse.getField("intVal").set(target, 20)
        assert(target.intVal == 20)
    }

    @Test
    fun testGetVar() {
        val target = TestTargetReflection()
        assert(analyse.getField("stringVar").get(target) == "test")
    }

    @Test
    fun testSetVar() {
        val target = TestTargetReflection()
        analyse.getField("stringVar").set(target, "update")
        assert(analyse.getField("stringVar").get(target) == "update")
    }

    @Test
    fun testGetStatic() {
        analyse.getField("intRangeVal").get()!!
    }

    @Test
    fun testSetStatic() {
        analyse.getField("intRangeVal").setStatic(IntRange(1, 10))
        assert(TestTargetReflection.intRangeVal == IntRange(1, 10))
    }

    @Test
    fun testInvokeMethod() {
        val target = TestTargetReflection()
        analyse.getMethod("method").invoke(target)
        assert(analyse.getMethod("method", 10, 10).invoke(target, 10, 10) == 20)
        assert(analyse.getMethodByType("method", Int::class.java, Int::class.java).invoke(target, 10, 10) == 20)
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
        assert(annotations[0] is InstantAnnotation)
        assert(annotations[0].source.name == "org.tabooproject.reflex.AnalyserAnnotation")
    }

    @Test
    fun testClassAnnotationGet1() {
        val annotation = analyse.getAnnotation(AnalyserAnnotation::class.java)
        assert(annotation is InstantAnnotation)
        assert(annotation.property<String>("value") == "test1")
    }

    @Test
    fun testClassAnnotationGet2() {
        val annotation = analyse.getField("stringVar").getAnnotation(AnalyserAnnotation::class.java)
        assert(annotation is InstantAnnotation)
        assert(annotation.property<String>("value") == "test2")
    }

    @Test
    fun testClassAnnotationGet3() {
        val annotation = analyse.getConstructor().getAnnotation(AnalyserAnnotation::class.java)
        assert(annotation is InstantAnnotation)
        assert(annotation.property<String>("value") == "test3")
    }

    @Test
    fun testClassAnnotationGet4() {
        val annotation = analyse.getMethod("method", 0, 0).getAnnotation(AnalyserAnnotation::class.java)
        assert(annotation is InstantAnnotation)
        assert(annotation.property<String>("value") == "test4")
    }

    @Test
    fun testClassAnnotationGet5() {
        val annotation = analyse.getMethod("method", 0, 0).parameter[0].getAnnotation(AnalyserAnnotation::class.java)
        assert(annotation is InstantAnnotation)
        assert(annotation.property<String>("value") == "test5")
    }

    @Test
    fun testClassAnnotationGetEnum() {
        val annotation = analyse.getAnnotation(AnalyserAnnotation::class.java)
        assert(annotation is InstantAnnotation)
        assert(annotation.enum<AnalyserAnnotation.Test>("type") == AnalyserAnnotation.Test.C)
    }
}