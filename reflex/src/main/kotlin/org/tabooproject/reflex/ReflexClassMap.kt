package org.tabooproject.reflex

/**
 * 基于 ByteBuffer 的二进制序列化工具
 * 以避免每次启动时重新加载所有类导致的性能问题
 *
 * 目前仅支持对 ASM 模式进行序列化
 */
object ReflexClassMap {

    fun serializeToBytes(map: Map<String, ReflexClass>): ByteArray {
        TODO()
    }

    fun deserializeFromBytes(bytes: ByteArray): Map<String, ReflexClass> {
        TODO()
    }
}