package org.tabooproject.reflex

import org.tabooproject.reflex.serializer.BinarySerializable
import java.lang.reflect.Modifier
import java.util.*

/**
 * @author 坏黑
 * @since 2022/1/21 6:41 PM
 */
abstract class ClassStructure(
    val type: Type,
    val owner: LazyClass,
    val access: Int,
    val superclass: LazyClass?,
    interfaces: List<LazyClass>,
    annotations: List<ClassAnnotation>,
    fields: List<ClassField>,
    methods: List<ClassMethod>,
    constructors: List<ClassConstructor>,
) : BinarySerializable {

    val name by lazy(LazyThreadSafetyMode.NONE) { runCatching { owner.name }.getOrNull() }
    val simpleName by lazy(LazyThreadSafetyMode.NONE) { runCatching { owner.simpleName }.getOrNull() }

    val interfaces = LinkedList(interfaces)
    val annotations = LinkedList(annotations)
    val fields = LinkedList(fields)
    val methods = LinkedList(methods)
    val constructors = LinkedList(constructors)

    val isStatic: Boolean
        get() = Modifier.isStatic(access)

    val isFinal: Boolean
        get() = Modifier.isFinal(access)

    val isPublic: Boolean
        get() = Modifier.isPublic(access)

    val isProtected: Boolean
        get() = Modifier.isProtected(access)

    val isPrivate: Boolean
        get() = Modifier.isPrivate(access)

    val isAbstract: Boolean
        get() = Modifier.isAbstract(access)

    val isInterface: Boolean
        get() = Modifier.isInterface(access)

    val isStrict: Boolean
        get() = Modifier.isStrict(access)

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
        return "ClassStructure(owner=$owner, superclass=$superclass, interfaces=$interfaces, annotations=$annotations, fields=$fields, methods=$methods, constructors=$constructors)"
    }
}