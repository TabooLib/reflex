package org.tabooproject.reflex

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.tabooproject.reflex.asm.AsmClassVisitor
import org.tabooproject.reflex.reflection.InstantAnnotation
import org.tabooproject.reflex.reflection.InstantClassConstructor
import org.tabooproject.reflex.reflection.InstantClassField
import org.tabooproject.reflex.reflection.InstantClassMethod

/**
 * @author 坏黑
 */
object ClassAnalyser {

    fun analyse(clazz: Class<*>): ClassStructure {
        return try {
            analyseByReflection(clazz)
        } catch (ex: Throwable) {
            when (ex) {
                is NoClassDefFoundError, is ArrayStoreException -> analyseByASM(clazz)
                else -> throw ex
            }
        }
    }

    fun analyseByReflection(clazz: Class<*>): JavaClassStructure {
        val annotations = clazz.declaredAnnotations.map { InstantAnnotation(it) }
        val fields = clazz.declaredFields.map { InstantClassField(clazz, it) }
        val methods = clazz.declaredMethods.map { InstantClassMethod(clazz, it) }
        val constructors = clazz.declaredConstructors.map { InstantClassConstructor(clazz, it) }
        return JavaClassStructure(clazz, annotations, fields, methods, constructors)
    }

    @Suppress("FoldInitializerAndIfToElvis")
    fun analyseByASM(clazz: Class<*>): JavaClassStructure {
        val resourceAsStream = clazz.getResourceAsStream("/${clazz.name.replace('.', '/')}.class")
        if (resourceAsStream == null) {
            // 无法从资源文件中找到对应的类文件，可能来自远程加载
            throw IllegalStateException("Class ${clazz.name} not found (file not in the jar)")
        }
        val classReader = ClassReader(resourceAsStream)
        val analyser = AsmClassVisitor(clazz, ClassWriter(ClassWriter.COMPUTE_MAXS))
        classReader.accept(analyser, ClassReader.SKIP_DEBUG)
        return JavaClassStructure(clazz, analyser.annotations, analyser.fields, analyser.methods, analyser.constructors)
    }
}