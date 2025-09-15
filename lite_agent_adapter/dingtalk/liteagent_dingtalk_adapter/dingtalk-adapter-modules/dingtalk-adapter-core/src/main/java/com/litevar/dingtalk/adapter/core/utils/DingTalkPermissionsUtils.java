package com.litevar.dingtalk.adapter.core.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.litevar.dingtalk.adapter.common.core.exception.AuthException;
import com.litevar.dingtalk.adapter.core.bot.CardManager;
import com.litevar.dingtalk.adapter.core.dto.AgentRobotRefDTO;
import com.litevar.dingtalk.adapter.core.dto.GetDepartmentUserListDTO;
import com.litevar.dingtalk.adapter.core.service.DepartmentService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

import static com.litevar.dingtalk.adapter.common.core.constant.DingTalkConstants.DING_TALK_NOT_PERMISSIONS_FLAG;

/**
 *
 * 钉钉应用权限
 *
 * @author Teoan
 * @since 2025/8/26 18:00
 */
@Slf4j
public class DingTalkPermissionsUtils {


    /**
     * 检测权限
     */
    public static void checkPermissions(AgentRobotRefDTO agentRobotRefDTO) {
        DepartmentService departmentService = SpringUtil.getBean(DepartmentService.class);
        CardManager cardManager = new CardManager(agentRobotRefDTO.getRobotClientId()
                , agentRobotRefDTO.getRobotClientSecret()
                , agentRobotRefDTO.getCardTemplateId());
        try {
            // 检查 Card.Instance.Write 权限
            cardManager.sendCard("test", agentRobotRefDTO.getRobotCode(), "test", "1", new HashMap<>());
            // 检查 Card.Streaming.Write 权限
            cardManager.streamUpdate("test", "test", false, "content");
            // 检查 qyapi_get_department_list 权限
            departmentService.getDepartmentList(null, agentRobotRefDTO.getRobotCode());
            // 检查 qyapi_get_department_member 权限
            departmentService.getDepartmentUserList(new GetDepartmentUserListDTO(0L, 1L, agentRobotRefDTO.getRobotCode(), null, 20L));
        } catch (Exception e) {
            log.error("checkPermissions error:{}", e.getMessage());
            if (StrUtil.contains(e.getMessage(), DING_TALK_NOT_PERMISSIONS_FLAG)) {
                throw new AuthException(StrUtil.format(DING_TALK_NOT_PERMISSIONS_FLAG + ":[{},{},{},{}],查看链接申请并开通即可：https://open-dev.dingtalk.com/appscope/apply?content={}",
                        "Card.Instance.Write", "Card.Streaming.Write", "qyapi_get_department_list", "qyapi_get_department_member",
                        agentRobotRefDTO.getRobotClientId()));
            }
        }
    }


}
