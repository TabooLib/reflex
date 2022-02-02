package org.tabooproject.reflex.asm

import org.objectweb.asm.*
import org.tabooproject.reflex.ClassAnnotation
import org.tabooproject.reflex.Internal

/**
 * @author izzel
 */
@Internal
class AsmClassVisitor(val owner: Class<*>, classVisitor: ClassVisitor) : ClassVisitor(Opcodes.ASM9, classVisitor), Opcodes {

    val annotations = ArrayList<ClassAnnotation>()
    val fields = ArrayList<AsmClassField>()
    val methods = ArrayList<AsmClassMethod>()
    val constructors = ArrayList<AsmClassConstructor>()

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
        return AsmClassAnnotationVisitor(descriptor, super.visitAnnotation(descriptor, visible)).apply {
            annotations += toAnnotation()
        }
    }

    override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor {
        return AsmClassFieldVisitor(super.visitField(access, name, descriptor, signature, value)).apply {
            fields += AsmClassField(name, owner, descriptor, access, annotations)
        }
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<String>?): MethodVisitor {
        if (name == "<clinit>") {
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
        return AsmClassMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions)).apply {
            if (name == "<init>") {
                constructors += AsmClassConstructor(name, owner, descriptor, access, parameterAnnotations, annotations)
            } else {
                methods += AsmClassMethod(name, owner, descriptor, access, parameterAnnotations, annotations)
            }
        }
    }

    override fun visitEnd() {
        methods.forEach { it.read() }
    }
}