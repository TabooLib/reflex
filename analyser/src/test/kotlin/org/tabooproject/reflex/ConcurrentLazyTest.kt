package org.tabooproject.reflex

import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * 复现 lazy(LazyThreadSafetyMode.NONE) 的并发 NPE
 *
 * UnsafeLazyImpl 的 getValue 实现：
 * ```
 * if (_value === UNINITIALIZED_VALUE) {
 *     _value = initializer!!()   // ← Thread B 读到 null 的 initializer，NPE
 * }
 * initializer = null             // ← Thread A 执行完这行后
 * ```
 *
 * 多线程同时首次访问时，Thread A 初始化完成后将 initializer 置 null，
 * Thread B 因内存可见性问题仍看到 _value == UNINITIALIZED_VALUE，
 * 调用 initializer!!() 触发 NPE。
 */
class ConcurrentLazyTest {

    /**
     * 直接复现 UnsafeLazyImpl 竞态
     * 通过慢初始化器扩大竞态窗口
     */
    @Test
    fun testUnsafeLazyDirectRace() {
        val totalAttempts = 500_000
        val failCount = AtomicInteger(0)
        val threadCount = 8
        for (attempt in 0 until totalAttempts) {
            // 使用 NONE 模式的 lazy，模拟 ClassMethod.returnType 的实现
            val lazyVal = lazy(LazyThreadSafetyMode.NONE) { "initialized" }
            val barrier = CyclicBarrier(threadCount)
            val error = AtomicReference<Throwable>()
            val threads = (0 until threadCount).map {
                thread(start = false) {
                    try {
                        barrier.await()
                        lazyVal.value
                    } catch (e: Throwable) {
                        error.compareAndSet(null, e)
                    }
                }
            }
            threads.forEach { it.start() }
            threads.forEach { it.join(2000) }
            if (error.get() != null) {
                failCount.incrementAndGet()
                println("第 $attempt 次迭代复现了 NPE: ${error.get()}")
                // 一次就够了
                fail<Unit>(
                    "lazy(LazyThreadSafetyMode.NONE) 并发 NPE 已复现 (第 $attempt 次迭代)",
                    error.get()
                )
                return
            }
        }
        println("$totalAttempts 次迭代未复现（x86 内存模型下竞态窗口极小，但 ARM 或高负载下可触发）")
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
