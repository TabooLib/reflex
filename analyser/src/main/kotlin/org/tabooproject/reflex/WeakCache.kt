package org.tabooproject.reflex

import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.LinkedHashMap
import java.util.Map
import java.util.concurrent.ConcurrentHashMap

/**
 * 基于 WeakReference 的缓存工具，带 LRU 强引用保护。
 *
 * 与直接使用 `ConcurrentHashMap<K, WeakReference<V>>` 相比，本工具通过引用队列精确清理
 * 已失效的弱引用条目，避免 key 无限累积（ConcurrentHashMap 不会自动清理失效条目）。
 *
 * 语义：
 * - value 主要被弱引用持有，无强引用时可被 GC；
 * - 活跃使用中的对象由调用方强引用持有，不会被回收；
 * - 缓存失效后重新计算（去重为 best-effort）。
 *
 * LRU 强引用保护（strongCapacity）：
 * - 最近被访问的 strongCapacity 个条目额外持有强引用，避免热点条目在
 *   「用完即弃」的调用模式下被 GC 反复回收后重新解析（ASM/反射解析有 CPU 开销）；
 * - 冷条目（超过 strongCapacity 的最久未用）仅保留弱引用，仍可被 GC 回收，
 *   因此不会造成内存无限膨胀；
 * - 该机制不改变外部 API 语义，get/computeIfAbsent/clear/size 行为不变。
 *
 * @param threshold 保留该参数以兼容旧调用方，引用队列实现不再依赖阈值
 * @param strongCapacity LRU 强引用保护条目上限；<=0 表示禁用强引用保护（纯弱引用）
 */
@Suppress("UNUSED_PARAMETER")
class WeakCache<K, V : Any>(
    threshold: Int = 1024,
    private val strongCapacity: Int = 1024,
) {

    private val map = ConcurrentHashMap<K, WeakReference<V>>()
    private val referenceQueue = ReferenceQueue<V>()
    private val enableStrong = strongCapacity > 0

    // access-order LRU 持有强引用（有界，淘汰最久未用）
    private val lru: LinkedHashMap<K, V>? = if (enableStrong) LinkedHashMap<K, V>(16, 0.75f, true) else null

    // 放入 LRU 并淘汰最久未用（accessOrder=true 时迭代顺序为最久未用在前）
    private fun lruPut(key: K, value: V) {
        val l = lru ?: return
        synchronized(l) {
            l.put(key, value)
            while (l.size > strongCapacity) {
                val it = l.keys.iterator()
                if (it.hasNext()) {
                    it.next()
                    it.remove()
                } else break
            }
        }
    }

    /**
     * 获取缓存值；不存在或已失效返回 null。
     */
    fun get(key: K): V? {
        drainQueue()
        val l = lru
        if (l != null) {
            synchronized(l) {
                l.get(key)?.let { return it }
            }
        }
        val reference = map[key] ?: return null
        val value = reference.get()
        if (value == null) {
            map.remove(key, reference)
        } else {
            lruPut(key, value)
        }
        return value
    }

    /**
     * 获取或计算。返回缓存中的值（若仍存活），否则用 [mapping] 计算并放入缓存。
     * 同一 key 的并发计算由 ConcurrentHashMap 串行化，所有调用返回同一结果。
     */
    fun computeIfAbsent(key: K, mapping: () -> V): V {
        drainQueue()
        val l = lru
        if (l != null) {
            synchronized(l) {
                l.get(key)?.let { return it }
            }
        }
        var result: V? = null
        map.compute(key) { _, reference ->
            val cached = reference?.get()
            if (cached != null) {
                result = cached
                reference
            } else {
                val value = mapping()
                result = value
                lruPut(key, value)
                CacheReference(key, value, referenceQueue)
            }
        }
        return result!!
    }

    /**
     * 强制清空缓存。
     */
    fun clear() {
        map.clear()
        lru?.let { synchronized(it) { it.clear() } }
        while (referenceQueue.poll() != null) {
            // 清空已经入队的引用
        }
    }

    /**
     * 当前条目数（含已失效但未清理的）。
     */
    fun size(): Int {
        drainQueue()
        return map.size
    }

    @Suppress("UNCHECKED_CAST")
    private fun drainQueue() {
        while (true) {
            val reference = referenceQueue.poll() as? CacheReference<K, V> ?: return
            map.remove(reference.key, reference)
        }
    }

    companion object {
        /**
         * 便捷：从 map 中取值（供内部遍历等场景使用）
         */
        @Suppress("unused")
        fun <K, V> getOrNull(map: ConcurrentHashMap<K, WeakReference<V>>, key: K): V? {
            return map[key]?.get()
        }
    }

    private class CacheReference<K, V : Any>(
        val key: K,
        value: V,
        queue: ReferenceQueue<V>,
    ) : WeakReference<V>(value, queue)
}
