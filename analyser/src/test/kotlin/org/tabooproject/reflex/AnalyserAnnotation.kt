package org.tabooproject.reflex

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR)
annotation class AnalyserAnnotation(
    val value: String,
    val type: Test = Test.A,
    val cls: KClass<*> = Test::class
) {

    enum class Test {

        A, B, C
    }
}