package org.tabooproject.reflex

/**
 * @author 坏黑
 * @since 2022/1/24 7:42 PM
 */
@Suppress("UNCHECKED_CAST")
abstract class ClassAnnotation(val source: LazyClass) {

    abstract fun <T> property(name: String): T?

    abstract fun <T> property(name: String, def: T): T

    abstract fun properties(): Map<String, Any>

    abstract fun propertyKeys(): Set<String>

    fun charArray(name: String): CharArray? {
        return property(name, null)
    }

    fun byteArray(name: String): ByteArray? {
        return property(name, null)
    }

    fun shortArray(name: String): ShortArray? {
        return property(name, null)
    }

    fun intArray(name: String): IntArray? {
        return property(name, null)
    }

    fun longArray(name: String): LongArray? {
        return property(name, null)
    }

    fun doubleArray(name: String): DoubleArray? {
        return property(name, null)
    }

    fun floatArray(name: String): FloatArray? {
        return property(name, null)
    }

    fun booleanArray(name: String): BooleanArray? {
        return property(name, null)
    }

    fun <T> list(name: String): List<T> {
        return property(name, arrayListOf())
    }

    fun mapList(name: String): List<Map<String, Any>> {
        return property(name, arrayListOf())
    }

    // 给 Java 用
    fun <T> getEnum(name: String): T {
        return enum(name)
    }

    fun <T> getEnum(name: String, def: T): T {
        return enum(name, def)
    }

    // 当时起这名字没想到 Java 无法访问，我笑了。
    fun <T> enum(name: String): T {
        val el = property<Any?>(name) ?: throw EnumNotFoundException(name)
        return if (el is LazyEnum) el.instance as T else el as T
    }

    fun <T> enum(name: String, def: T): T {
        val el = property<Any?>(name) ?: return def
        return if (el is LazyEnum) el.instance as T else el as T
    }

    fun enumName(name: String): String {
        val el = property<Any?>(name) ?: throw EnumNotFoundException(name)
        return if (el is LazyEnum) el.name else el.toString()
    }

    fun enumName(name: String, def: String): String {
        val el = property<Any?>(name) ?: return def
        return if (el is LazyEnum) el.name else el.toString()
    }

    fun <T> enumList(name: String): List<T> {
        return list<LazyEnum>(name).map { it.instance as T }
    }

    fun enumNameList(name: String): List<String> {
        return list<LazyEnum>(name).map { it.name }
    }

    override fun toString(): String {
        return "ClassAnnotation(source=$source, properties=${propertyKeys()})"
    }
}