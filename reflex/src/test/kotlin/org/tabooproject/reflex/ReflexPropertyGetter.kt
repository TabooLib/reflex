package org.tabooproject.reflex

import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty1

class ReflexPropertyGetter {

    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.FIELD)
    annotation class Key(val id: String)

    class Data(
        @Key("sb")
        val name: String,
    )

    inline fun <reified T> query(process: Processor.() -> Unit) {
        Processor(T::class.java).also(process)
    }

    class Processor(val owner: Class<*>) {

        infix fun KProperty1<*, *>.eq(any: Any) {
            println("正在进行 ${owner.name} 下的判定: $name eq $any")
            val id = ReflexClass.of(owner).getField(name).getAnnotation(Key::class.java).property<String>("id")
            println("@Key = $id")
        }
    }

    @Test
    fun test() {
        query<Data> {
            Data::name eq "SB"
        }
    }
}