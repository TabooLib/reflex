package org.tabooproject.reflex.serializer

import org.tabooproject.reflex.serializer.SerializationType.values

enum class SerializationType(val id: Int) {

    // 基础类型
    BYTE(0),
    SHORT(1),
    INT(2),
    LONG(3),
    FLOAT(4),
    DOUBLE(5),
    BOOLEAN(6),
    CHAR(7),
    STRING(8),

    // 数组和集合
    ARRAY(10),
    LIST(11),

    // 嵌套对象会被转换为 Map
    MAP(12),

    // 类相关
    CLASS(21),
    LAZY_CLASS(22),
    ENUM(23),
    LAZY_ENUM(24),
    TYPE(25), // ASM 类型

    // 基础类型数组
    BYTE_ARRAY(31),
    SHORT_ARRAY(32),
    INT_ARRAY(33),
    LONG_ARRAY(34),
    FLOAT_ARRAY(35),
    DOUBLE_ARRAY(36),
    BOOLEAN_ARRAY(37),
    CHAR_ARRAY(38);

    companion object {

        val typeMap = values().associateBy { it.id }

        fun fromId(id: Int): SerializationType {
            return typeMap[id] ?: error("Unknown type id: $id")
        }
    }
} 