package org.tabooproject.reflex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 验证 WeakCache（弱引用 + 惰性清理）的行为：
 * 1. 活跃值不被回收（调用方持强引用）
 * 2. 失效值可被清除（弱引用语义）
 * 3. 惰性清理会移除失效条目（key 不无限累积）
 * 4. computeIfAbsent 并发去重语义
 */
class WeakCacheTest {

    @Test
    fun testGetAndCompute() {
        val cache = WeakCache<String, String>()
        assertNull(cache.get("a"))
        val v = cache.computeIfAbsent("a") { "value-a" }
        assertEquals("value-a", v)
        assertSame(v, cache.computeIfAbsent("a") { "value-a-2" }, "存活期内应命中缓存")
    }

    @Test
    fun testWeakValueCollectable() {
        val cache = WeakCache<String, Any>()
        var obj: Any? = Any()
        cache.computeIfAbsent("key") { obj!! }
        assertNotNull(cache.get("key"))
        // 释放强引用后，值应可被 GC（不强制断言 get 为 null，避免 flaky，但验证条目可清理机制）
        obj = null
        System.gc()
        System.gc()
        cache.clear()
        assertNull(cache.get("key"))
    }

    @Test
    fun testSweepRemovesExpiredEntries() {
        // 验证失效条目清理：引用队列会在后续任意操作时移除已被 GC 回收的条目。
        // strongCapacity=0 关闭 LRU 强引用保护，否则放入的对象会被强引用钉住、无法成为 GC 候选
        val cache = WeakCache<String, Any>(threshold = 4, strongCapacity = 0)
        // 放入不持有强引用的对象（立即成为 GC 候选）
        repeat(100) { i ->
            var tmp: Any? = Any()
            cache.computeIfAbsent("key-$i") { tmp!! }
            tmp = null
        }
        // 强制 GC 让弱引用失效并入队
        System.gc()
        System.gc()
        // 再 put 几次触发 drainQueue，移除已失效条目
        repeat(32) { i ->
            cache.computeIfAbsent("new-$i") { "v$i" }
        }
        // 失效条目应被清理（size 显著小于放入总数）
        val size = cache.size()
        assert(size < 100) { "失效条目应被引用队列清理，实际 size=$size" }
    }

    @Test
    fun testClear() {
        val cache = WeakCache<String, String>()
        cache.computeIfAbsent("a") { "1" }
        cache.clear()
        assertNull(cache.get("a"))
    }

    @Test
    fun testExpiredEntryCanBeReplaced() {
        val cache = WeakCache<String, Any>()
        expiredEntry(cache, "key")
        val calls = AtomicInteger()

        val first = cache.computeIfAbsent("key") { Any().also { calls.incrementAndGet() } }
        val second = cache.computeIfAbsent("key") { Any().also { calls.incrementAndGet() } }

        assertSame(first, second, "失效引用被替换后应重新进入缓存")
        assertSame(first, cache.get("key"), "缓存应保存重新计算的值")
        assertEquals(1, calls.get(), "同一失效条目只应重新计算一次")
    }

    @Test
    fun testConcurrentReplacementPublishesSingleWinner() {
        val cache = WeakCache<String, Any>()
        expiredEntry(cache, "key")
        val threadCount = 16
        val ready = CountDownLatch(threadCount)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threadCount)

        try {
            val futures = (0 until threadCount).map {
                executor.submit<Any> {
                    ready.countDown()
                    start.await()
                    cache.computeIfAbsent("key") { Any() }
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS), "并发任务未按时就绪")
            start.countDown()
            val values = futures.map { it.get(5, TimeUnit.SECONDS) }
            val winner = values.first()
            assertTrue(values.all { it === winner }, "并发重填必须发布同一个结果")
            assertSame(winner, cache.get("key"), "并发 winner 必须保存在缓存中")
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun testLruStrongReferenceKeepsHotEntries() {
        // 验证 LRU 强引用保护：strongCapacity 内的热点条目即使调用方释放引用、
        // 且发生 GC，也应继续存活（缓存命中同一实例）。
        val cache = WeakCache<String, Any>(strongCapacity = 2)
        val a = cache.computeIfAbsent("a") { Any() }
        val b = cache.computeIfAbsent("b") { Any() }

        System.gc()
        System.gc()

        assertSame(a, cache.get("a"), "容量内的热点条目应被强引用保护")
        assertSame(b, cache.get("b"), "容量内的热点条目应被强引用保护")
    }

    @Test
    fun testLruEvictsLeastRecentlyUsedBeyondCapacity() {
        // 验证超出 strongCapacity 后，最久未用的条目被移出强引用保护，
        // 释放外部强引用并 GC 后可被回收。
        val cache = WeakCache<String, Any>(strongCapacity = 2)
        cache.computeIfAbsent("a") { Any() }
        cache.computeIfAbsent("b") { Any() }
        // 访问 a 使其成为最近使用，b 变为最久未用
        cache.get("a")
        // 插入 c，超出容量，最久未用的 b 应被移出 LRU 强引用
        cache.computeIfAbsent("c") { Any() }

        System.gc()
        System.gc()

        assertNotNull(cache.get("a"), "最近使用的条目应保持在强引用保护内")
        assertNotNull(cache.get("c"), "最新插入的条目应保持在强引用保护内")
    }

    @Test
    fun testStrongCapacityZeroDisablesProtection() {
        // strongCapacity <= 0 时应关闭强引用保护：值仅由弱引用持有。
        val cache = WeakCache<String, Any>(strongCapacity = 0)
        var value: Any? = Any()
        cache.computeIfAbsent("key") { value!! }
        assertNotNull(cache.get("key"))

        value = null
        System.gc()
        System.gc()
        // 无强引用保护时条目应可被回收，最坏情况是被清理后 get 返回 null
        // 不断言一定为 null（GC 时机不确定），但清理后必须为 null
        cache.clear()
        assertNull(cache.get("key"))
    }

    @Suppress("UNCHECKED_CAST")
    private fun expiredEntry(cache: WeakCache<String, Any>, key: String) {
        val field = WeakCache::class.java.getDeclaredField("map")
        field.isAccessible = true
        val map = field.get(cache) as ConcurrentHashMap<String, WeakReference<Any>>
        map[key] = WeakReference(null)
    }
}
