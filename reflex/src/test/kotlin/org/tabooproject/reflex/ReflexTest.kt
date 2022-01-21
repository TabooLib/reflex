package org.tabooproject.reflex

import org.junit.jupiter.api.Test
import org.tabooproject.reflex.Reflex.Companion.getProperty
import org.tabooproject.reflex.Reflex.Companion.invokeMethod
import java.lang.IllegalStateException

/**
 * Reflex
 * org.tabooproject.reflex.ReflexTest
 *
 * @author 坏黑
 * @since 2022/1/22 3:01 AM
 */
class ReflexTest {

    private open class TargetParentParent {

        val level = 5
    }

    private open class TargetParent(val user: String) : TargetParentParent() {

        fun walk(value: Int) = value
    }

    private class Target(val id: Int, user: String) : TargetParent(user) {

        fun run(value: Int) = value
    }

    @Test
    fun testGetField() {
        val target = Target(10, "test")
        assert(target.getProperty<Int>("id") == 10)
    }

    @Test
    fun testGetParentField() {
        val target = Target(10, "test")
        assert(target.getProperty<String>("user") == "test")
    }

    @Test
    fun testGetParentParentField() {
        val target = Target(10, "test")
        assert(target.getProperty<Int>("level") == 5)
    }

    @Test
    fun testGetMethod() {
        val target = Target(10, "test")
        assert(target.invokeMethod<Int>("run", 10) == 10)
    }

    @Test
    fun testGetParentMethod() {
        val target = Target(10, "test")
        assert(target.invokeMethod<Int>("walk", 10) == 10)
    }
}