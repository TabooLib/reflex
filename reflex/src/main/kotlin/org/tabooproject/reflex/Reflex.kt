package org.tabooproject.reflex

@Suppress("UNCHECKED_CAST")
class Reflex {

    /**
     * 为了兼容性，保持原有的形势
     */
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
            return ReflexClass.of(this).getConstructor(*parameter).instance(*parameter) as T
        }

        /**
         * 执行方法
         * @param name 方法名称
         * @param parameter 方法参数
         * @param isStatic 是否为静态方法
         * @param findToParent 是否查找父类方法
         */
        fun <T> Any.invokeMethod(name: String, vararg parameter: Any?, isStatic: Boolean = false, findToParent: Boolean = true, remap: Boolean = true): T? {
            return if (isStatic && this is Class<*>) {
                ReflexClass.of(this).getMethod(name, findToParent, remap, *parameter).invokeStatic(*parameter) as T?
            } else {
                ReflexClass.of(javaClass).getMethod(name, findToParent, remap, *parameter).invoke(this, *parameter) as T?
            }
        }

        /**
         * 获取字段
         * @param path 字段名称，使用 "/" 符号进行递归获取
         * @param isStatic 是否为静态字段
         * @param findToParent 是否查找父类字段
         */
        fun <T> Any.getProperty(path: String, isStatic: Boolean = false, findToParent: Boolean = true, remap: Boolean = true): T? {
            return if (path.contains('/')) {
                val left = path.substringBefore('/')
                val right = path.substringAfter('/')
                getLocalProperty<Any>(left, isStatic, findToParent, remap)?.getProperty(right, isStatic, findToParent, remap)
            } else {
                getLocalProperty(path, isStatic, findToParent, remap)
            }
        }

        /**
         * 修改字段
         * @param path 字段名称，使用 "/" 符号进行递归获取
         * @param value 值
         * @param isStatic 是否为静态字段
         * @param findToParent 是否查找到父类字段
         */
        fun Any.setProperty(path: String, value: Any?, isStatic: Boolean = false, findToParent: Boolean = true, remap: Boolean = true) {
            if (path.contains('/')) {
                val left = path.substringBefore('/')
                val right = path.substringAfter('/')
                getLocalProperty<Any>(left, isStatic, findToParent, remap)?.setProperty(right, value, isStatic, findToParent, remap)
            } else {
                setLocalProperty(path, value, isStatic, findToParent, remap)
            }
        }

        private fun <T> Any.getLocalProperty(name: String, isStatic: Boolean = false, findToParent: Boolean = true, remap: Boolean = true): T? {
            return if (isStatic && this is Class<*>) {
                ReflexClass.of(this).getField(name, findToParent, remap).get() as T?
            } else {
                ReflexClass.of(javaClass).getField(name, findToParent, remap).get(this) as T?
            }
        }

        private fun Any.setLocalProperty(name: String, value: Any?, isStatic: Boolean = false, findToParent: Boolean = true, remap: Boolean = true) {
            if (isStatic && this is Class<*>) {
                ReflexClass.of(this).getField(name, findToParent, remap).setStatic(value)
            } else {
                ReflexClass.of(javaClass).getField(name, findToParent, remap).set(this, value)
            }
        }
    }
}