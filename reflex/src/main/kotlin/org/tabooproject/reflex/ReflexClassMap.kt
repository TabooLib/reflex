package org.tabooproject.reflex

import org.tabooproject.reflex.serializer.BinaryReader
import org.tabooproject.reflex.serializer.BinaryWriter
import java.util.concurrent.ConcurrentHashMap

/**
 * 基于 ByteBuffer 的二进制序列化工具
 * 以避免每次启动时重新加载所有类导致的性能问题
 *
 * 目前仅支持对 ASM 模式进行序列化
 */
object ReflexClassMap {

    /** 版本 0: 原始格式，立即反序列化所有类 */
    const val VERSION_LEGACY = 0

    /** 版本 1: 延迟加载格式，只在访问时反序列化 */
    const val VERSION_LAZY = 1

    /** 当前版本 */
    const val VERSION = VERSION_LAZY

    /**
     * 序列化为字节数组（新格式，支持延迟加载）
     *
     * 格式:
     * [VERSION: 4 bytes]
     * [COUNT: 4 bytes]
     * [INDEX TABLE]
     *   - [className: String][dataOffset: 4 bytes][dataSize: 4 bytes] × COUNT
     * [DATA SECTION]
     *   - [class binary data] × COUNT
     */
    fun serializeToBytes(map: Map<String, ReflexClass>): ByteArray {
        // 第一步：序列化所有类数据，记录每个类的大小
        val classDataList = map.map { (name, reflexClass) ->
            val classWriter = BinaryWriter()
            classWriter.writeObj(reflexClass)
            Triple(name, classWriter.toByteArray(), 0) // offset 稍后计算
        }
        // 第二步：计算索引表大小和数据偏移量
        val indexWriter = BinaryWriter()
        classDataList.forEach { (name, _, _) ->
            indexWriter.writeNullableString(name)
            indexWriter.writeInt(0) // placeholder for offset
            indexWriter.writeInt(0) // placeholder for size
        }
        val indexSize = indexWriter.size()
        val headerSize = 4 + 4 // VERSION + COUNT
        // 第三步：计算每个类的实际偏移量
        var currentOffset = headerSize + indexSize
        val classDataWithOffset = classDataList.map { (name, data, _) ->
            val offset = currentOffset
            currentOffset += data.size
            Triple(name, data, offset)
        }

        // 第四步：写入最终数据
        val writer = BinaryWriter()
        writer.writeInt(VERSION)
        writer.writeInt(map.size)
        // 写入索引表
        classDataWithOffset.forEach { (name, data, offset) ->
            writer.writeNullableString(name)
            writer.writeInt(offset)
            writer.writeInt(data.size)
        }
        // 写入数据区
        classDataWithOffset.forEach { (_, data, _) ->
            writer.writeBytes(data)
        }
        return writer.toByteArray()
    }

    /**
     * 反序列化为 Map（自动检测格式）
     */
    fun deserializeFromBytes(bytes: ByteArray, classFinder: ClassAnalyser.ClassFinder?): Map<String, ReflexClass> {
        val reader = BinaryReader(bytes)
        val version = reader.readInt()
        // println("[ReflexClassMap] Deserializing with version=$version (LAZY=$VERSION_LAZY)")
        return when (version) {
            VERSION_LEGACY -> deserializeLegacy(reader, classFinder)
            VERSION_LAZY -> deserializeLazy(bytes, reader, classFinder)
            else -> throw IllegalArgumentException("Unsupported version: $version")
        }
    }

    /**
     * 反序列化为延迟加载 Map
     * 返回的 Map 在访问具体类时才进行反序列化
     */
    fun deserializeFromBytesLazy(bytes: ByteArray, classFinder: ClassAnalyser.ClassFinder?): Map<String, ReflexClass> {
        val reader = BinaryReader(bytes)
        return when (val version = reader.readInt()) {
            VERSION_LEGACY -> deserializeLegacy(reader, classFinder)
            VERSION_LAZY -> deserializeLazy(bytes, reader, classFinder)
            else -> throw IllegalArgumentException("Unsupported version: $version")
        }
    }

