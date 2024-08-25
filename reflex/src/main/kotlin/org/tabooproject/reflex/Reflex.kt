package org.tabooproject.reflex

@Suppress("UNCHECKED_CAST")
class Reflex {

    /**
     * 为了兼容性，保持原有的形势
     */
    @Suppress("FunctionName")
    companion object {

        /**
         * 已注册的 ReflexRemapper
         * 直接添加到改容器即可完成注册，起初用于转换 1.17 版本的混淆字段名称
         */
        val remapper = ArrayList<ReflexRemapper>()

        /**
         * 不通过构造函数实例化对象
         */
        fun <T> Class<T>.unsafeInstance(): Any {
            return UnsafeAccess.unsafe.allocateInstance(this)!!
        }

        /**
         * 通过构造方法实例化对象
         */
        fun <T> Class<T>.invokeConstructor(vararg parameter: Any?): T {
            return ReflexClass.of(this).newInstance(*parameter) as T
        }

        /**
         * 通过构造方法实例化对象
         */
        fun <T> Class<T>.invokeConstructor(mode: AnalyseMode, vararg parameter: Any?): T {
            return ReflexClass.of(this, mode).newInstance(*parameter) as T
        }

        /**
         * 执行方法
         * 为 Java 调用提供便利，不查父类，不重映射
         */
        fun <T> Any.invokeLocalMethod(name: String, vararg parameter: Any?): T? {
            return invokeMethod<T>(name, *parameter, false, false, false, AnalyseMode.default)
        }

        /**
         * 执行方法
         * @param name 方法名称
         * @param parameter 方法参数
         * @param isStatic 是否为静态方法
         * @param findToParent 是否查找父类方法
         * @param remap 是否应用重映射
         * @param mode 分析模式
         */
        fun <T> Any.invokeMethod(
            name: String,
            vararg parameter: Any?,
            isStatic: Boolean = false,
            findToParent: Boolean = true,
            remap: Boolean = true,
            mode: AnalyseMode = AnalyseMode.default
        ): T? {
            return if (isStatic && this is Class<*>) {
                ReflexClass.of(this, mode).getMethod(name, findToParent, remap, *parameter).invokeStatic(*parameter) as T?
            } else {
                ReflexClass.of(javaClass, mode).getMethod(name, findToParent, remap, *parameter).invoke(this, *parameter) as T?
            }
        }

        /**
         * 获取字段
         * 为 Java 调用提供便利，不查父类，不重映射
         */
        fun <T> Any.getLocalProperty(name: String): T? {
            return getProperty(name, false, findToParent = false, remap = false, mode = AnalyseMode.default)
        }

        /**
         * 获取字段
         * @param path 字段名称，使用 "/" 符号进行递归获取
         * @param isStatic 是否为静态字段
         * @param findToParent 是否查找父类字段
         * @param remap 是否应用重映射
         * @param mode 分析模式
         */
        fun <T> Any.getProperty(
            path: String,
            isStatic: Boolean = false,
            findToParent: Boolean = true,
            remap: Boolean = true,
            mode: AnalyseMode = AnalyseMode.default
        ): T? {
            return if (path.contains('/')) {
                val left = path.substringBefore('/')
                val right = path.substringAfter('/')
                _get<Any>(left, isStatic, findToParent, remap, mode)?.getProperty(right, isStatic, findToParent, remap, mode)
            } else {
                _get(path, isStatic, findToParent, remap, mode)
            }
        }

        /**
         * 修改字段
         * 为 Java 调用提供便利，不查父类，不重映射
         */
        fun Any.setLocalProperty(name: String, value: Any?) {
            setProperty(name, value, false, findToParent = false, remap = false, mode = AnalyseMode.default)
        }

        /**
         * 修改字段
         * @param path 字段名称，使用 "/" 符号进行递归获取
         * @param value 值
         * @param isStatic 是否为静态字段
         * @param findToParent 是否查找到父类字段
         * @param remap 是否应用重映射
         * @param mode 分析模式
         */
        fun Any.setProperty(
            path: String,
            value: Any?,
            isStatic: Boolean = false,
            findToParent: Boolean = true,
            remap: Boolean = true,
            mode: AnalyseMode = AnalyseMode.default
        ) {
            if (path.contains('/')) {
                val left = path.substringBefore('/')
                val right = path.substringAfter('/')
                _get<Any>(left, isStatic, findToParent, remap, mode)?.setProperty(right, value, isStatic, findToParent, remap, mode)
            } else {
                _set(path, value, isStatic, findToParent, remap, mode)
            }
        }

        private fun <T> Any._get(
            name: String,
            isStatic: Boolean = false,
            findToParent: Boolean = true,
            remap: Boolean = true,
            mode: AnalyseMode = AnalyseMode.default
        ): T? {
            return if (isStatic && this is Class<*>) {
                ReflexClass.of(this, mode).getField(name, findToParent, remap).get() as T?
            } else {
                ReflexClass.of(javaClass, mode).getField(name, findToParent, remap).get(this) as T?
            }
        }

        private fun Any._set(
            name: String,
            value: Any?,
            isStatic: Boolean = false,
            findToParent: Boolean = true,
            remap: Boolean = true,
            mode: AnalyseMode = AnalyseMode.default
        ) {
            if (isStatic && this is Class<*>) {
                ReflexClass.of(this, mode).getField(name, findToParent, remap).setStatic(value)
            } else {
                ReflexClass.of(javaClass, mode).getField(name, findToParent, remap).set(this, value)
            }
        }
    }
}