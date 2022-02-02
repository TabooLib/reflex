package org.tabooproject.reflex.reflection

import org.tabooproject.reflex.ClassAnnotation
import org.tabooproject.reflex.Internal
import org.tabooproject.reflex.JavaClassField
import org.tabooproject.reflex.LazyClass
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * @author 坏黑
 * @since 2022/1/21 7:11 PM
 */
@Internal
class InstantClassField(owner: Class<*>, private val field: Field) : JavaClassField(field.name, owner) {

    val annotationsLocal = field.declaredAnnotations.map { InstantAnnotation(it) }

    override val type: LazyClass
        get() = InstantClass(this.field.type)

    override val isStatic: Boolean
        get() = Modifier.isStatic(this.field.modifiers)

    override val annotations: List<ClassAnnotation>
        get() = annotationsLocal

    override fun toString(): String {
        return "InstantClassField(field=$field)"
    }
}