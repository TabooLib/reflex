package org.tabooproject.reflex

import org.junit.jupiter.api.Test
import java.lang.IllegalStateException

/**
 * Reflex
 * org.tabooproject.reflex.ReflexTest
 *
 * @author 坏黑
 * @since 2022/1/22 3:01 AM
 */
class ReflexClassTest {

    private open class TargetParentParent {

        val level = 0
    }

    private open class TargetParent(val user: String) : TargetParentParent() {

        fun walk() {}
    }

    private class Target(val id: Int, user: String) : TargetParent(user) {

        fun run() {}
    }

    private val target = Target(10, "test")
    private val targetReflexClass = ReflexClass.of(target::class.java)

    @Test
    fun testGetField() {
        targetReflexClass.getField("id")
    }

    @Test
    fun testGetParentField() {
        targetReflexClass.getField("user")
    }

    @Test
    fun testGetParentParentField() {
        targetReflexClass.getField("level")
    }

    @Test
    fun testGetInvalidField() {
        try {
            targetReflexClass.getField("grade")
            throw IllegalStateException()
        } catch (_: NoSuchFieldException) {
        }
    }

    @Test
    fun testGetMethod() {
        targetReflexClass.getMethod("run")
    }

    @Test
    fun testGetParentMethod() {
        targetReflexClass.getMethod("walk")
    }

    @Test
    fun testGetInvalidMethod() {
        try {
            targetReflexClass.getMethod("NotDefined")
            throw IllegalStateException()
        } catch (_: NoSuchMethodException) {
        }
    }

    @Test
    fun testGetMethodByType() {
        targetReflexClass.getMethodByTypes("run")
        targetReflexClass.getMethodByTypes("walk")
    }
}