package com.litevar.agent.base.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 导入重复数据时,操作类型
 *
 * @author uncle
 * @since 2025/11/6 12:09
 */
@Getter
@AllArgsConstructor
public enum OperateTypeEnum {
    /**
     * 新增
     */
    INSERT(0),
    /**
     * 覆盖(引用并更新旧数据)
     */
    UPDATE(1),
    /**
     * 跳过(引用旧数据)
     */
    SKIP(2),

    ;
    private final Integer operate;
}