package com.litevar.agent.base.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 模型状态变更事件
 *
 * @author uncle
 * @since 2026/3/6 17:15
 */
@Getter
public class ModelStatusChangeEvent extends ApplicationEvent {

    private final String modelId;

    public ModelStatusChangeEvent(Object source, String modelId) {
        super(source);
        this.modelId = modelId;
    }
}
