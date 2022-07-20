package org.tabooproject.reflex

/**
 * @author 坏黑
 * @since 2022/1/21 6:47 PM
 */
open class LazyClass(source: String) {

    val name = source.replace('/', '.')

    open val instance by lazy(LazyThreadSafetyMode.NONE) { kotlin.runCatching { Class.forName(name) }.getOrNull() }

    override fun toString(): String {
        return "LazyClass(name='$name')"
    }
}