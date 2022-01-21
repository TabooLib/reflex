package org.tabooproject.reflex.asm

import org.objectweb.asm.*
import org.tabooproject.reflex.ClassConstructor
import org.tabooproject.reflex.ClassField
import org.tabooproject.reflex.ClassMethod
import kotlin.collections.ArrayList

/**
 * @author izzel
 */
class AsmClassVisitor(val owner: Class<*>, classVisitor: ClassVisitor, val excludeModifier: Int) : ClassVisitor(Opcodes.ASM9, classVisitor), Opcodes {

    val fields = ArrayList<ClassField>()
    val methods = ArrayList<ClassMethod>()
    val constructors = ArrayList<ClassConstructor>()

    override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor {
        if (access and excludeModifier == 0) {
            fields.add(AsmClassField(name, owner, descriptor, access))
        }
        return super.visitField(access, name, descriptor, signature, value)
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<String>?): MethodVisitor {
        if (access and excludeModifier == 0 && name != "<clinit>") {
            if (name == "<init>") {
                constructors.add(AsmClassConstructor(name, owner, descriptor, access))
            } else {
                methods.add(AsmClassMethod(name, owner, descriptor, access))
            }
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }
}