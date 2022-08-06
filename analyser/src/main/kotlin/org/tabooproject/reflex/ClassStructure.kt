package org.tabooproject.reflex

import java.util.LinkedList

/**
 * @author 坏黑
 * @since 2022/1/21 6:41 PM
 */
abstract class ClassStructure(
    val owner: Class<*>,
    annotations: List<ClassAnnotation>,
    fields: List<ClassField>,
    methods: List<ClassMethod>,
    constructors: List<ClassConstructor>,
) {

    val annotations = LinkedList(annotations)

    val fields = LinkedList(fields)

    val methods = LinkedList(methods)

    val constructors = LinkedList(constructors)

    val name by lazy(LazyThreadSafetyMode.NONE) {
        kotlin.runCatching { owner.name }.getOrNull()
    }

    val simpleName by lazy(LazyThreadSafetyMode.NONE) {
        kotlin.runCatching { owner.simpleName }.getOrNull()
    }

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

    abstract fun getAnnotation(annotation: Class<out Annotation>): ClassAnnotation

    abstract fun isAnnotationPresent(annotation: Class<out Annotation>): Boolean

    override fun toString(): String {
        return "ClassStructure(owner=$owner, fields=$fields, methods=$methods, constructors=$constructors)"
    }
}