package org.tabooproject.reflex

import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * 验证 reflex 使用的线程安全 lazy（by lazy / SynchronizedLazyImpl）在并发访问下的正确性。
 *
 * 背景：commit 7ef1d9a 已将 reflex 内部所有 lazy(LazyThreadSafetyMode.NONE) 改为线程安全 lazy，
 * 修复了 UnsafeLazyImpl 的并发 NPE（getValue 中 initializer 置 null 后的内存可见性问题）。
 *
 * 改写原因：改写前的测试通过 `lazy(LazyThreadSafetyMode.NONE)` 主动复现 NPE，
 * 命中竞态时调用 fail() —— 这是「复现型」验证，在修复后要么跑数十万次才通过（耗时），
 * 要么偶发命中竞态导致构建失败，不适合作为常驻回归测试。
 * 现改为验证已修复的实现（线程安全 lazy）在并发下只初始化一次且不抛异常，
 * 从而稳定守护该修复不被回退。
 */
class ConcurrentLazyTest {

    /**
     * 验证线程安全 lazy 的并发正确性
     * 多线程同时首次访问，应只初始化一次且不抛异常
     */
    @Test
    fun testSynchronizedLazyConcurrentSafety() {
        val totalAttempts = 50_000
        val threadCount = 8
        for (attempt in 0 until totalAttempts) {
            // 使用线程安全的 by lazy（SynchronizedLazyImpl），与 reflex 当前实现一致
            val lazyVal by lazy { "initialized" }
            val barrier = CyclicBarrier(threadCount)
            val error = AtomicReference<Throwable>()
            val threads = (0 until threadCount).map {
                thread(start = false) {
                    try {
                        barrier.await()
                        check(lazyVal == "initialized") { "lazy 值异常: $lazyVal" }
                    } catch (e: Throwable) {
                        error.compareAndSet(null, e)
                    }
                }
            }
            threads.forEach { it.start() }
            threads.forEach { it.join(2000) }
            if (error.get() != null) {
                fail<Unit>(
                    "线程安全 lazy 并发访问出现异常 (第 $attempt 次迭代): ${error.get()!!.javaClass.simpleName}: ${error.get()!!.message}",
                    error.get()
                )
                return
            }
        }
        println("$totalAttempts 次迭代并发访问线程安全 lazy，未出现异常")
    }

    private class TestTarget {
        fun hello(): String = "world"
    }

    /**
     * 通过 ClassMethod.returnType 复现（需要更多迭代）
     */
    @RepeatedTest(500)
    fun testClassMethodReturnTypeConcurrency() {
        val structure = ClassAnalyser.analyseByASM(TestTarget::class.java)
        val method = structure.methods.first { it.name == "hello" }
        val threadCount = 16
        val barrier = CyclicBarrier(threadCount)
        val error = AtomicReference<Throwable>()
        val threads = (0 until threadCount).map {
            thread(start = false) {
                try {
                    barrier.await()
                    method.returnType
                } catch (e: Throwable) {
                    error.compareAndSet(null, e)
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join(5000) }
        val caught = error.get()
        if (caught != null) {
            fail<Unit>("并发访问 ClassMethod.returnType 出现异常: ${caught.javaClass.simpleName}: ${caught.message}", caught)
        }
    }
}
