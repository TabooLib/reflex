package org.tabooproject.reflex

import org.junit.jupiter.api.Test
import org.tabooproject.reflex.res.EventPriority
import org.tabooproject.reflex.res.SubscribeEvent

/**
 * Reflex
 * org.tabooproject.reflex.AnalyserTestAnnotation
 *
 * @author 坏黑
 * @since 2022/8/6 14:47
 */
class AnalyserTestAnnotation {

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun event() {
    }

    @Test
    fun testKotlinReflect() {
        val analyser = ClassAnalyser.analyseByASM(AnalyserTestAnnotation::class.java)
        val method = analyser.getMethod("event")
        assert(method.getAnnotation(SubscribeEvent::class.java).properties().size == 1)
    }
}