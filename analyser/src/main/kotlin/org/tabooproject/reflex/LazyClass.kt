package org.tabooproject.reflex

/**
 * @author 坏黑
 * @since 2022/1/21 6:47 PM
 */
open class LazyClass(val name: String) {

    open val instance: Class<*>? by lazy { kotlin.runCatching { Class.forName(name.replace('/', '.')) }.getOrNull() }

    override fun toString(): String {
        return "LazyClass(name='$name')"
    }
}