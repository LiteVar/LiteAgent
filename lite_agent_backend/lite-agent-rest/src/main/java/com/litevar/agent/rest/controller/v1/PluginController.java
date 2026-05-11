package com.litevar.agent.rest.controller.v1;

import cn.hutool.core.util.ObjectUtil;
import com.litevar.agent.auth.annotation.SystemRole;
import com.litevar.agent.auth.annotation.WorkspaceRole;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.dto.ConnectorDTO;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.entity.AgentApiKey;
import com.litevar.agent.base.entity.Plugin;
import com.litevar.agent.base.enums.*;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.valid.AddAction;
import com.litevar.agent.base.valid.UpdateAction;
import com.litevar.agent.base.vo.PluginAgentVO;
import com.litevar.agent.base.vo.PluginAnalyzeVO;
import com.litevar.agent.base.vo.PluginConnectorVO;
import com.litevar.agent.base.vo.PluginVO;
import com.litevar.agent.core.module.agent.AgentApiKeyService;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.core.module.plugin.PluginConnectorService;
import com.litevar.agent.core.module.plugin.PluginService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 插件相关
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Validated
@RestController
@RequestMapping("/v1/plugin")
public class PluginController {

    @Resource
    private PluginService pluginService;
    @Resource
    private PluginConnectorService connectorService;
    @Resource
    private AgentService agentService;
    @Autowired
    private AgentApiKeyService agentApiKeyService;


    /**
     * 插件列表
     * 智连中选择插件应只查已启用状态的插件
     *
     * @param status 插件状态
     * @return 插件列表
     */
    @GetMapping("/list")
    public ResponseData<List<Plugin>> list(@RequestParam(value = "status", required = false) Integer status) {
        List<Plugin> list = pluginService.lambdaQuery()
                .projectNone(Plugin::getUrl)
                .eq(ObjectUtil.isNotEmpty(status), Plugin::getStatus, status)
                .orderByDesc(Plugin::getCreateTime).list();
        return ResponseData.success(list);
    }

    /**
     * 新增插件（系统管理员）
     *
     * @param vo   插件表单
     * @param file 插件包
     * @return plugin id
     */
    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SystemRole(value = {SystemRoleEnum.ROLE_SYSTEM_ADMIN})
    public ResponseData<String> createWithPackage(@ModelAttribute @Validated(AddAction.class) PluginVO vo,
                                                  @RequestPart("file") MultipartFile file) {
        String id = pluginService.createPlugin(vo, file);
        return ResponseData.success(id);
    }

    /**
     * 编辑插件（系统管理员）
     *
     * @param id 插件ID
     * @param vo 插件表单
     * @return 处理结果
     */
    @PutMapping("/{id}")
    @SystemRole(value = {SystemRoleEnum.ROLE_SYSTEM_ADMIN})
    public ResponseData<String> update(@PathVariable String id,
                                       @RequestBody @Validated(UpdateAction.class) PluginVO vo) {
        vo.setId(id);
        pluginService.updatePlugin(vo);
        return ResponseData.success();
    }


    /**
     * 启用插件（系统管理员）
     *
     * @param id 插件ID
     * @return
     */
    @PostMapping("/{id}/enable")
    @SystemRole(value = {SystemRoleEnum.ROLE_SYSTEM_ADMIN})
    public ResponseData<String> enable(@PathVariable String id) {
        pluginService.enable(id);
        return ResponseData.success();
    }


    /**
     * 关闭插件（系统管理员）
     *
     * @param id 插件ID
     * @return
     */
    @PostMapping("/{id}/disable")
    @SystemRole(value = {SystemRoleEnum.ROLE_SYSTEM_ADMIN})
    public ResponseData<String> disable(@PathVariable String id) {
        pluginService.disable(id);
        return ResponseData.success();
    }

    /**
     * 删除插件（系统管理员）
     *
     * @param id 插件ID
     * @return 处理结果
     */
    @DeleteMapping("/{id}")
    @SystemRole(value = {SystemRoleEnum.ROLE_SYSTEM_ADMIN})
    public ResponseData<String> delete(@PathVariable String id) {
        pluginService.deletePlugin(id);
        return ResponseData.success();
    }

