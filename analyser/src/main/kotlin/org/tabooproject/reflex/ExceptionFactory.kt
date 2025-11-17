package org.tabooproject.reflex

/**
 * 异常工厂
 * 用于创建统一格式的反射异常
 *
 * @author 坏黑
 * @since 2025/11/17
 */
@Internal
object ExceptionFactory {

    /**
     * 创建字段不存在异常
     * @param className 类名
     * @param fieldName 字段名
     */
    fun noSuchField(className: String?, fieldName: String): NoSuchFieldException {
        return NoSuchFieldException("$className#$fieldName")
    }

    /**
     * 创建方法不存在异常
     * @param className 类名
     * @param methodName 方法名
     * @param parameters 方法参数（实例）
     */
    fun noSuchMethod(className: String?, methodName: String, vararg parameters: Any?): NoSuchMethodException {
        val paramStr = parameters.joinToString(";") { it?.javaClass?.name ?: "null" }
        return NoSuchMethodException("$className#$methodName($paramStr)")
    }

    /**
     * 创建方法不存在异常（通过类型）
     * @param className 类名
     * @param methodName 方法名
     * @param parameterTypes 方法参数类型
     */
    fun noSuchMethodByType(className: String?, methodName: String, vararg parameterTypes: Class<*>): NoSuchMethodException {
        val paramStr = parameterTypes.joinToString(";") { it.name }
        return NoSuchMethodException("$className#$methodName($paramStr)")
    }

    /**
     * 创建构造器不存在异常
     * @param className 类名
     * @param parameters 构造器参数（实例）
     */
    fun noSuchConstructor(className: String?, vararg parameters: Any?): NoSuchMethodException {
        val paramStr = parameters.joinToString(";") { it?.javaClass?.name ?: "null" }
        return NoSuchMethodException("$className#<init>($paramStr)")
    }

    /**
     * 创建构造器不存在异常（通过类型）
     * @param className 类名
     * @param parameterTypes 构造器参数类型
     */
    fun noSuchConstructorByType(className: String?, vararg parameterTypes: Class<*>): NoSuchMethodException {
        val paramStr = parameterTypes.joinToString(";") { it.name }
        return NoSuchMethodException("$className#<init>($paramStr)")
    }
}

