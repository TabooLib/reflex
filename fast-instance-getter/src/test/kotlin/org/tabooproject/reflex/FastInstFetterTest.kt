package org.tabooproject.reflex

import org.junit.jupiter.api.Test

/**
 * @author 坏黑
 * @since 2022/1/22 4:03 AM
 */
class FastInstFetterTest {

    object ObjectTarget

    class CompanionTarget {

        companion object
    }

    @Test
    fun testObjectInstance() {
        assert(ObjectTarget == FastInstGetter(ObjectTarget::class.java.name).instance)
    }

    @Test
    fun testCompanionInstance() {
        assert(CompanionTarget.Companion == FastInstGetter(CompanionTarget::class.java.name).companion)
    }
}