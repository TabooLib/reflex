package org.tabooproject.reflex

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.tabooproject.reflex.asm.AsmClassVisitor
import org.tabooproject.reflex.reflection.InstantAnnotation
import org.tabooproject.reflex.reflection.InstantClassConstructor
import org.tabooproject.reflex.reflection.InstantClassField
import org.tabooproject.reflex.reflection.InstantClassMethod
import java.util.*

/**
 * @author 坏黑
 */
object ClassAnalyser {

    fun analyse(clazz: Class<*>): ClassStructure {
        return try {
            val annotations = clazz.declaredAnnotations.map { InstantAnnotation(it) }
            val fields = clazz.declaredFields.map { InstantClassField(clazz, it) }
            val methods = clazz.declaredMethods.map { InstantClassMethod(clazz, it) }
            val constructors = clazz.declaredConstructors.map { InstantClassConstructor(clazz, it) }
            JavaClassStructure(clazz, annotations, fields, methods, constructors)
        } catch (ex: NoClassDefFoundError) {
            val classReader = ClassReader(Objects.requireNonNull(clazz.getResourceAsStream("/${clazz.name.replace('.', '/')}.class")))
            val analyser = AsmClassVisitor(clazz, ClassWriter(ClassWriter.COMPUTE_MAXS))
            classReader.accept(analyser, ClassReader.SKIP_DEBUG)
            JavaClassStructure(clazz, analyser.annotations, analyser.fields, analyser.methods, analyser.constructors)
        }
    }
}