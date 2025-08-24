package org.tabooproject.reflex

import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class InvokeMethodTest {

    object Obj {

        fun func(): String {
            return "ok"
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testInvokeMethod() {
        val reflexClass = ReflexClass.of(Obj::class.java, AnalyseMode.REFLECTION_ONLY)
        val func = reflexClass.getMethod("func")
        println(func)
        println(func.invoke(Obj))

        val time = measureTime {
            repeat(1000000) {
                func.invoke(Obj)
            }
        }
        println(time)
    }
}