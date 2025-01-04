package org.tabooproject.reflex

import org.junit.jupiter.api.Test
import org.objectweb.asm.Type
import org.tabooproject.reflex.Asm.TypeTest
import org.tabooproject.reflex.asm.AsmSignature
import org.tabooproject.reflex.serializer.BinaryReader
import org.tabooproject.reflex.serializer.BinaryWriter
import kotlin.reflect.KClass

@TypeTest(String::class)
class Asm {

    annotation class TypeTest(val value: KClass<*>)

    private val test = arrayOf(intArrayOf(1, 2))
    private val bool = false

    fun test(): Boolean {
        return true
    }

    fun empty() {
    }

    class VoidConstructor(empty: Void)

    class Obj(val type: BaseType)

    open class BaseType

    class SubType : BaseType()

    @Test
    fun testAsm() {
        val rClass = ReflexClass.of(Asm::class.java, saving = false)
        val type = rClass.getAnnotation(TypeTest::class.java).property<Type>("value")!!
        println(type.descriptor)
        println(type.className)
        println(type.dimensions)
        println(type.sort)
    }

    @Test
    fun testAsm2() {
        val rClass = ReflexClass.of(Asm::class.java, saving = false)
        val field = rClass.getField("test")
        println(field.type.name)                       // I
        println(field.fieldType)                       // [[I
        println(test::class.java)                      // [[I
        println(test::class.java.getArrayDimensions()) // 2
    }

    @Test
    fun testAsm3() {
        val rClass = ReflexClass.of(Asm::class.java, saving = false)
        val field = rClass.getMethod("test")
        println(field.result.name)  // Z
        println(field.returnType)   // boolean
        println(field.result.isPrimitive)

        val writer = BinaryWriter()
        field.result.writeTo(writer)
        val toByteArray = writer.toByteArray()
        val of = LazyClass.of(BinaryReader(toByteArray), null)
        println(of)
        println(of.isPrimitive)
    }

    @Test
    fun testAsm4() {
        val rClass = ReflexClass.of(Asm::class.java, saving = false)
        val writer = BinaryWriter()
        rClass.writeTo(writer)
        val toByteArray = writer.toByteArray()
        val nClass = ReflexClass.of(BinaryReader(toByteArray), null)
        println(nClass)
        val field = nClass.getField("bool")
        println(field.type.name)
        println(field.fieldType)
    }

    @Test
    fun testAsm5() {
        val rClass = ReflexClass.of(Asm::class.java, saving = false)
        val field = rClass.getMethod("empty")
        println(field.result.name)         // V
        println(field.result.isPrimitive)  // true
        println(field.returnType)          // void
    }

    @Test
    fun testAsm6() {
        val rClass = ReflexClass.of(Asm::class.java, saving = false)
        val constructor = rClass.structure.constructors[0]
        constructor.parameter.forEach {
            println(it)
            println(it.name)
            println(it.instance)
        }
    }

    @Test
    fun testAsm7() {
        val rClass = ReflexClass.of(Obj::class.java, saving = false)
        val constructor = rClass.structure.constructors[0]
        println(constructor)
        constructor.parameter.forEach { t ->
            println("----")
            println(t)
            println(t.name)
            println(t.instance)
        }
        println("====")

        println(rClass.structure.getConstructorByType(BaseType::class.java))
        println(rClass.getConstructor(BaseType()))
        println(rClass.getConstructor(SubType()))
    }

    @Test
    fun testArray() {
        println(AsmSignature.signatureToClass("[[I"))  // [LazyClass([[int)]
    }
}