package org.tabooproject.reflex

import org.junit.jupiter.api.Test
import org.tabooproject.reflex.Reflex.Companion.invokeMethod

class ReflexJava {

    @Test
    fun test1() {
        val rc = ReflexClass.of(OpenAPI::class.java)
        rc.structure.fields.forEach {
            println("Field: $it")
        }
        rc.structure.constructors.forEach {
            println("Constructor: $it")
            it.parameter.forEach { klass ->
                println("  Parameter: $klass")
            }
        }
        rc.structure.methods.forEach {
            println("Method: $it")
            it.parameter.forEach { klass ->
                println("  Parameter: $klass")
            }
        }
    }

    @Test
    fun test2() {
        OpenAPI::class.java.invokeMethod<Any>("call", "sb", arrayOf(1), isStatic = true)
    }
}