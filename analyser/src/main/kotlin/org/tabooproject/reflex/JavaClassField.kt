package org.tabooproject.reflex

/**
 * @author 坏黑
 * @since 2022/1/21 10:12 PM
 */
@Internal
abstract class JavaClassField(name: String, owner: LazyClass) : ClassField(name, owner) {

    private val handleGetter by lazy(LazyThreadSafetyMode.NONE) {
        val getter = if (isStatic) {
            UnsafeAccess.lookup.findStaticGetter(owner.instance, name, fieldType)
        } else {
            UnsafeAccess.lookup.findGetter(owner.instance, name, fieldType)
        }
        getter.asType(getter.type().generic())
    }

    private val handleSetter by lazy(LazyThreadSafetyMode.NONE) {
        val setter = if (isStatic) {
            UnsafeAccess.lookup.findStaticSetter(owner.instance, name, fieldType)
        } else {
            UnsafeAccess.lookup.findSetter(owner.instance, name, fieldType)
        }
        setter.asType(setter.type().generic())
    }

    override fun get(src: Any?): Any? {
        if (fieldType == Unknown::class.java) {
            throw NoClassDefFoundError("${type.name}.$name (${owner})")
        }
        return try {
            if (isStatic) {
                handleGetter.invoke()
            } else {
                handleGetter.invoke(src)
            }
        } catch (ex: ClassCastException) {
            if (!isStatic && src == StaticSrc) {
                throw IllegalStateException("$name is not a static field", ex)
            }
            throw ex
        } catch (ex: Throwable) {
            if (ex is RuntimeException || ex is Error) {
                throw ex
            }
            throw RuntimeException(ex)
        }
    }

    override fun set(src: Any?, value: Any?) {
        if (fieldType == Unknown::class.java) {
            throw NoClassDefFoundError("${type.name}.$name (${owner})")
        }
        try {
            if (isStatic) {
                handleSetter.invoke(value)
            } else {
                handleSetter.invoke(src, value)
            }
        } catch (ex: ClassCastException) {
            if (!isStatic && src == StaticSrc) {
                throw IllegalStateException("$name is not a static field", ex)
            }
            throw ex
        } catch (ex: Throwable) {
            if (ex is RuntimeException || ex is Error) {
                throw ex
            }
            throw RuntimeException(ex)
        }
    }
}