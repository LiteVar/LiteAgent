package com.litevar.agent.rest.util;

import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.core.module.workspace.WorkspaceMemberService;

/**
 * @author reid
 * @since 3/26/25
 */
public class PermissionUtil {

    /**
     * 编辑权限
     */
    public static boolean getEditPermission(String creatorId, String workspaceId) {
        String userId = LoginContext.currentUserId();
        RoleEnum role = SpringUtil.getBean(WorkspaceMemberService.class).userRole(workspaceId, userId);
        //谁创建谁有权限编辑,并且普通成员没有权限修改
        return role == RoleEnum.ROLE_ADMIN || role == RoleEnum.ROLE_DEVELOPER;
    }

    /**
     * 删除权限
     */
    public static boolean getDeletePermission(String creatorId, String workspaceId) {
        String userId = LoginContext.currentUserId();
        RoleEnum role = SpringUtil.getBean(WorkspaceMemberService.class).userRole(workspaceId, userId);

        //管理员: 可以删除自己及他人分享的模型
        //开发者:只能删除自己创建的模型
        return role == RoleEnum.ROLE_ADMIN
            || (role == RoleEnum.ROLE_DEVELOPER && StrUtil.equals(creatorId, userId));
    }
}
