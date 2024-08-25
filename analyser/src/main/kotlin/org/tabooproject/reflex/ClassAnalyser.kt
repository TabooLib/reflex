package org.tabooproject.reflex

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.tabooproject.reflex.asm.AsmClassVisitor
import org.tabooproject.reflex.reflection.InstantAnnotation
import org.tabooproject.reflex.reflection.InstantClassConstructor
import org.tabooproject.reflex.reflection.InstantClassField
import org.tabooproject.reflex.reflection.InstantClassMethod
import java.io.InputStream

/**
 * @author 坏黑
 */
object ClassAnalyser {

    fun analyse(clazz: Class<*>): ClassStructure {
        return analyse(clazz, AnalyseMode.default)
    }

    fun analyse(clazz: Class<*>, mode: AnalyseMode): ClassStructure {
        return when (mode) {
            // 反射优先
            AnalyseMode.REFLECTION_FIRST -> {
                return try {
                    analyseByReflection(clazz)
                } catch (ex: Throwable) {
                    when (ex) {
                        is NoClassDefFoundError, is ArrayStoreException -> analyseByASM(clazz)
                        else -> throw ex
                    }
                }
            }
            // 仅反射
            AnalyseMode.REFLECTION_ONLY -> analyseByReflection(clazz)
            // ASM 优先
            AnalyseMode.ASM_FIRST -> {
                return try {
                    analyseByASM(clazz)
                } catch (ex: Throwable) {
                    when (ex) {
                        is ClassNotFoundException -> analyseByReflection(clazz)
                        else -> throw ex
                    }
                }
            }
            // 仅 ASM
            AnalyseMode.ASM_ONLY -> analyseByASM(clazz)
        }
    }

    fun analyseByReflection(clazz: Class<*>): JavaClassStructure {
        val lc = LazyClass.of(clazz)
        val superclass = clazz.superclass?.let { LazyClass.of(it) }
        val interfaces = clazz.interfaces.map { LazyClass.of(it) }
        val annotations = clazz.declaredAnnotations.map { InstantAnnotation(it) }
        val fields = clazz.declaredFields.map { InstantClassField(lc, it) }
        val methods = clazz.declaredMethods.map { InstantClassMethod(lc, it) }
        val constructors = clazz.declaredConstructors.map { InstantClassConstructor(lc, it) }
        return JavaClassStructure(Type.REFLECTION, lc, clazz.modifiers, superclass, interfaces, annotations, fields, methods, constructors)
    }

    fun analyseByASM(clazz: Class<*>): JavaClassStructure {
        return analyseByASM(clazz) { Class.forName(it) }
    }

    @Suppress("FoldInitializerAndIfToElvis")
    fun analyseByASM(clazz: Class<*>, classFinder: ClassFinder): JavaClassStructure {
        val resourceAsStream = clazz.getResourceAsStream("/${clazz.name.replace('.', '/')}.class")
        if (resourceAsStream == null) {
            // 无法从资源文件中找到对应的类文件，可能来自远程加载
            throw ClassNotFoundException("Class ${clazz.name} not found (file not in the jar)")
        }
        return analyseByASM(LazyClass.of(clazz), resourceAsStream, classFinder)
    }

    fun analyseByASM(clazz: LazyClass, inputStream: InputStream): JavaClassStructure {
        return analyseByASM(clazz, inputStream) {
            try {
                Class.forName(it)
            } catch (ex: ClassNotFoundException) {
                throw ClassNotFoundException("Class \"$it\" not found, classloader ${javaClass.classLoader}")
            }
        }
    }

    fun analyseByASM(clazz: LazyClass, inputStream: InputStream, classFinder: ClassFinder): JavaClassStructure {
        val classReader = ClassReader(inputStream)
        val analyser = AsmClassVisitor(clazz, classFinder, ClassWriter(ClassWriter.COMPUTE_MAXS))
        classReader.accept(analyser, ClassReader.SKIP_DEBUG)
        return JavaClassStructure(
            Type.ASM, clazz, analyser.access, analyser.superclass, analyser.interfaces, analyser.annotations, analyser.fields, analyser.methods, analyser.constructors
        )
    }

    fun interface ClassFinder {

        fun findClass(name: String): Class<*>
    }
}