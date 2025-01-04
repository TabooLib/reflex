package org.tabooproject.reflex

import org.tabooproject.reflex.serializer.BinaryReader
import org.tabooproject.reflex.serializer.BinaryWriter

/**
 * Reflex
 * org.tabooproject.reflex.JavaClassStructure
 *
 * @author 坏黑
 * @since 2022/1/22 2:53 AM
 */
@Internal
class JavaClassStructure(
    type: Type,
    owner: LazyClass,
    access: Int,
    superclass: LazyClass?,
    interfaces: List<LazyClass>,
    annotations: List<ClassAnnotation>,
    fields: List<ClassField>,
    methods: List<ClassMethod>,
    constructors: List<ClassConstructor>,
) : ClassStructure(type, owner, access, superclass, interfaces, annotations, fields, methods, constructors) {

    override fun getField(name: String): ClassField {
        return fields.find { it.name == name } ?: throw NoSuchFieldException("${this.name}#$name")
    }

    override fun getMethod(name: String, vararg parameter: Any?): ClassMethod {
        return methods.find { it.name == name && Reflection.isAssignableFrom(it.parameterTypes, parameter.map { p -> p?.javaClass }.toTypedArray()) }
            ?: throw NoSuchMethodException("${this.name}#$name(${parameter.joinToString(";") { it?.javaClass?.name ?: "null" }})")
    }

    override fun getMethodByType(name: String, vararg parameter: Class<*>): ClassMethod {
        return methods.find { it.name == name && Reflection.isAssignableFrom(it.parameterTypes, parameter.toList().toTypedArray()) }
            ?: throw NoSuchMethodException("${this.name}#$name(${parameter.joinToString(";") { it.name }})")
    }

    override fun getConstructor(vararg parameter: Any?): ClassConstructor {
        return constructors.find { Reflection.isAssignableFrom(it.parameterTypes, parameter.map { p -> p?.javaClass }.toTypedArray()) }
            ?: throw NoSuchMethodException("${this.name}#<init>(${parameter.joinToString(";") { it?.javaClass?.name ?: "null" }})")
    }

    override fun getConstructorByType(vararg parameter: Class<*>): ClassConstructor {
        return constructors.find { Reflection.isAssignableFrom(it.parameterTypes, parameter.toList().toTypedArray()) }
            ?: throw NoSuchMethodException("${this.name}#<init>(${parameter.joinToString(";") { it.name }})")
    }

    override fun getFieldSilently(name: String): ClassField? {
        return runCatching { getField(name) }.getOrNull()
    }

    override fun getMethodSilently(name: String, vararg parameter: Any?): ClassMethod? {
        return runCatching { getMethod(name, *parameter) }.getOrNull()
    }

    override fun getMethodByTypeSilently(name: String, vararg parameter: Class<*>): ClassMethod? {
        return runCatching { getMethodByType(name, *parameter) }.getOrNull()
    }

    override fun getConstructorSilently(vararg parameter: Any?): ClassConstructor? {
        return runCatching { getConstructor(*parameter) }.getOrNull()
    }

    override fun getConstructorByTypeSilently(vararg parameter: Class<*>): ClassConstructor? {
        return runCatching { getConstructorByType(*parameter) }.getOrNull()
    }

    override fun getAnnotation(annotation: Class<out Annotation>): ClassAnnotation {
        return annotations.first { it.source.name == annotation.name }
    }

    override fun isAnnotationPresent(annotation: Class<out Annotation>): Boolean {
        return annotations.any { it.source.name == annotation.name }
    }

    override fun writeTo(writer: BinaryWriter) {
        // 反射模式不支持序列化
        if (type == Type.REFLECTION) error("Cannot serialize reflection mode")
        writer.writeObj(owner)
        writer.writeInt(access)
        writer.writeNullableObj(superclass)
        // println("write interfaces")
        writer.writeList(interfaces)
        // println("write annotations")
        writer.writeList(annotations)
        // println("write fields")
        writer.writeList(fields)
        // println("write methods")
        writer.writeList(methods)
        // println("write constructors")
        writer.writeList(constructors)
    }

    companion object {

        fun of(reader: BinaryReader, classFinder: ClassAnalyser.ClassFinder?): JavaClassStructure {
            return JavaClassStructure(
                Type.ASM,
                LazyClass.of(reader, classFinder),
                reader.readInt(),
                reader.readNullableClass(classFinder),
                reader.readClassList(classFinder),
                reader.readAnnotationList(classFinder),
                reader.readFieldList(classFinder),
                reader.readMethodList(classFinder),
                reader.readConstructorList(classFinder)
            )
        }
    }
}