    /**
     * 兼容旧版本格式的反序列化
     */
    private fun deserializeLegacy(reader: BinaryReader, classFinder: ClassAnalyser.ClassFinder?): Map<String, ReflexClass> {
        val map = mutableMapOf<String, ReflexClass>()
        repeat(reader.readInt()) {
            val name = reader.readNullableString()!!
            try {
                map[name] = ReflexClass.of(reader, classFinder)
            } catch (ex: Throwable) {
                println("Failed to deserialize class $name")
                throw ex
            }
        }
        return map
    }

    /**
     * 延迟加载格式的反序列化
     * 只读取索引表，返回延迟加载的 Map
     */
    private fun deserializeLazy(bytes: ByteArray, reader: BinaryReader, classFinder: ClassAnalyser.ClassFinder?): Map<String, ReflexClass> {
        val count = reader.readInt()
        val startTime = System.currentTimeMillis()
        // 读取索引表
        val index = (0 until count).map {
            val name = reader.readNullableString()!!
            val offset = reader.readInt()
            val size = reader.readInt()
            IndexEntry(name, offset, size)
        }
        val indexTime = System.currentTimeMillis() - startTime
        // println("[ReflexClassMap] Read $count index entries in ${indexTime}ms")
        // 返回延迟加载的 Map
        return LazyReflexClassMap(bytes, index, classFinder)
    }

    /**
     * 索引条目
     */
    private data class IndexEntry(
        val name: String,
        val offset: Int,
        val size: Int
    )

    /**
     * 延迟加载的 ReflexClass Map
     * 只有在访问具体类时才进行反序列化
     */
    private class LazyReflexClassMap(
        private val bytes: ByteArray,
        private val index: List<IndexEntry>,
        private val classFinder: ClassAnalyser.ClassFinder?
    ) : AbstractMap<String, ReflexClass>() {

        /** 索引映射：类名 -> 索引条目 */
        private val indexMap: Map<String, IndexEntry> = index.associateBy { it.name }

        /** 已解析的类缓存 */
        private val cache = ConcurrentHashMap<String, ReflexClass>()

        /** 所有键集合 */
        private val keySet: Set<String> = indexMap.keys

        override val entries: Set<Map.Entry<String, ReflexClass>>
            get() = object : AbstractSet<Map.Entry<String, ReflexClass>>() {
                override val size: Int get() = index.size
                override fun iterator(): Iterator<Map.Entry<String, ReflexClass>> {
                    return index.asSequence().map { entry ->
                        object : Map.Entry<String, ReflexClass> {
                            override val key: String get() = entry.name
                            override val value: ReflexClass get() = getOrDeserialize(entry.name)!!
                        }
                    }.iterator()
                }
            }

        override val keys: Set<String>
            get() = keySet

        override val size: Int
            get() = index.size

        override fun containsKey(key: String): Boolean = indexMap.containsKey(key)

        override fun get(key: String): ReflexClass? = getOrDeserialize(key)

        /**
         * 获取或反序列化类
         */
        private fun getOrDeserialize(name: String): ReflexClass? {
            // 先检查缓存
            cache[name]?.let { return it }
            // 获取索引条目
            val entry = indexMap[name] ?: return null
            // 反序列化并缓存
            return cache.computeIfAbsent(name) {
                try {
                    val reader = BinaryReader(bytes, entry.offset)
                    val reflexClass = ReflexClass.of(reader, classFinder)
                    // 同时添加到全局缓存
                    ReflexClass.reflexClassCacheMap[name] = reflexClass
                    reflexClass
                } catch (ex: Throwable) {
                    println("Failed to deserialize class $name")
                    throw ex
                }
            }
        }

        /**
         * 预加载指定的类（用于批量加载）
         */
        fun preload(names: Collection<String>) {
            names.forEach { getOrDeserialize(it) }
        }

        /**
         * 并行预加载指定的类
         */
        fun preloadParallel(names: Collection<String>) {
            names.parallelStream().forEach { getOrDeserialize(it) }
        }
    }
}