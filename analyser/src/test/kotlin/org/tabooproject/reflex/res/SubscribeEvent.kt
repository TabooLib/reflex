package org.tabooproject.reflex.res

import org.tabooproject.reflex.res.EventOrder
import org.tabooproject.reflex.res.EventPriority
import org.tabooproject.reflex.res.PostOrder

@Target(AnnotationTarget.FUNCTION)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
annotation class SubscribeEvent(
    val priority: EventPriority = EventPriority.NORMAL,
    val ignoreCancelled: Boolean = false,
    // only bungeecord platform
    val level: Int = -1,
    // only velocity
    val postOrder: PostOrder = PostOrder.NORMAL,
    // only sponge platform
    val order: EventOrder = EventOrder.DEFAULT,
    val beforeModifications: Boolean = false,
    // optional event
    val bind: String = ""
)