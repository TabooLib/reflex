package org.tabooproject.reflex

import org.tabooproject.reflex.serializer.BinaryReader
import org.tabooproject.reflex.serializer.BinarySerializable
import org.tabooproject.reflex.serializer.BinaryWriter
import java.io.DataOutputStream
import java.util.*
import java.util.function.Supplier

/**
 * 一个懒加载的类
 *
 * @property source 类地址，可能是 "." 也可能是 "/"，不确定
 * @property dimensions 数组维度
 * @property isPrimitive 是否为基本类型
 * @property isInstant 是否已经实例化（是否已经加载，此时 classFinder 必定为空，getter 直接返回类本身）
 * @property classGetter 获取 Class 对象，通常是 forName 的包装
 */
open class LazyClass internal constructor(
    source: String,
    val dimensions: Int,
    val isInstant: Boolean,
    val isPrimitive: Boolean,
    val classGetter: Supplier<Class<*>?>,
    val name: String = source.replace('/', '.'),
    val simpleName: String = name.substringAfterLast('.'),
) : BinarySerializable {

    /**
     * 是否为数组类型
     * dimensions > 0 时为 true
     */
    val isArray: Boolean
        get() = dimensions > 0

    /**
     * 类的实例
     * 如果是数组类型，则创建一个多维数组实例
     * 例如：
     * dimensions = 1 时创建 new Type[0]
     * dimensions = 2 时创建 new Type[0][0]
     * dimensions = 3 时创建 new Type[0][0][0]
     */
    val instance by lazy {
        if (isArray) {
            // 递归创建多维数组
            fun createArray(componentType: Class<*>, dim: Int): Class<*> {
                return if (dim <= 0) {
                    componentType
                } else {
                    createArray(java.lang.reflect.Array.newInstance(componentType, 0).javaClass, dim - 1)
                }
            }
            createArray(classGetter.get()!!, dimensions)
        } else {
            classGetter.get()
        }
    }

    /**
     * 此类是否存在
     * 此方法会尝试加载类，如果加载失败，则返回 false
     */
    val isExist by lazy {
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
        return "LazyClass($name,dim=$dimensions)"
    }

    override fun writeTo(writer: BinaryWriter) {
        writer.writeInt(1) // 1：表示 LazyClass
        writer.writeNullableString(name)
        writer.writeNullableString(simpleName)
        writer.writeInt(dimensions)
        writer.writeBoolean(isInstant)
        writer.writeBoolean(isPrimitive)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LazyClass) return false
        if (dimensions != other.dimensions) return false
        if (name != other.name) return false
        return true
    }

    override fun hashCode(): Int {
        var result = dimensions
        result = 31 * result + name.hashCode()
        return result
    }

    companion object {

        private data class CacheKey(val clazz: Class<*>, val dimensions: Int)
        private data class StringCacheKey(val source: String, val dimensions: Int, val isPrimitive: Boolean, val finder: IdentityKey<ClassAnalyser.ClassFinder>)
        private data class SupplierCacheKey(val source: String, val dimensions: Int, val isPrimitive: Boolean, val getter: IdentityKey<Supplier<Class<*>?>>)

        /**
         * 反序列化缓存 key。
         * 包含 classFinder 身份，避免不同类加载器下同名类串用（跨插件场景）。
         * finder 通常为长生命周期单例（每个插件的类加载器对应一个），不会被回收。
         */
        private data class DeserializeCacheKey(
            val name: String,
            val simpleName: String,
            val dimensions: Int,
            val isInstant: Boolean,
            val isPrimitive: Boolean,
            val finder: IdentityKey<ClassAnalyser.ClassFinder>,
        )

        private val classCache = WeakCache<CacheKey, LazyClass>()
        private val stringCache = WeakCache<StringCacheKey, LazyClass>()
        private val supplierCache = WeakCache<SupplierCacheKey, LazyClass>()

        /**
         * 反序列化专用缓存（type 1，无注解的 LazyClass）
         * 同一个类名在反序列化时只创建一个实例，避免大量重复对象。
         * 使用弱引用 + 惰性清理：反序列化去重仅在缓存存活期内生效，
         * 失效条目会被自动清理，避免类名/类型对象无限累积。
         */
        private val deserializeCache = WeakCache<DeserializeCacheKey, LazyClass>()

        /**
         * 反序列化专用缓存（type 2，注解为空的 LazyAnnotatedClass）
         * 大多数方法参数没有注解，annotations 为空时与 LazyClass 行为一致，可安全去重。
         * 使用弱引用 + 惰性清理，理由同上。
         */
        private val deserializeAnnotatedCache = WeakCache<DeserializeCacheKey, LazyAnnotatedClass>()

        /**
         * 创建一个 LazyClass 实例
         *
         * @param clazz 类对象
         * @return LazyClass 实例
         */
        fun of(clazz: Class<*>, dimensions: Int = clazz.getArrayDimensions()): LazyClass {
            val key = CacheKey(clazz, dimensions)
            return classCache.computeIfAbsent(key) {
                LazyClass(clazz.name, dimensions, isInstant = true, clazz.isPrimitive, { clazz })
            }
        }

        /**
         * 创建一个 LazyClass 实例
         *
         * @param source 类名
         * @param classFinder 类查找器
         * @return LazyClass 实例
         */
        fun of(source: String, dimensions: Int = 0, isPrimitive: Boolean = false, classFinder: ClassAnalyser.ClassFinder?): LazyClass {
            val finder = classFinder ?: ClassAnalyser.ClassFinder.default
            val key = StringCacheKey(source, dimensions, isPrimitive, IdentityKey(finder))
            return stringCache.computeIfAbsent(key) {
                LazyClass(source, dimensions, isInstant = false, isPrimitive, { finder.findClass(source.replace('/', '.')) })
            }
        }

        /**
         * 创建一个 LazyClass 实例
         *
         * @param source 类名
         * @param getter 类获取器
         * @return LazyClass 实例
         */
        fun of(source: String, dimensions: Int = 0, isPrimitive: Boolean = false, getter: Supplier<Class<*>?>): LazyClass {
            val key = SupplierCacheKey(source, dimensions, isPrimitive, IdentityKey(getter))
            return supplierCache.computeIfAbsent(key) {
                LazyClass(source, dimensions, isInstant = false, isPrimitive, classGetter = getter)
            }
        }

        /**
         * 从 BinaryReader 中读取一个 LazyClass 实例
         *
         * type 1（LazyClass，无注解）走反序列化缓存，同一类名只创建一个实例
         * type 2（LazyAnnotatedClass，有注解）不走缓存，因为同一类名在不同参数位置可能有不同注解
         */
        fun of(reader: BinaryReader, classFinder: ClassAnalyser.ClassFinder?): LazyClass {
            val type = reader.readInt()
            val name = reader.readNullableString()!!
            val simpleName = reader.readNullableString()!!
            val dimensions = reader.readInt()
            val isInstant = reader.readBoolean()
            val isPrimitive = reader.readBoolean()
            val finder = classFinder ?: ClassAnalyser.ClassFinder.default
            val classGetter = Supplier { if (isPrimitive) Reflection.getPrimitiveType(name[0]) else finder.findClass(name) }
            when (type) {
                1 -> {
                    // type 1 无额外数据，缓存命中时 reader 位置已正确，零跳过开销
                    val key = DeserializeCacheKey(name, simpleName, dimensions, isInstant, isPrimitive, IdentityKey(finder))
                    return deserializeCache.computeIfAbsent(key) {
                        LazyClass(name, dimensions, isInstant, isPrimitive, classGetter, name, simpleName)
                    }
                }
                2 -> {
                    // type 2 有额外的 annotations 列表，需要读取以推进 reader
                    val annotations = reader.readAnnotationList(classFinder)
                    // 注解为空时与 LazyClass 行为一致，可安全按类名去重
                    if (annotations.isEmpty()) {
                        val key = DeserializeCacheKey(name, simpleName, dimensions, isInstant, isPrimitive, IdentityKey(finder))
                        return deserializeAnnotatedCache.computeIfAbsent(key) {
                            LazyAnnotatedClass(name, dimensions, isInstant, isPrimitive, classGetter, emptyList(), name, simpleName)
                        }
                    }
                    // 注解非空时不去重，因为同一类名在不同参数位置可能有不同注解
                    return LazyAnnotatedClass(name, dimensions, isInstant, isPrimitive, classGetter, annotations, name, simpleName)
                }
                else -> {
                    error("Unknown type: $type")
                }
            }
        }

        fun writeTo(clazz: Class<*>, output: DataOutputStream) {
            // 名字
            val name = clazz.name
            output.writeInt(name.length)
            output.write(name.toByteArray(), 0, name.length)
            // 简单名
            val simpleName = clazz.simpleName
            output.writeInt(simpleName.length)
            output.write(simpleName.toByteArray(), 0, simpleName.length)
            // 类信息
            output.writeInt(clazz.getArrayDimensions())
            output.writeBoolean(true)
            output.writeBoolean(clazz.isPrimitive)
        }

        /**
         * 清空反序列化去重缓存。
         * 供 [Reflex.clearCaches] 调用，释放不再被引用的 LazyClass/LazyAnnotatedClass。
         */
        fun clearDeserializeCaches() {
            deserializeCache.clear()
            deserializeAnnotatedCache.clear()
        }

        fun clearCaches() {
            classCache.clear()
            stringCache.clear()
            supplierCache.clear()
            clearDeserializeCaches()
        }

        private class IdentityKey<T : Any>(private val value: T) {

            override fun equals(other: Any?): Boolean {
                return other is IdentityKey<*> && value === other.value
            }

            override fun hashCode(): Int {
                return System.identityHashCode(value)
            }
        }
    }
}
