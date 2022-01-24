package org.tabooproject.reflex

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CONSTRUCTOR)
annotation class AnalyserAnnotation(val value: String)