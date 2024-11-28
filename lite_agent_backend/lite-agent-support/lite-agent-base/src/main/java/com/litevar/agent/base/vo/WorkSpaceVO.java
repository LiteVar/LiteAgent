package com.litevar.agent.base.vo;

import com.litevar.agent.base.enums.RoleEnum;
import lombok.Data;

/**
 * @author uncle
 * @since 2024/8/5 11:55
 */
@Data
public class WorkSpaceVO {
    /**
     * 工作空间id
     */
    private String id;
    /**
     * 工作空间名字
     */
    private String name;
    /**
     * 当前账号在里面的角色
     * @see RoleEnum#code
     */
    private RoleEnum role;
}
