package org.tabooproject.reflex

enum class AnalyseMode {

    /**
     * 反射优先
     * 失败会尝试使用 ASM 模式
     */
    REFLECTION_FIRST,

    /**
     * 仅反射
     * 失败会抛出异常
     */
    REFLECTION_ONLY,

    /**
     * ASM 优先
     * 失败会尝试使用反射模式
     */
    ASM_FIRST,

    /**
     * 仅 ASM
     * 失败会抛出异常
     */
    ASM_ONLY,
}