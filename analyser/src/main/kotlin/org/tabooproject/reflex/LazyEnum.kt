package org.tabooproject.reflex

import java.util.concurrent.ConcurrentHashMap

/**
 * @author 坏黑
 * @since 2022/1/24 9:27 PM
 */
@Suppress("UNCHECKED_CAST")
class LazyEnum(val source: LazyClass, val name: String) {

    val instance by lazy(LazyThreadSafetyMode.NONE) {
        allOf(source.instance as Class<Enum<*>>)[name]!!
    }

    override fun toString(): String {
        return "LazyEnum(source=$source, name='$name')"
    }

    companion object {

        val map = ConcurrentHashMap<String, Map<String, Enum<*>>>()

        fun allOf(enumClass: Class<Enum<*>>): Map<String, Enum<*>> {
            return map.getOrPut(enumClass.name) { enumClass.enumConstants.associateBy { (it as Enum<*>).name } }
        }
    }
}