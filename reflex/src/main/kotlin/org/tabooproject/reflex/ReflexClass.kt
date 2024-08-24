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
        var fixed = name
        if (remap) {
            Reflex.remapper.forEach { fixed = it.field(structure.name ?: return@forEach, fixed) }
        }
        return try {
            structure.getField(fixed)
        } catch (ex: NoSuchFieldException) {
            if (findToParent) {
                superclass?.getField(name, true, remap) ?: throw ex
            } else {
                throw ex
            }
        }
    }

    fun getMethod(name: String, findToParent: Boolean = true, remap: Boolean = true, vararg parameter: Any?): ClassMethod {
        var fixed = name
        if (remap) {
            Reflex.remapper.forEach { fixed = it.method(structure.name ?: return@forEach, fixed, *parameter) }
        }
        return try {
            structure.getMethod(fixed, *parameter)
        } catch (ex: NoSuchMethodException) {
            if (findToParent) {
                try {
                    superclass?.getMethod(name, true, remap, *parameter) ?: throw ex
                } catch (ex: NoSuchMethodException) {
                    interfaces.forEach { runCatching { return it.getMethod(name, true, remap, *parameter) } }
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

        fun of(clazz: Class<*>): ReflexClass {
            return of(clazz, true)
        }

        fun of(clazz: Class<*>, mode: AnalyseMode): ReflexClass {
            return of(clazz, mode, true)
        }

        fun of(clazz: Class<*>, saving: Boolean): ReflexClass {
            return of(clazz, AnalyseMode.REFLECTION_FIRST, saving)
        }

        fun of(clazz: Class<*>, mode: AnalyseMode, saving: Boolean): ReflexClass {
            if (saving && analyseMap.containsKey(clazz.name)) {
                return analyseMap[clazz.name]!!
            }
            return ReflexClass(ClassAnalyser.analyse(clazz, mode)).apply {
                if (saving) {
                    analyseMap[clazz.name] = this
                }
            }
        }
    }
}