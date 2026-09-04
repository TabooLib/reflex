package org.tabooproject.reflex

import sun.misc.Unsafe
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Field
import java.lang.reflect.Modifier

@Suppress("UNCHECKED_CAST")
object UnsafeAccess {

    var unsafe: Unsafe
        private set

    var lookup: MethodHandles.Lookup
        private set

    init {
        try {
            val theUnsafe = Unsafe::class.java.getDeclaredField("theUnsafe")
            theUnsafe.isAccessible = true
            unsafe = theUnsafe.get(null) as Unsafe
            try {
                Unsafe::class.java.getDeclaredMethod("ensureClassInitialized", Class::class.java).invoke(unsafe, MethodHandles.Lookup::class.java)
            } catch (ignored: Throwable) {
                // Fix JDK22 compatibility
                // MethodHandles.lookup().ensureInitialized(MethodHandles.Lookup::class.java)
                MethodHandles.Lookup::class.java.getDeclaredMethod("ensureInitialized", Class::class.java).invoke(MethodHandles.lookup(), MethodHandles.Lookup::class.java)
            }
            val lookupField = MethodHandles.Lookup::class.java.getDeclaredField("IMPL_LOOKUP")
            val lookupBase = unsafe.staticFieldBase(lookupField)
            val lookupOffset = unsafe.staticFieldOffset(lookupField)
            lookup = unsafe.getObject(lookupBase, lookupOffset) as MethodHandles.Lookup
        } catch (t: Throwable) {
            throw IllegalStateException("Unsafe not found", t)
        }
    }

    /**
     * Field -> MethodHandle 的弱引用缓存。
     * Field 无外部强引用时其条目可被 GC 回收，避免长期持有已卸载类的元数据；
     * LRU 强引用保护热点字段，避免反复 unreflect（每次 unreflect 都有访问检查与对象分配开销）。
     */
    private val getterCache = WeakCache<Field, MethodHandle>()
    private val setterCache = WeakCache<Field, MethodHandle>()

    fun put(src: Any?, field: Field, value: Any?) {
        // 缓存 MethodHandle：避免每次调用都 unreflect（含访问检查与对象分配）
        val methodHandle = setterCache.computeIfAbsent(field) { lookup.unreflectSetter(field) }
        if (Modifier.isStatic(field.modifiers)) {
            methodHandle.invoke(value)
        } else {
            // 直接传入接收者，避免每次调用 bindTo 或 invokeWithArguments 的参数打包开销
            methodHandle.invoke(src, value)
        }
    }

    fun <T> get(src: Any?, field: Field): T? {
        val methodHandle = getterCache.computeIfAbsent(field) { lookup.unreflectGetter(field) }
        return if (Modifier.isStatic(field.modifiers)) {
            methodHandle.invoke() as? T
        } else {
            methodHandle.invoke(src) as? T
        }
    }
}