package org.tabooproject.reflex

import java.util.function.Supplier

/**
 * @author 坏黑
 * @since 2022/1/21 6:47 PM
 */
open class LazyClass protected constructor(source: String, val isInstant: Boolean, val getter: Supplier<Class<*>?>) {

    val name = source.replace('/', '.')

    val simpleName = name.substringAfterLast('.')

    val instance by lazy(LazyThreadSafetyMode.NONE) { getter.get() }

    fun notfound(): Nothing = throw ClassNotFoundException("Class not found: $name")

    override fun toString(): String {
        return "LazyClass($name)"
    }

    companion object {

        fun of(clazz: Class<*>): LazyClass {
            return LazyClass(clazz.name, true) { clazz }
        }

        fun of(source: String): LazyClass {
            return LazyClass(source, false) { runCatching { Class.forName(source.replace('/', '.')) }.getOrNull() }
        }

        fun of(source: String, classFinder: ClassAnalyser.ClassFinder): LazyClass {
            return LazyClass(source, false) { classFinder.findClass(source.replace('/', '.')) }
        }

        fun of(source: String, getter: Supplier<Class<*>?>): LazyClass {
            return LazyClass(source, false, getter)
        }
    }
}