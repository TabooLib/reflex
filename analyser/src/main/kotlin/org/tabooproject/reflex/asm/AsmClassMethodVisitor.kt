package org.tabooproject.reflex.asm

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.tabooproject.reflex.ClassAnalyser
import org.tabooproject.reflex.Internal

/**
 * @author 坏黑
 * @since 2022/1/24 9:11 PM
 */
@Internal
class AsmClassMethodVisitor(methodVisitor: MethodVisitor, val classFinder: ClassAnalyser.ClassFinder) : MethodVisitor(Opcodes.ASM9, methodVisitor), Opcodes {

    val annotations = ArrayList<AsmAnnotation>()
    val parameterAnnotations = LinkedHashMap<Int, ArrayList<AsmAnnotation>>()

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
        return AsmClassAnnotationVisitor(super.visitAnnotation(descriptor, visible), descriptor, classFinder).also {
            annotations += AsmAnnotation(it)
        }
    }

    override fun visitParameterAnnotation(parameter: Int, descriptor: String, visible: Boolean): AnnotationVisitor {
        return AsmClassAnnotationVisitor(super.visitParameterAnnotation(parameter, descriptor, visible), descriptor, classFinder).also {
            parameterAnnotations.getOrPut(parameter) { ArrayList() } += AsmAnnotation(it)
        }
    }
}