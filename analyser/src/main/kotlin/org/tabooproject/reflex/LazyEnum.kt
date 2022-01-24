package org.tabooproject.reflex

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * @author 坏黑
 * @since 2022/1/24 9:27 PM
 */
@Suppress("UNCHECKED_CAST")
class LazyEnum(val source: LazyClass, val name: String) {

    val instance: Enum<*> by lazy { allOf(source.instance as Class<Enum<*>>)[name]!! }

    override fun toString(): String {
        return "LazyEnum(source=$source, name='$name')"
    }

    private companion object {

        val map = ConcurrentHashMap<String, Map<String, Enum<*>>>()

        fun allOf(enumClass: Class<Enum<*>>): Map<String, Enum<*>> {
            return map.computeIfAbsent(enumClass.name) { EnumSet.allOf(enumClass).associateBy { it.name } }
        }
    }
}