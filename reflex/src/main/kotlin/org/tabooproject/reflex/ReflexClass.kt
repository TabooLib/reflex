package org.tabooproject.reflex

import java.util.concurrent.ConcurrentHashMap

/**
 * @author 坏黑
 * @since 2022/1/22 2:25 AM
 */
class ReflexClass(val structure: ClassStructure) {

    val superclass by lazy(LazyThreadSafetyMode.NONE) {
        if (structure.owner.superclass != Any::class.java) of(structure.owner.superclass) else null
    }

    val interfaces by lazy(LazyThreadSafetyMode.NONE) {
        structure.owner.interfaces.map { of(it) }
    }

    fun getField(name: String, findToParent: Boolean = true): ClassField {
        var field = name
        Reflex.remapper.forEach {
            field = it.field(structure.name ?: return@forEach, field)
        }
        return try {
            structure.getField(name)
        } catch (ex: NoSuchFieldException) {
            if (findToParent) {
                superclass?.getField(name, true) ?: throw ex
            } else {
                throw ex
            }
        }
    }

    fun getMethod(name: String, findToParent: Boolean = true, vararg parameter: Any?): ClassMethod {
        var field = name
        Reflex.remapper.forEach {
            field = it.method(structure.name ?: return@forEach, field)
        }
        return try {
            structure.getMethod(name, *parameter)
        } catch (ex: NoSuchMethodException) {
            if (findToParent) {
                try {
                    superclass?.getMethod(name, true, *parameter) ?: throw ex
                } catch (ex: NoSuchMethodException) {
                    interfaces.forEach { kotlin.runCatching { return it.getMethod(name, true, *parameter) } }
                    throw ex
                }
            } else {
                throw ex
            }
        }
    }

    fun getConstructor(vararg parameter: Any?): ClassConstructor {
        return structure.getConstructor(*parameter)
    }

    companion object {

        private val analyseMap = ConcurrentHashMap<String, ReflexClass>()

        fun of(clazz: Class<*>, saving: Boolean = true): ReflexClass {
            if (saving && analyseMap.containsKey(clazz.name)) {
                return analyseMap[clazz.name]!!
            }
            return ReflexClass(ClassAnalyser.analyse(clazz)).apply {
                if (saving) {
                    analyseMap[clazz.name] = this
                }
            }
        }
    }
}