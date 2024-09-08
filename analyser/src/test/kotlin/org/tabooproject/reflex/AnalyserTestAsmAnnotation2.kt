package org.tabooproject.reflex

import org.junit.jupiter.api.Test
import org.tabooproject.reflex.asm.AsmAnnotation

/**
 * @author 坏黑
 * @since 2022/1/21 11:49 PM
 */
class AnalyserTestAsmAnnotation2 {

    private val analyse = ClassAnalyser.analyseByASM(TestTargetAsm::class.java)

    @AnalyserAnnotation("test1", cls = AnalyserAnnotation::class)
    private class TestTargetAsm

    @Test
    fun testClassAnnotationGetEnum() {
        val annotation = analyse.getAnnotation(AnalyserAnnotation::class.java)
        assert(annotation is AsmAnnotation)
        assert(annotation.type("cls").name == AnalyserAnnotation::class.java.name)
    }
}