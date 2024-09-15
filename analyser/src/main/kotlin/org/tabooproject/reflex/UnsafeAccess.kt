package org.tabooproject.reflex

import sun.misc.Unsafe
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

    fun put(src: Any?, field: Field, value: Any?) {
        val methodHandle = lookup.unreflectSetter(field)
        if (Modifier.isStatic(field.modifiers)) {
            methodHandle.invokeWithArguments(value)
        } else {
            methodHandle.bindTo(src).invokeWithArguments(value)
        }
    }

    fun <T> get(src: Any?, field: Field): T? {
        val methodHandle = lookup.unreflectGetter(field)
        return if (Modifier.isStatic(field.modifiers)) {
            methodHandle.invokeWithArguments()
        } else {
            methodHandle.bindTo(src).invokeWithArguments()
        } as? T
    }
}