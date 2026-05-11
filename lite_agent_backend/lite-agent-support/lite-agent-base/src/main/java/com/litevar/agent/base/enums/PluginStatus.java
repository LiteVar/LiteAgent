package com.litevar.agent.base.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Plugin status.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Getter
public enum PluginStatus {
    /**
     * 初始化
     */
    INIT(0),
    /**
     * 启用中
     */
    ENABLING(1),
    /**
     * 已启用,上线
     */
    ENABLED(2),
    /**
     * 不启用,下线
     */
    DISABLED(3);

    @JsonValue
    private final Integer status;

    PluginStatus(Integer status) {
        this.status = status;
    }

    public static PluginStatus of(Integer status) {
        for (PluginStatus value : values()) {
            if (value.status.equals(status)) {
                return value;
            }
        }
        return null;
    }
}
