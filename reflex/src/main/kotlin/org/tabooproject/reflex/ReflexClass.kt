package org.tabooproject.reflex

import org.tabooproject.reflex.serializer.BinaryReader
import org.tabooproject.reflex.serializer.BinarySerializable
import org.tabooproject.reflex.serializer.BinaryWriter
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * @author 坏黑
 * @since 2022/1/22 2:25 AM
 */
class ReflexClass(val structure: ClassStructure, val mode: AnalyseMode) : BinarySerializable {

    /** 类名 */
    val name = structure.name

    /** 简单类名 */
    val simpleName = structure.simpleName

    /** 父类 */
    val superclass by lazy {
        val superclass = structure.superclass
        if (superclass != null && superclass != Any::class.java) of(superclass.instance ?: superclass.notfound(), mode) else null
    }

    /** 接口 */
    val interfaces by lazy {
        structure.interfaces.map { of(it.instance ?: it.notfound(), mode) }
    }

    /** 单例字段 */
    private val singletonField by lazy { getFieldSilently("INSTANCE", findToParent = false, remap = false) }

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
        return singletonField?.isStatic == true
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
                val parentClass = of(classFinder.findClass(name.substringBeforeLast('$'))!!, mode)
                parentClass.getLocalField("Companion").get()
            }
            // 单例类
            isSingleton() -> singletonField?.get()
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
     * 获取构造器（静默版本）
     * @param parameter 构造器参数
     */
    fun getConstructorSilently(vararg parameter: Any?): ClassConstructor? {
        return structure.getConstructorSilently(*parameter)
    }

    /**
     * 获取构造器（按类型）
     * @param parameter 构造器参数类型
     */
    fun getConstructorByType(vararg parameter: Class<*>): ClassConstructor {
        return structure.getConstructorByType(*parameter)
    }

    /**
     * 获取构造器（按类型，静默版本）
     * @param parameter 构造器参数类型
     */
    fun getConstructorByTypeSilently(vararg parameter: Class<*>): ClassConstructor? {
        return structure.getConstructorByTypeSilently(*parameter)
    }

    /**
     * 获取字段
     * @param name 字段名
     * @param findToParent 是否向上查找父类
     * @param remap 是否应用重映射
     */
    fun getField(name: String, findToParent: Boolean = true, remap: Boolean = true): ClassField {
        return getFieldSilently(name, findToParent, remap)
            ?: throw ExceptionFactory.noSuchField(structure.name, if (remap) remapFieldName(name) else name)
    }

    /**
     * 获取字段
     * 不查父类，不重映射
     */
    fun getLocalField(name: String): ClassField {
        return getField(name, findToParent = false, remap = false)
    }

    /**
     * 获取字段（静默版本）
     * @param name 字段名
     * @param findToParent 是否向上查找父类
     * @param remap 是否应用重映射
     * @return 字段对象，如果不存在则返回 null
     */
    fun getFieldSilently(name: String, findToParent: Boolean = true, remap: Boolean = true): ClassField? {
        val fixed = if (remap) remapFieldName(name) else name
        val field = structure.getFieldSilently(fixed)
        if (field != null) {
            return field
        }
        if (findToParent) {
            return superclass?.getFieldSilently(name, true, remap)
        }
        return null
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
        return getMethodSilently(name, findToParent, remap, *parameter)
            ?: throw ExceptionFactory.noSuchMethod(structure.name, if (remap) remapMethodName(name, *parameter) else name, *parameter)
    }

    /**
     * 获取方法（静默版本）
     * @param name 方法名
     * @param findToParent 是否向上查找父类
     * @param remap 是否应用重映射
     * @param parameter 方法参数
     * @return 方法对象，如果不存在则返回 null
     */
    fun getMethodSilently(name: String, findToParent: Boolean = true, remap: Boolean = true, vararg parameter: Any?): ClassMethod? {
        val fixed = if (remap) remapMethodName(name, *parameter) else name
        val method = structure.getMethodSilently(fixed, *parameter)
        if (method != null) {
            return method
        }
        if (findToParent) {
            val parentMethod = superclass?.getMethodSilently(name, true, remap, *parameter)
            if (parentMethod != null) {
                return parentMethod
            }
            // 在接口中查找
            for (intf in interfaces) {
                val interfaceMethod = intf.getMethodSilently(name, true, remap, *parameter)
                if (interfaceMethod != null) {
                    return interfaceMethod
                }
            }
        }
        return null
    }

    /**
     * 获取方法（按类型）
     * @param name 方法名
     * @param findToParent 是否向上查找父类
     * @param remap 是否应用重映射
     * @param parameter 方法参数类型
     */
    fun getMethodByTypes(name: String, findToParent: Boolean = true, remap: Boolean = true, vararg parameter: Class<*>): ClassMethod {
        return getMethodByTypeSilently(name, findToParent, remap, *parameter)
            ?: throw ExceptionFactory.noSuchMethodByType(structure.name, if (remap) remapMethodNameByTypes(name, *parameter) else name, *parameter)
    }


    /**
     * 获取方法（按类型，静默版本）
     * @param name 方法名
     * @param findToParent 是否向上查找父类
     * @param remap 是否应用重映射
     * @param parameter 方法参数类型
     * @return 方法对象，如果不存在则返回 null
     */
    fun getMethodByTypeSilently(name: String, findToParent: Boolean = true, remap: Boolean = true, vararg parameter: Class<*>): ClassMethod? {
        val fixed = if (remap) remapMethodNameByTypes(name, *parameter) else name
        val method = structure.getMethodByTypeSilently(fixed, *parameter)
        if (method != null) {
            return method
        }
        if (findToParent) {
            val parentMethod = superclass?.getMethodByTypeSilently(name, true, remap, *parameter)
            if (parentMethod != null) {
                return parentMethod
            }
            // 在接口中查找
            for (intf in interfaces) {
                val interfaceMethod = intf.getMethodByTypeSilently(name, true, remap, *parameter)
                if (interfaceMethod != null) {
                    return interfaceMethod
                }
            }
        }
        return null
    }

    /**
     * 获取当前类的 [Class] 对象
     */
    fun toClass(): Class<out Any> {
        return structure.owner.instance ?: error("Class not found: $name")
    }

    /**
     * 获取当前类的 [Class] 对象，可为空
     */
    fun toClassOrNull(): Class<out Any>? {
        return structure.owner.instance
    }

    override fun toString(): String {
        return "ReflexClass $mode($name)"
    }

    override fun writeTo(writer: BinaryWriter) {
        writer.writeObj(structure)
    }

    /**
     * 应用字段重映射
     */
    private fun remapFieldName(name: String): String {
        var fixed = name
        Reflex.remapper.forEach { fixed = it.field(structure.name ?: return@forEach, fixed) }
        return fixed
    }

    /**
     * 应用方法重映射
     */
    private fun remapMethodName(name: String, vararg parameter: Any?): String {
        var fixed = name
        Reflex.remapper.forEach { fixed = it.method(structure.name ?: return@forEach, fixed, *parameter) }
        return fixed
    }

    /**
     * 应用方法重映射（按类型）
     */
    private fun remapMethodNameByTypes(name: String, vararg parameter: Class<*>): String {
        var fixed = name
        Reflex.remapper.forEach { fixed = it.method(structure.name ?: return@forEach, fixed, *parameter) }
        return fixed
    }

    companion object {

        /**
         * 保留公开字段以维持源码与二进制兼容（1.2.x 的调用方可继续引用）。
         *
         * 注意：该表不再作为实际缓存使用 —— 强引用缓存会导致长期运行的宿主
         * （如插件容器反复热重载）中反射结构对象只增不减。
         * 实际缓存由内部 [reflexClassCache] 承担（弱引用 + LRU 强引用保护）。
         * 该字段仅作为兼容占位，读取它可能得到空内容，写入它不会影响缓存行为。
         *
         * 不加 @JvmField：保持 Kotlin 默认生成的 getReflexClassCacheMap() getter，
         * 与 1.2.x 的二进制签名一致（加 @JvmField 会改为直接暴露字段，破坏兼容）。
         */
        val reflexClassCacheMap = ConcurrentHashMap<String, ReflexClass>()

        /**
         * 实际使用的反射类缓存。
         * 弱引用持有：无外部强引用时可被 GC 回收，活跃对象由调用方强引用持有；
         * LRU 强引用保护热点条目，避免热类被反复回收后重新分析（ASM 解析有 CPU 开销）。
         */
        private val reflexClassCache = WeakCache<String, ReflexClass>()

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
            if (!saving) {
                return ReflexClass(ClassAnalyser.analyse(clazz, mode), mode)
            }
            // 命中则直接返回；未命中时把分析动作放进 computeIfAbsent，
            // 使并发场景下同一类只执行一次 ASM/反射分析，避免重复开销
            return reflexClassCache.computeIfAbsent(clazz.name) {
                ReflexClass(ClassAnalyser.analyse(clazz, mode), mode)
            }
        }

        fun of(clazz: LazyClass, inputStream: InputStream): ReflexClass {
            return of(clazz, inputStream, true)
        }

        fun of(clazz: LazyClass, inputStream: InputStream, saving: Boolean): ReflexClass {
            if (!saving) {
                return ReflexClass(ClassAnalyser.analyseByASM(clazz, inputStream), AnalyseMode.ASM_ONLY)
            }
            return reflexClassCache.computeIfAbsent(clazz.name) {
                ReflexClass(ClassAnalyser.analyseByASM(clazz, inputStream), AnalyseMode.ASM_ONLY)
            }
        }

        fun of(reader: BinaryReader, classFinder: ClassAnalyser.ClassFinder? = null): ReflexClass {
            return ReflexClass(JavaClassStructure.of(reader, classFinder), AnalyseMode.ASM_ONLY)
        }

        /**
         * 清空反射类缓存。
         * 供宿主在插件卸载或定期维护时调用，主动释放不再使用的反射结构。
         */
        @JvmStatic
        fun clearReflexClassCache() {
            reflexClassCache.clear()
            reflexClassCacheMap.clear()
        }
    }
}
