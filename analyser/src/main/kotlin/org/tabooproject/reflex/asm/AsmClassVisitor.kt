package org.tabooproject.reflex.asm

import org.objectweb.asm.*
import org.tabooproject.reflex.ClassAnalyser
import org.tabooproject.reflex.ClassAnnotation
import org.tabooproject.reflex.Internal
import org.tabooproject.reflex.LazyClass

/**
 * @author izzel
 */
@Internal
class AsmClassVisitor(val owner: LazyClass, val classFinder: ClassAnalyser.ClassFinder, classVisitor: ClassVisitor) : ClassVisitor(Opcodes.ASM9, classVisitor), Opcodes {

    var access = -1
    var superclass: LazyClass? = null
    val interfaces = ArrayList<LazyClass>()
    val annotations = ArrayList<ClassAnnotation>()
    val fields = ArrayList<AsmClassField>()
    val methods = ArrayList<AsmClassMethod>()
    val constructors = ArrayList<AsmClassConstructor>()

    override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
        super.visit(version, access, name, signature, superName, interfaces)
        this.access = access
        // 获取父类信息
        if (superName != null) {
            this.superclass = LazyClass.of(superName, classFinder)
        }
        // 获取接口信息
        if (interfaces != null) {
            this.interfaces += interfaces.map { LazyClass.of(it, classFinder) }
        }
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
        return AsmClassAnnotationVisitor(super.visitAnnotation(descriptor, visible), descriptor, classFinder).apply {
            annotations += AsmAnnotation(this)
        }
    }

    override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor {
        return AsmClassFieldVisitor(super.visitField(access, name, descriptor, signature, value), classFinder).apply {
            fields += AsmClassField(name, owner, descriptor, access, classFinder, annotations)
        }
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<String>?): MethodVisitor {
        if (name == "<clinit>") {
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
        return AsmClassMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions), classFinder).apply {
            if (name == "<init>") {
                constructors += AsmClassConstructor(name, owner, descriptor, access, parameterAnnotations, classFinder, annotations)
            } else {
                methods += AsmClassMethod(name, owner, descriptor, access, parameterAnnotations, classFinder, annotations)
            }
        }
    }

    override fun visitEnd() {
        methods.forEach { it.read() }
    }
}