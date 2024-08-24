package org.tabooproject.reflex

/**
 * @author 坏黑
 * @since 2022/1/21 10:12 PM
 */
@Internal
abstract class JavaClassField(name: String, owner: LazyClass) : ClassField(name, owner) {

    private val handleGetter by lazy(LazyThreadSafetyMode.NONE) {
        if (isStatic) {
            UnsafeAccess.lookup.findStaticGetter(owner.instance, name, fieldType)
        } else {
            UnsafeAccess.lookup.findGetter(owner.instance, name, fieldType)
        }
    }

    private val handleSetter by lazy(LazyThreadSafetyMode.NONE) {
        if (isStatic) {
            UnsafeAccess.lookup.findStaticSetter(owner.instance, name, fieldType)
        } else {
            UnsafeAccess.lookup.findSetter(owner.instance, name, fieldType)
        }
    }

    override fun get(src: Any?): Any? {
        if (fieldType == Unknown::class.java) {
            throw NoClassDefFoundError("${type.name}.$name (${owner})")
        }
        return if (isStatic) {
            handleGetter.invokeWithArguments()
        } else {
            try {
                handleGetter.bindTo(src).invokeWithArguments()
            } catch (ex: ClassCastException) {
                if (src == StaticSrc) {
                    throw IllegalStateException("$name is not a static field", ex)
                }
                throw ex
            }
        }
    }

    override fun set(src: Any?, value: Any?) {
        if (fieldType == Unknown::class.java) {
            throw NoClassDefFoundError("${type.name}.$name (${owner})")
        }
        if (isStatic) {
            handleSetter.invokeWithArguments(value)
        } else {
            try {
                handleSetter.bindTo(src).invokeWithArguments(value)
            } catch (ex: ClassCastException) {
                if (src == StaticSrc) {
                    throw IllegalStateException("$name is not a static field", ex)
                }
                throw ex
            }
        }
    }
}