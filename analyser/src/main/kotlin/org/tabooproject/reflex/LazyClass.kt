package org.tabooproject.reflex

import java.util.function.Supplier

/**
 * @author 坏黑
 * @since 2022/1/21 6:47 PM
 */
open class LazyClass protected constructor(source: String, val isArray: Boolean, val isInstant: Boolean, val getter: Supplier<Class<*>?>) {

    /**
     * 类的完全限定名称
     */
    val name = source.replace('/', '.')

    /**
     * 类的简单名称
     */
    val simpleName = name.substringAfterLast('.')

    /**
     * 类的实例
     * 如果是数组类型，则创建一个长度为 0 的数组实例
     */
    val instance by lazy(LazyThreadSafetyMode.NONE) {
        if (isArray) java.lang.reflect.Array.newInstance(getter.get(), 0).javaClass else getter.get()
    }

    /**
     * 此类是否存在
     * 此方法会尝试加载类，如果加载失败，则返回 false
     */
    val isExist by lazy(LazyThreadSafetyMode.NONE) {
        try {
            instance
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * 抛出一个找不到类的异常
     */
    fun notfound(): Nothing = throw ClassNotFoundException("Class not found: $name")

    override fun toString(): String {
        return "LazyClass(${if (isArray) "Array[$name]" else name})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LazyClass) return false
        if (isArray != other.isArray) return false
        if (name != other.name) return false
        return true
    }

    override fun hashCode(): Int {
        var result = isArray.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    companion object {

        /**
         * 创建一个 LazyClass 实例
         *
         * @param clazz 类对象
         * @return LazyClass 实例
         */
        fun of(clazz: Class<*>): LazyClass {
            return LazyClass(clazz.name, isArray = false, isInstant = true) { clazz }
        }

        /**
         * 创建一个 LazyClass 实例
         *
         * @param clazz 类对象
         * @param isArray 是否为数组类型
         * @return LazyClass 实例
         */
        fun of(clazz: Class<*>, isArray: Boolean): LazyClass {
            return LazyClass(clazz.name, isArray, true) { clazz }
        }

        /**
         * 创建一个 LazyClass 实例
         *
         * @param source 类名
         * @return LazyClass 实例
         */
        fun of(source: String): LazyClass {
            return LazyClass(source, isArray = false, isInstant = false) { runCatching { Class.forName(source.replace('/', '.')) }.getOrNull() }
        }

        /**
         * 创建一个 LazyClass 实例
         *
         * @param source 类名
         * @param classFinder 类查找器
         * @return LazyClass 实例
         */
        fun of(source: String, classFinder: ClassAnalyser.ClassFinder): LazyClass {
            return LazyClass(source, isArray = false, isInstant = false) { classFinder.findClass(source.replace('/', '.')) }
        }

        /**
         * 创建一个 LazyClass 实例
         *
         * @param source 类名
         * @param isArray 是否为数组类型
         * @param classFinder 类查找器
         * @return LazyClass 实例
         */
        fun of(source: String, isArray: Boolean, classFinder: ClassAnalyser.ClassFinder): LazyClass {
            return LazyClass(source, isArray, false) { classFinder.findClass(source.replace('/', '.')) }
        }

        /**
         * 创建一个 LazyClass 实例
         *
         * @param source 类名
         * @param getter 类获取器
         * @return LazyClass 实例
         */
        fun of(source: String, getter: Supplier<Class<*>?>): LazyClass {
            return LazyClass(source, isArray = false, isInstant = false, getter = getter)
        }
    }
}