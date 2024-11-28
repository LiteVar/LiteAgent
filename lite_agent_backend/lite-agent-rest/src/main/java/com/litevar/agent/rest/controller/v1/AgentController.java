package com.litevar.agent.rest.controller.v1;

import com.litevar.agent.auth.annotation.WorkspaceRole;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.dto.AgentDTO;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.vo.AgentCreateForm;
import com.litevar.agent.base.vo.AgentDetailVO;
import com.litevar.agent.base.vo.AgentUpdateForm;
import com.litevar.agent.core.module.agent.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * agent接口
 *
 * @author reid
 * @since 2024/8/9
 */

@Validated
@RestController
@RequestMapping("/v1/agent")
public class AgentController {
    @Autowired
    private AgentService agentService;

    /**
     * agent列表
     *
     * @param workspaceId
     * @param tab         0-全部,1-系统,2-来自分享,3-我的,4-本地agent
     * @param name        agent名称
     * @return
     */
    @GetMapping("/list")
    public ResponseData<List<AgentDTO>> agentList(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
                                                  @RequestParam(value = "tab", defaultValue = "0") Integer tab,
                                                  @RequestParam(value = "name", required = false) String name) {
        List<AgentDTO> list = agentService.agentList(workspaceId, tab, name, 1);
        return ResponseData.success(list);
    }

    /**
     * agent列表(管理)
     *
     * @param workspaceId
     * @param tab         0-全部,1-系统,2-来自分享,3-我的
     * @param name        agent名称
     * @return
     */
    @GetMapping("/adminList")
    public ResponseData<List<AgentDTO>> agentAdminListAdmin(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
                                                            @RequestParam(value = "tab", defaultValue = "0") Integer tab,
                                                            @RequestParam(value = "name", required = false) String name) {
        List<AgentDTO> list = agentService.agentAdminList(workspaceId, tab, name);
        return ResponseData.success(list);
    }

    /**
     * agent详情
     *
     * @param id agent id
     * @return
     */
    @GetMapping("/{id}")
    public ResponseData<AgentDetailVO> info(@PathVariable("id") String id) {
        return ResponseData.success(agentService.info(id));
    }

    /**
     * agent详情(管理)
     *
     * @param id
     * @return
     */
    @GetMapping("/adminInfo/{id}")
    public ResponseData<AgentDetailVO> adminInfo(@PathVariable("id") String id) {
        return ResponseData.success(agentService.adminInfo(id));
    }

    /**
     * 新建agent
     *
     * @return
     */
    @PostMapping("/add")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<Agent> createAgent(
            @RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
            @Validated @RequestBody AgentCreateForm form
    ) {
        return ResponseData.success(agentService.addAgent(workspaceId, form));
    }

    /**
     * 删除agent
     *
     * @param id agent id
     * @return
     */
    @DeleteMapping("/{id}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> deleteAgent(@PathVariable("id") String id) {
        agentService.removeAgent(id);
        return ResponseData.success();
    }

    /**
     * 修改agent
     *
     * @param id   agentId
     * @param form
     * @return
     */
    @PutMapping("/{id}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<Agent> updateAgent(
            @PathVariable("id") String id,
            @Validated @RequestBody AgentUpdateForm form
    ) {
        return ResponseData.success(agentService.updateAgent(id, form));
    }

    /**
     * 开启或关闭分享状态
     *
     * @param id agent id
     * @return
     */
    @PostMapping("/enableShare/{id}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> enableShare(@PathVariable("id") String id) {
        agentService.enableShare(id);
        return ResponseData.success();
    }

    /**
     * 发布
     *
     * @param id agent id
     * @return
     */
    @PutMapping("/release/{id}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> release(@PathVariable("id") String id) {
        agentService.release(id);
        return ResponseData.success();
    }
}
