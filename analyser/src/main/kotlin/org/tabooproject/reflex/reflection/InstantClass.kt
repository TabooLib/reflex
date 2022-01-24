package org.tabooproject.reflex.reflection

import org.tabooproject.reflex.LazyClass

class InstantClass(override val instance: Class<*>) : LazyClass(instance.name) {

    override fun toString(): String {
        return "InstantClass(instance=$instance) ${super.toString()}"
    }
}