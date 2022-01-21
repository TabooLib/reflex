package org.tabooproject.reflex

/**
 * @author 坏黑
 * @since 2022/1/21 6:41 PM
 */
abstract class ClassStructure(val owner: Class<*>, val fields: List<ClassField>, val methods: List<ClassMethod>, val constructors: List<ClassConstructor>) {

    val name: String = owner.name
    val simpleName: String = owner.simpleName

    abstract fun getField(name: String): ClassField

    abstract fun getFieldSilently(name: String): ClassField?

    abstract fun getMethod(name: String, vararg parameter: Any?): ClassMethod

    abstract fun getMethodSilently(name: String, vararg parameter: Any?): ClassMethod?

    abstract fun getMethodByType(name: String, vararg parameter: Class<*>): ClassMethod

    abstract fun getMethodByTypeSilently(name: String, vararg parameter: Class<*>): ClassMethod?

    abstract fun getConstructor(vararg parameter: Any?): ClassConstructor

    abstract fun getConstructorSilently(vararg parameter: Any?): ClassConstructor?

    abstract fun getConstructorByType(vararg parameter: Class<*>): ClassConstructor

    abstract fun getConstructorByTypeSilently(vararg parameter: Class<*>): ClassConstructor?

    override fun toString(): String {
        return "ClassStructure(owner=$owner, fields=$fields, methods=$methods, constructors=$constructors)"
    }
}