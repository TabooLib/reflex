package org.tabooproject.reflex

import org.tabooproject.reflex.serializer.BinaryReader
import org.tabooproject.reflex.serializer.BinarySerializable
import org.tabooproject.reflex.serializer.BinaryWriter
import java.io.DataOutputStream
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
    val instance by lazy(LazyThreadSafetyMode.NONE) {
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
        return "LazyClass(${"[".repeat(dimensions)}$name)"
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

        /**
         * 创建一个 LazyClass 实例
         *
         * @param clazz 类对象
         * @return LazyClass 实例
         */
        fun of(clazz: Class<*>, dimensions: Int = clazz.getArrayDimensions()): LazyClass {
            return LazyClass(clazz.name, dimensions, isInstant = true, clazz.isPrimitive, { clazz })
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
            return LazyClass(source, dimensions, isInstant = false, isPrimitive, { finder.findClass(source.replace('/', '.')) })
        }

        /**
         * 创建一个 LazyClass 实例
         *
         * @param source 类名
         * @param getter 类获取器
         * @return LazyClass 实例
         */
        fun of(source: String, dimensions: Int = 0, isPrimitive: Boolean = false, getter: Supplier<Class<*>?>): LazyClass {
            return LazyClass(source, dimensions, isInstant = false, isPrimitive, classGetter = getter)
        }

        /**
         * 从 BinaryReader 中读取一个 LazyClass 实例
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
            if (type == 1) {
                return LazyClass(name, dimensions, isInstant, isPrimitive, classGetter, name, simpleName)
            } else if (type == 2) {
                val annotations = reader.readAnnotationList(classFinder)
                return LazyAnnotatedClass(name, dimensions, isInstant, isPrimitive, classGetter, annotations, name, simpleName)
            } else {
                error("Unknown type: $type")
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
    }
}