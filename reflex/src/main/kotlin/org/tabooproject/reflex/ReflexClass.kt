package org.tabooproject.reflex

import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * @author 坏黑
 * @since 2022/1/22 2:25 AM
 */
class ReflexClass(val structure: ClassStructure, val mode: AnalyseMode) {

    /** 类名 */
    val name = structure.name

    /** 简单类名 */
    val simpleName = structure.simpleName

    /** 父类 */
    val superclass by lazy(LazyThreadSafetyMode.NONE) {
        val superclass = structure.superclass
        if (superclass != null && superclass != Any::class.java) of(superclass.instance ?: superclass.notfound(), mode) else null
    }

    /** 接口 */
    val interfaces by lazy(LazyThreadSafetyMode.NONE) {
        structure.interfaces.map { of(it.instance ?: it.notfound(), mode) }
    }

    /** 单例字段 */
    private val singletonField by lazy(LazyThreadSafetyMode.NONE) { getLocalField("INSTANCE") }

    /** 单例实例 */
    private var singletonInstance: Any? = null

    /** 单例实例是否已被初始化 */
    private var isSingletonInstanceInitialized = false

    /**
     * 是否为伴生类
     * 并非基于 Kotlin API，因此此方法不支持识别改过名字的伴生类
     * ```
     * companion object CustomCompanionName // 不受支持
     * ```
     */
    fun isCompanion(): Boolean {
        return simpleName?.endsWith("\$Companion") == true
    }

    /**
     * 是否为单例类
     * 通过检查是否存在 `INSTANCE` 字段来判断
     */
    fun isSingleton(): Boolean {
        return try {
            singletonField.isStatic
        } catch (ex: NoSuchFieldException) {
            false
        }
    }

    /**
     * 获取实例
     */
    fun getInstance(): Any? {
        return getInstance { Class.forName(it) }
    }

    /**
     * 获取实例
     * 伴生类或单例类则返回对应的实例，如果不是则返回 null
     */
    @Synchronized
    fun getInstance(classFinder: ClassAnalyser.ClassFinder): Any? {
        if (isSingletonInstanceInitialized) return singletonInstance
        isSingletonInstanceInitialized = true
        singletonInstance = when {
            // 伴生类
            isCompanion() -> {
                name ?: error("Unknown class name.")
                val parentClass = of(classFinder.findClass(name.substringBeforeLast('$')), mode)
                parentClass.getLocalField("Companion").get()
            }
            // 单例类
            isSingleton() -> singletonField.get()
            // 其他
            else -> null
        }
        return singletonInstance
    }

    /**
     * 创建实例
     */
    fun newInstance(vararg parameter: Any?): Any? {
        // 经过测试，对于无参构造器，Java Reflect 速度更快
        return if (parameter.isEmpty()) {
            structure.owner.instance?.getDeclaredConstructor()?.newInstance()
        } else {
            getConstructor(*parameter).instance(*parameter)
        }
    }

    /**
     * 是否实现了接口（在本类中）
     */
    fun hasInterface(ic: Class<*>): Boolean {
        val name = ic.name
        return structure.interfaces.any { it.name == name }
    }

    /**
     * 注解是否存在
     */
    fun hasAnnotation(annotation: Class<out Annotation>): Boolean {
        return structure.isAnnotationPresent(annotation)
    }

    /**
     * 获取注解信息
     * 如果不存在则抛出异常
     */
    fun getAnnotation(annotation: Class<out Annotation>): ClassAnnotation {
        return structure.getAnnotation(annotation)
    }

    /**
     * 获取注解信息
     * 如果不存在则返回 null
     */
    fun getAnnotationIfPresent(annotation: Class<out Annotation>): ClassAnnotation? {
        return if (hasAnnotation(annotation)) getAnnotation(annotation) else null
    }

    /**
     * 获取构造器
     * @param parameter 构造器参数
     */
    fun getConstructor(vararg parameter: Any?): ClassConstructor {
        return structure.getConstructor(*parameter)
    }

    /**
     * 获取字段
     * 不查父类，不重映射
     */
    fun getLocalField(name: String): ClassField {
        return getField(name, findToParent = false, remap = false)
    }

    /**
     * 获取字段
     * @param name 字段名
     * @param findToParent 是否向上查找父类
     * @param remap 是否应用重映射
     */
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

    /**
     * 获取方法
     * 不查父类，不重映射
     */
    fun getLocalMethod(name: String, vararg parameter: Any?): ClassMethod {
        return getMethod(name, findToParent = false, remap = false, *parameter)
    }

    /**
     * 获取方法
     * @param name 方法名
     * @param findToParent 是否向上查找父类
     * @param remap 是否应用重映射
     * @param parameter 方法参数
     */
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

    override fun toString(): String {
        return "ReflexClass $mode($name)"
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
            return of(clazz, AnalyseMode.default, saving)
        }

        fun of(clazz: Class<*>, mode: AnalyseMode, saving: Boolean): ReflexClass {
            if (saving && analyseMap.containsKey(clazz.name)) {
                return analyseMap[clazz.name]!!
            }
            return ReflexClass(ClassAnalyser.analyse(clazz, mode), mode).also {
                if (saving) {
                    analyseMap[clazz.name] = it
                }
            }
        }

        fun of(clazz: LazyClass, inputStream: InputStream): ReflexClass {
            return of(clazz, inputStream, true)
        }

        fun of(clazz: LazyClass, inputStream: InputStream, saving: Boolean): ReflexClass {
            if (saving && analyseMap.containsKey(clazz.name)) {
                return analyseMap[clazz.name]!!
            }
            return ReflexClass(ClassAnalyser.analyseByASM(clazz, inputStream), AnalyseMode.ASM_ONLY).also {
                if (saving) {
                    analyseMap[clazz.name] = it
                }
            }
        }
    }
}