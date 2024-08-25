package org.tabooproject.reflex

enum class AnalyseMode {

    /**
     * 反射优先
     * 失败会尝试使用 ASM 模式
     * 稳定性高，但是初始化速度慢
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
     * 初始化速度快，稳定性暂未知（缺少测试数据）
     */
    ASM_FIRST,

    /**
     * 仅 ASM
     * 失败会抛出异常
     */
    ASM_ONLY;

    companion object {

        /**
         * 默认使用的分析模式
         * 自 1.1.1 版本起，采用 ASM_FIRST 模式，以提高初始化速度
         */
        var default = ASM_FIRST
    }
}