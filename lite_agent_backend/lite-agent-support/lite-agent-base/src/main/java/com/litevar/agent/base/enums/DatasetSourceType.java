package com.litevar.agent.base.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author reid
 * @since 3/24/25
 */

@Getter
@AllArgsConstructor
public enum DatasetSourceType {
    INPUT("INPUT"),
    HTML("HTML"),
    FILE("FILE");

    private final String value;
}
