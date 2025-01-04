package org.tabooproject.reflex

import java.io.File
import java.net.JarURLConnection
import java.net.URISyntaxException
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
class ReflexJarScanner {

    // @Test
    fun testJar1() {
        val classes = File("common-legacy-api-6.2.2-local-test-1-53cd8f1d.jar").toURI().toURL().getClasses()
        println("共 ${classes.size} 个类")
        val serializeToBytes = ReflexClassMap.serializeToBytes(classes)
        println("序列化 ${serializeToBytes.size} 字节")

        // 反序列化
        ReflexClassMap.deserializeFromBytes(serializeToBytes, null)

        // 写入文件
        val file = File("classes-legacy-api")
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeBytes(serializeToBytes)

        // 从文件里读出来
        val fromBytes = ReflexClassMap.deserializeFromBytes(File("classes-legacy-api").readBytes(), null)
    }

    // @Test
    fun scanner() {
        val classes: Map<String, ReflexClass>
        val time1 = measureTime { classes = File("Adyeshach-2.0.25-api.jar").toURI().toURL().getClasses() }
        println("首次读取 $time1")
        println("共 ${classes.size} 个类")

        val time2 = measureTime { File("Adyeshach-2.0.25-api.jar").toURI().toURL().getClasses() }
        println("预热后读取 $time2")

        val bytes: ByteArray
        val time3 = measureTime { bytes = ReflexClassMap.serializeToBytes(classes) }
        println("序列化 $time3")
        println("序列化 ${bytes.size} 字节")

        val file = File("classes")
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeBytes(bytes)

        val read: Map<String, ReflexClass>
        val time4 = measureTime { read = ReflexClassMap.deserializeFromBytes(file.readBytes(), null) }
        println("反序列化 $time4")
        println("共 ${read.size} 个类")

        val time5 = measureTime { ReflexClassMap.deserializeFromBytes(file.readBytes(), null) }
        println("预热后反序列化 $time5")
    }

    /**
     * 获取 URL 下的所有类
     */
    fun URL.getClasses(): MutableMap<String, ReflexClass> {
        val classes = ConcurrentHashMap<String, ReflexClass>()
        val srcFile = try {
            File(toURI())
        } catch (ex: IllegalArgumentException) {
            File((openConnection() as JarURLConnection).jarFileURL.toURI())
        } catch (ex: URISyntaxException) {
            File(path)
        }
        val classLoader = ReflexJarScanner::class.java.classLoader
        // 是文件
        if (srcFile.isFile) {
            JarFile(srcFile).use { jar ->
                jar.stream()
                    .parallel()
                    .filter { it.name.endsWith(".class") }
                    .forEach {
                        val className = it.name.replace('/', '.').substringBeforeLast('.')
                        val lc = LazyClass.of(className) { Class.forName(className, false, classLoader) }
                        classes[className] = ReflexClass.of(lc, jar.getInputStream(it), saving = false)
                    }
            }
        }
        // 是目录
        else {
            srcFile.walk().filter { it.extension == "class" }.forEach {
                val className = it.path.substringAfter(srcFile.path).drop(1).replace('/', '.').replace('\\', '.').substringBeforeLast(".class")
                val lc = LazyClass.of(className) { Class.forName(className, false, classLoader) }
                classes[className] = ReflexClass.of(lc, it.inputStream())
            }
        }
        return classes
    }
}