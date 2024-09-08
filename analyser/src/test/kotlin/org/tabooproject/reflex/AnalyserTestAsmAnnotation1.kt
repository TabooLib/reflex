package org.tabooproject.reflex

import org.junit.jupiter.api.Test
import org.tabooproject.reflex.res.*

/**
 * Reflex
 * org.tabooproject.reflex.AnalyserTestAnnotation
 *
 * @author 坏黑
 * @since 2022/8/6 14:47
 */
@RuntimeResources(
    value1 = [
        RuntimeResource("resource1", hash = "1", scopes = [DependencyScope.RUNTIME]),
        RuntimeResource("resource2", hash = "1", scopes = [DependencyScope.RUNTIME, DependencyScope.COMPILE])
    ],
    value2 = [1, 2],
    value3 = ["aaa", "bbb"],
    value4 = [true],
    value5 = true,
)
class AnalyserTestAsmAnnotation1 {

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun event() {
    }

    @Test
    fun testAnnotation() {
        val analyser = ClassAnalyser.analyseByASM(AnalyserTestAsmAnnotation1::class.java)
        val method = analyser.getMethod("event")
        assert(method.getAnnotation(SubscribeEvent::class.java).properties().size == 1)
    }

    @Test
    fun testAnnotationObjectArray() {
        val analyser = ClassAnalyser.analyseByASM(AnalyserTestAsmAnnotation1::class.java)
        val value1 = analyser.getAnnotation(RuntimeResources::class.java).list<Any>("value1")
        assert(value1.isNotEmpty())
    }

    @Test
    fun testAnnotationIntArray() {
        val analyser = ClassAnalyser.analyseByASM(AnalyserTestAsmAnnotation1::class.java)
        val value2 = analyser.getAnnotation(RuntimeResources::class.java).intArray("value2")
        assert(value2 != null)
    }

    @Test
    fun testAnnotationStringArray() {
        val analyser = ClassAnalyser.analyseByASM(AnalyserTestAsmAnnotation1::class.java)
        val value3 = analyser.getAnnotation(RuntimeResources::class.java).list<String>("value3")
        assert(value3.isNotEmpty())
    }

    @Test
    fun testAnnotationBooleanArray() {
        val analyser = ClassAnalyser.analyseByASM(AnalyserTestAsmAnnotation1::class.java)
        val value4 = analyser.getAnnotation(RuntimeResources::class.java).booleanArray("value4")
        assert(value4 != null)
    }

    @Test
    fun testAnnotationBoolean() {
        val analyser = ClassAnalyser.analyseByASM(AnalyserTestAsmAnnotation1::class.java)
        val value5 = analyser.getAnnotation(RuntimeResources::class.java).property("value5", false)
        assert(value5)
    }

    @Test
    fun testAnnotationEnumArray() {
        val analyser = ClassAnalyser.analyseByASM(AnalyserTestAsmAnnotation1::class.java)
        println(analyser.getAnnotation(RuntimeResources::class.java).properties())
    }
}