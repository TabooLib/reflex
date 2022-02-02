package org.tabooproject.reflex.reflection

import org.tabooproject.reflex.ClassAnnotation
import org.tabooproject.reflex.Internal
import org.tabooproject.reflex.LazyAnnotatedClass

@Internal
class InstantAnnotatedClass(override val instance: Class<*>, annotations: List<ClassAnnotation>) : LazyAnnotatedClass(instance.name, annotations) {

    override fun toString(): String {
        return "InstantAnnotatedClass(instance=$instance) ${super.toString()}"
    }
}