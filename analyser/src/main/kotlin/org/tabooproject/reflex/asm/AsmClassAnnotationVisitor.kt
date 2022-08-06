package org.tabooproject.reflex.asm

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Opcodes
import org.tabooproject.reflex.Internal
import org.tabooproject.reflex.LazyEnum

/**
 * @author 坏黑
 * @since 2022/1/24 9:11 PM
 */
@Internal
class AsmClassAnnotationVisitor(val descriptor: String, annotationVisitor: AnnotationVisitor, val fromArray: Boolean = false) :
    AnnotationVisitor(Opcodes.ASM9, annotationVisitor), Opcodes {

    val source = AsmSignature.signatureToClass(descriptor)[0]
    val map = HashMap<String, Any>()
    val array = ArrayList<Any>()

    override fun visit(name: String?, value: Any) {
        if (fromArray) {
            array += value
        } else {
            map[name!!] = value
        }
        super.visit(name, value)
    }

    override fun visitEnum(name: String?, descriptor: String, value: String) {
        name ?: return super.visitEnum(null, descriptor, value)
        map[name] = LazyEnum(AsmSignature.signatureToClass(descriptor)[0], value)
        super.visitEnum(name, descriptor, value)
    }

    override fun visitAnnotation(name: String?, descriptor: String): AnnotationVisitor {
        return AsmClassAnnotationVisitor(descriptor, super.visitAnnotation(name, descriptor)).apply {
            if (name != null) {
                this@AsmClassAnnotationVisitor.map[name] = this
            }
        }

    }

    override fun visitArray(name: String?): AnnotationVisitor {
        return AsmClassAnnotationVisitor(descriptor, super.visitArray(name), true).apply {
            if (name != null) {
                this@AsmClassAnnotationVisitor.map[name] = array
            }
        }
    }

    fun toAnnotation(): AsmAnnotation {
        return AsmAnnotation(this)
    }
}