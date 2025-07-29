package com.litevar.agent.base.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 执行模式
 *
 * @author uncle
 * @since 2025/3/10 17:28
 */
@Getter
@AllArgsConstructor
public enum ExecuteMode {
    /**
     * 并行
     */
    PARALLEL(0),
    /**
     * 串行
     */
    SERIAL(1),
    /**
     * 拒绝
     */
    REJECT(2);

    private final Integer mode;

    public static ExecuteMode of(Integer mode) {
        for (ExecuteMode execMode : ExecuteMode.values()) {
            if (ObjectUtil.equals(execMode.mode, mode)) {
                return execMode;
            }
        }
        return null;
    }
}
