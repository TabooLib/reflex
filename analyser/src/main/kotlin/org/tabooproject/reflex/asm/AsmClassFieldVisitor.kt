package org.tabooproject.reflex.asm

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes
import org.tabooproject.reflex.ClassAnalyser
import org.tabooproject.reflex.Internal

/**
 * @author 坏黑
 * @since 2022/1/24 9:11 PM
 */
@Internal
class AsmClassFieldVisitor(fieldVisitor: FieldVisitor, val classFinder: ClassAnalyser.ClassFinder) : FieldVisitor(Opcodes.ASM9, fieldVisitor), Opcodes {

    val annotations = ArrayList<AsmAnnotation>()

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
        return AsmClassAnnotationVisitor(super.visitAnnotation(descriptor, visible), descriptor, classFinder).also {
            annotations += AsmAnnotation(it)
        }
    }
}