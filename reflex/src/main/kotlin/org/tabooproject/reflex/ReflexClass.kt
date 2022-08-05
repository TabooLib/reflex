package org.tabooproject.reflex

import java.util.concurrent.ConcurrentHashMap

/**
 * @author 坏黑
 * @since 2022/1/22 2:25 AM
 */
class ReflexClass(val structure: ClassStructure) {

    val superclass by lazy(LazyThreadSafetyMode.NONE) {
        val clazz = structure.owner.superclass
        if (clazz != Any::class.java && clazz != null) of(clazz) else null
    }

    val interfaces by lazy(LazyThreadSafetyMode.NONE) {
        structure.owner.interfaces.map { of(it) }
    }

    fun getField(name: String, findToParent: Boolean = true, remap: Boolean = true): ClassField {
        var fixname = name
        if (remap) {
            Reflex.remapper.forEach { fixname = it.field(structure.name ?: return@forEach, fixname) }
        }
        return try {
            structure.getField(fixname)
        } catch (ex: NoSuchFieldException) {
            if (findToParent) {
                superclass?.getField(name, true, remap) ?: throw ex
            } else {
                throw ex
            }
        }
    }

    fun getMethod(name: String, findToParent: Boolean = true, remap: Boolean = true, vararg parameter: Any?): ClassMethod {
        var fixname = name
        if (remap) {
            Reflex.remapper.forEach { fixname = it.method(structure.name ?: return@forEach, fixname, *parameter) }
        }
        return try {
            structure.getMethod(fixname, *parameter)
        } catch (ex: NoSuchMethodException) {
            if (findToParent) {
                try {
                    superclass?.getMethod(name, true, remap, *parameter) ?: throw ex
                } catch (ex: NoSuchMethodException) {
                    interfaces.forEach { kotlin.runCatching { return it.getMethod(name, true, remap, *parameter) } }
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