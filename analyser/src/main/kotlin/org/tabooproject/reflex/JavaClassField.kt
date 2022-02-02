package org.tabooproject.reflex

import java.lang.invoke.MethodHandle

/**
 * @author 坏黑
 * @since 2022/1/21 10:12 PM
 */
@Internal
abstract class JavaClassField(name: String, owner: Class<*>) : ClassField(name, owner) {

    private val handleGetter: MethodHandle by lazy {
        if (isStatic) {
            UnsafeAccess.lookup.findStaticGetter(owner, name, fieldType)
        } else {
            UnsafeAccess.lookup.findGetter(owner, name, fieldType)
        }
    }

    private val handleSetter: MethodHandle by lazy {
        if (isStatic) {
            UnsafeAccess.lookup.findStaticSetter(owner, name, fieldType)
        } else {
            UnsafeAccess.lookup.findSetter(owner, name, fieldType)
        }
    }

    override fun get(src: Any?): Any? {
        if (fieldType == Unknown::class.java) {
            throw NoClassDefFoundError(type.name)
        }
        return if (isStatic) {
            handleGetter.invokeWithArguments()
        } else {
            try {
                handleGetter.bindTo(src).invokeWithArguments()
            } catch (ex: ClassCastException) {
                if (src == StaticSrc) {
                    throw IllegalStateException("$name is not a static field", ex)
                } else {
                    throw IllegalStateException("${src?.javaClass?.name} is not an instance of ${owner.name}", ex)
                }
            }
        }
    }

    override fun set(src: Any?, value: Any?) {
        if (fieldType == Unknown::class.java) {
            throw NoClassDefFoundError(type.name)
        }
        if (isStatic) {
            handleSetter.invokeWithArguments(value)
        } else {
            try {
                handleSetter.bindTo(src).invokeWithArguments(value)
            } catch (ex: ClassCastException) {
                if (src == StaticSrc) {
                    throw IllegalStateException("$name is not a static field", ex)
                } else {
                    throw IllegalStateException("${src?.javaClass?.name} is not an instance of ${owner.name}", ex)
                }
            }
        }
    }
}