package com.litevar.agent.base.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author reid
 * @since 3/24/25
 */

@Getter
@AllArgsConstructor
public enum EmbedStatus {
    PENDING("PENDING"),
    SUCCESS("SUCCESS"),
    FAIL("FAIL");

    private final String value;
}