    /**
     * 智连列表
     *
     * @param workspaceId 工作空间ID
     * @param query       查询条件
     * @param pluginId    插件ID
     * @param status      智连状态
     * @return 智连列表
     */
    @GetMapping("/connector")
    public ResponseData<List<ConnectorDTO>> connectorList(@RequestHeader(value = CommonConstant.HEADER_WORKSPACE_ID, required = false) String workspaceId,
                                                          @RequestParam(value = "pluginId", required = false) String pluginId,
                                                          @RequestParam(value = "status", required = false) Integer status,
                                                          @RequestParam(value = "query", required = false) String query) {
        return ResponseData.success(connectorService.connectorList(workspaceId, query, pluginId, status));
    }

    /**
     * 新增智连
     *
     * @param workspaceId 工作空间ID
     * @param vo          智连表单
     * @return connectorId
     */
    @PostMapping("/connector")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> createConnector(
            @RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
            @RequestBody @Validated(AddAction.class) PluginConnectorVO vo) {
        String connectorId = connectorService.createConnector(workspaceId, vo);
        return ResponseData.success(connectorId);
    }

    /**
     * 编辑智连
     *
     * @param id 智连ID
     * @param vo 智连表单
     * @return 处理结果
     */
    @PutMapping("/connector/{id}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> updateConnector(@PathVariable String id,
                                                @RequestBody @Validated(UpdateAction.class) PluginConnectorVO vo) {
        vo.setId(id);
        connectorService.updateConnector(vo);
        return ResponseData.success();
    }

    /**
     * 获取插件配置 schema
     *
     * @param pluginId 插件ID
     * @return schema
     */
    @GetMapping("/{pluginId}/schema")
    public ResponseData<Object> schema(@PathVariable String pluginId) {
        return ResponseData.success(connectorService.schema(pluginId));
    }

    /**
     * 智连配置回显数据
     *
     * @param id connectorId
     * @return 配置数据
     */
    @GetMapping("/connector/{id}/data")
    public ResponseData<Object> connectorData(@PathVariable String id) {
        return ResponseData.success(connectorService.connectorData(id));
    }

    /**
     * 设置智连上下线
     *
     * @param id     智连ID
     * @param status 状态 2-上线,3-下线
     * @return 处理结果
     */
    @PostMapping("/connector/{id}/status")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> connectorStatus(@PathVariable String id,
                                                @RequestParam("status") Integer status) {
        PluginStatus statusEnum = PluginStatus.of(status);
        if (statusEnum != PluginStatus.ENABLED && statusEnum != PluginStatus.DISABLED) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        }
        connectorService.updateConnectorStatus(id, statusEnum);
        return ResponseData.success();
    }

    /**
     * 删除智连
     *
     * @param id 智连ID
     * @return 处理结果
     */
    @DeleteMapping("/connector/{id}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> deleteConnector(@PathVariable String id) {
        connectorService.deleteConnector(id);
        return ResponseData.success();
    }

    /**
     * 统计数据
     *
     * @param vo analyze request
     * @return analyze result
     */
    @PostMapping("/connector/analyze")
    public ResponseData<Object> analyze(@RequestBody @Validated PluginAnalyzeVO vo) {
        return ResponseData.success(connectorService.analyze(vo));
    }

    /**
     * agent列表(智连)
     *
     * @return
     */
    @GetMapping("/agentList")
    public ResponseData<List<PluginAgentVO>> pluginAgent(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId) {
        //当前工作空间已发布,非反思agent
        List<Agent> list = agentService.lambdaQuery()
                .projectDisplay(Agent::getId, Agent::getName)
                .eq(Agent::getWorkspaceId, workspaceId)
                .eq(Agent::getStatus, 1)
                .ne(Agent::getType, AgentType.REFLECTION.getType())
                .orderByDesc(Agent::getCreateTime)
                .list();
        List<String> agentIds = list.stream().map(Agent::getId).toList();
        List<PluginAgentVO> result = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(agentIds)) {
            Map<String, Agent> agentMap = list.stream().collect(Collectors.toMap(Agent::getId, i -> i));
            agentApiKeyService.lambdaQuery().in(AgentApiKey::getAgentId, agentIds)
                    .list().forEach(agentApiKey -> {
                        Agent agent = agentMap.get(agentApiKey.getAgentId());
                        PluginAgentVO vo = new PluginAgentVO();
                        vo.setId(agent.getId());
                        vo.setName(agent.getName());
                        vo.setApiKey(agentApiKey.getApiKey());
                        vo.setApiUrl(agentApiKey.getApiUrl());
                        result.add(vo);
                    });
        }
        return ResponseData.success(result);
    }
}
