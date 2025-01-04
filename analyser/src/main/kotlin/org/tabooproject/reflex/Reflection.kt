package org.tabooproject.reflex

import org.apache.commons.lang3.JavaVersion
import org.apache.commons.lang3.SystemUtils

/**
 * @author 坏黑
 * @since 2022/1/21 11:19 PM
 */
object Reflection {

    val autoboxing = runCatching { SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_5) }.getOrElse { true }

    fun isAssignableFrom(left: Array<Class<*>>, right: Array<Class<*>?>): Boolean {
        if (left.size != right.size) {
            return false
        }
        return left.indices.all { right[it] == null || getReferenceType(left[it]).isAssignableFrom(getReferenceType(right[it]!!)) }
    }

    fun getPrimitiveType(descriptor: Char): Class<*> {
        return when (descriptor) {
            'B' -> java.lang.Byte.TYPE
            'C' -> Character.TYPE
            'D' -> java.lang.Double.TYPE
            'F' -> java.lang.Float.TYPE
            'I' -> Integer.TYPE
            'J' -> java.lang.Long.TYPE
            'S' -> java.lang.Short.TYPE
            'V' -> Void.TYPE
            'Z' -> java.lang.Boolean.TYPE
            else -> throw IllegalArgumentException()
        }
    }

    fun getReferenceType(primitive: Class<*>): Class<*> {
        return when (primitive) {
            Integer.TYPE -> Integer::class.java
            Character.TYPE -> Character::class.java
            java.lang.Byte.TYPE -> java.lang.Byte::class.java
            java.lang.Long.TYPE -> java.lang.Long::class.java
            java.lang.Double.TYPE -> java.lang.Double::class.java
            java.lang.Float.TYPE -> java.lang.Float::class.java
            java.lang.Short.TYPE -> java.lang.Short::class.java
            java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
            else -> primitive
        }
    }
}

fun Class<*>.getArrayDimensions(): Int {
    var dimensions = 0
    var currentClass: Class<*>? = this
    // 通过检查类名来计算维度
    while (currentClass?.isArray == true) {
        dimensions++
        currentClass = currentClass.componentType
    }
    return dimensions
}