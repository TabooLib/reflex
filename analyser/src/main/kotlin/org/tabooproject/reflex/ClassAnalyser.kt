package org.tabooproject.reflex

import org.objectweb.asm.*
import org.tabooproject.reflex.asm.AsmClassVisitor
import org.tabooproject.reflex.reflection.InstantClassConstructor
import org.tabooproject.reflex.reflection.InstantClassField
import org.tabooproject.reflex.reflection.InstantClassMethod
import java.util.*

/**
 * @author 坏黑
 */
object ClassAnalyser {

    fun analyse(clazz: Class<*>, excludeModifiers: Int = 0): ClassStructure {
        return try {
            val fields = clazz.declaredFields.map { InstantClassField(clazz, it) }
            val methods = clazz.declaredMethods.map { InstantClassMethod(clazz, it) }
            val constructors = clazz.declaredConstructors.map { InstantClassConstructor(clazz, it) }
            JavaClassStructure(clazz, fields, methods, constructors)
        } catch (ex: NoClassDefFoundError) {
            val classReader = ClassReader(Objects.requireNonNull(clazz.getResourceAsStream("/${clazz.name.replace('.', '/')}.class")))
            val analyser = AsmClassVisitor(clazz, ClassWriter(ClassWriter.COMPUTE_MAXS), excludeModifiers)
            classReader.accept(analyser, ClassReader.SKIP_DEBUG)
            JavaClassStructure(clazz, analyser.fields, analyser.methods, analyser.constructors)
        }
    }
}