package org.tabooproject.reflex.asm

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Opcodes
import org.tabooproject.reflex.ClassAnalyser
import org.tabooproject.reflex.Internal
import org.tabooproject.reflex.LazyEnum

/**
 * @author 坏黑
 * @since 2022/1/24 9:11 PM
 */
@Internal
class AsmClassAnnotationVisitor(
    annotationVisitor: AnnotationVisitor,
    val descriptor: String,
    val classFinder: ClassAnalyser.ClassFinder,
    val context: Context? = null
) : AnnotationVisitor(Opcodes.ASM9, annotationVisitor), Opcodes {

    val source = AsmSignature.signatureToClass(descriptor, classFinder).first()
    val map = HashMap<String, Any>()
    val array = ArrayList<Any>()

    override fun visit(name: String?, value: Any) {
        super.visit(name, value)
        if (name == null) {
            array += value
        } else {
            map[name] = value
        }
    }

    override fun visitEnum(name: String?, descriptor: String, value: String) {
        super.visitEnum(name, descriptor, value)
        val enum = LazyEnum(AsmSignature.signatureToClass(descriptor, classFinder).first(), value)
        if (name == null) {
            array += enum
        } else {
            map[name] = enum
        }
    }

    override fun visitAnnotation(name: String?, descriptor: String): AnnotationVisitor {
        return AsmClassAnnotationVisitor(super.visitAnnotation(name, descriptor), descriptor, classFinder, Context(name, this))
    }

    override fun visitArray(name: String?): AnnotationVisitor {
        return AsmClassAnnotationVisitor(super.visitArray(name), descriptor, classFinder, Context(name, this))
    }

    override fun visitEnd() {
        super.visitEnd()
        if (context != null) {
            // 取二者之一不为空的值
            val value = array.ifEmpty { map.ifEmpty { null } } ?: return
            // 有名字则为对象，反之为数组
            if (context.name != null) {
                context.visitor.map[context.name] = value
            } else {
                context.visitor.array += value
            }
        }
    }

    class Context(val name: String?, val visitor: AsmClassAnnotationVisitor)
}