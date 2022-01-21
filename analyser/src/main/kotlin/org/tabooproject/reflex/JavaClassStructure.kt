package org.tabooproject.reflex

/**
 * Reflex
 * org.tabooproject.reflex.JavaClassStructure
 *
 * @author 坏黑
 * @since 2022/1/22 2:53 AM
 */
class JavaClassStructure(owner: Class<*>, fields: List<ClassField>, methods: List<ClassMethod>, constructors: List<ClassConstructor>) :
    ClassStructure(owner, fields, methods, constructors) {

    override fun getField(name: String): ClassField {
        return fields.firstOrNull { it.name == name } ?: throw NoSuchFieldException(name)
    }

    override fun getMethod(name: String, vararg parameter: Any?): ClassMethod {
        return methods.firstOrNull { it.name == name && Reflection.isAssignableFrom(it.parameterTypes, parameter.map { p -> p?.javaClass }.toTypedArray()) }
            ?: throw NoSuchMethodException("$name(${parameter.joinToString(";") { it?.javaClass?.name ?: "null" }})")
    }

    override fun getMethodByType(name: String, vararg parameter: Class<*>): ClassMethod {
        return methods.firstOrNull { it.name == name && Reflection.isAssignableFrom(it.parameterTypes, parameter.toList().toTypedArray()) }
            ?: throw NoSuchMethodException("$name(${parameter.joinToString(";") { it.name }})")
    }

    override fun getConstructor(vararg parameter: Any?): ClassConstructor {
        return constructors.firstOrNull { Reflection.isAssignableFrom(it.parameterTypes, parameter.map { p -> p?.javaClass }.toTypedArray()) }
            ?: throw NoSuchMethodException("<init>(${parameter.joinToString(";") { it?.javaClass?.name ?: "null" }})")
    }

    override fun getConstructorByType(vararg parameter: Class<*>): ClassConstructor {
        return constructors.firstOrNull { Reflection.isAssignableFrom(it.parameterTypes, parameter.toList().toTypedArray()) }
            ?: throw NoSuchMethodException("<init>(${parameter.joinToString(";") { it.name }})")
    }

    override fun getFieldSilently(name: String): ClassField? {
        return kotlin.runCatching { getField(name) }.getOrNull()
    }

    override fun getMethodSilently(name: String, vararg parameter: Any?): ClassMethod? {
        return kotlin.runCatching { getMethod(name, *parameter) }.getOrNull()
    }

    override fun getMethodByTypeSilently(name: String, vararg parameter: Class<*>): ClassMethod? {
        return kotlin.runCatching { getMethodByType(name, *parameter) }.getOrNull()
    }

    override fun getConstructorSilently(vararg parameter: Any?): ClassConstructor? {
        return kotlin.runCatching { getConstructor(*parameter) }.getOrNull()
    }

    override fun getConstructorByTypeSilently(vararg parameter: Class<*>): ClassConstructor? {
        return kotlin.runCatching { getConstructorByType(*parameter) }.getOrNull()
    }
}