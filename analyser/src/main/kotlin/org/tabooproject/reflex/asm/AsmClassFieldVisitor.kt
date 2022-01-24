package org.tabooproject.reflex.asm

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.TypePath

/**
 * @author 坏黑
 * @since 2022/1/24 9:11 PM
 */
class AsmClassFieldVisitor(fieldVisitor: FieldVisitor) : FieldVisitor(Opcodes.ASM9, fieldVisitor), Opcodes {

    val annotations = ArrayList<AsmAnnotation>()

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String?, visible: Boolean): AnnotationVisitor {
        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible)
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
        return AsmClassAnnotationVisitor(descriptor, super.visitAnnotation(descriptor, visible)).apply { annotations += toAnnotation() }
    }
}