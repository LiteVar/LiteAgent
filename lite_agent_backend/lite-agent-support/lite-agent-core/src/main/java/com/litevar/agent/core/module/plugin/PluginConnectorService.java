package com.litevar.agent.core.module.plugin;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.dto.ConnectorDTO;
import com.litevar.agent.base.entity.Account;
import com.litevar.agent.base.entity.Plugin;
import com.litevar.agent.base.entity.PluginConnector;
import com.litevar.agent.base.enums.PluginStatus;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.enums.SystemRoleEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.vo.PluginAnalyzeVO;
import com.litevar.agent.base.vo.PluginConnectorVO;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.core.module.workspace.WorkspaceMemberService;
import com.mongoplus.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Plugin connector service.
 *
 * @author uncle
 * @since 2026/01/12 14:45
 */
@Service
public class PluginConnectorService extends ServiceImpl<PluginConnector> {

    @Lazy
    @Resource
    private PluginService pluginService;

    @Resource
    private PluginContainerClient containerClient;
    @Resource
    private ModelService modelService;
    @Autowired
    private WorkspaceMemberService workspaceMemberService;

    public PluginConnector findById(String id) {
        return Optional.ofNullable(this.getById(id)).orElseThrow();
    }

    public List<ConnectorDTO> connectorList(String workspaceId, String query, String pluginId, Integer status) {
        List<PluginConnector> list = this.lambdaQuery()
                .eq(StrUtil.isNotBlank(workspaceId), PluginConnector::getWorkspaceId, workspaceId)
                .eq(StrUtil.isNotBlank(pluginId), PluginConnector::getPluginId, pluginId)
                .eq(ObjectUtil.isNotEmpty(status), PluginConnector::getStatus, status)
                .like(StrUtil.isNotBlank(query), PluginConnector::getName, query)
                .orderByDesc(PluginConnector::getCreateTime).list();
        String userId = LoginContext.currentUserId();
        return list.stream().map(i -> {
            ConnectorDTO dto = BeanUtil.copyProperties(i, ConnectorDTO.class);
            Plugin plugin = pluginService.findById(dto.getPluginId());
            if (plugin != null) {
                dto.setPluginStatus(plugin.getStatus());
                dto.setPluginName(plugin.getName());
                dto.setPluginDescription(plugin.getDescription());
                //connector是自己创建的,非上线状态,插件是上线状态,空间非普通成员 => 可编辑
                boolean canEdit = StrUtil.equals(userId, i.getUserId())
                        && notUserRole(workspaceId);
                dto.setCanEdit(canEdit);
                dto.setCreateUser(modelService.userInfo(i.getUserId()).getName());
            }
            return dto;
        }).toList();
    }

    public String createConnector(String workspaceId, PluginConnectorVO vo) {
        Plugin plugin = pluginService.ensurePluginEnabled(vo.getPluginId());
        String connectorId = IdUtil.getSnowflakeNextIdStr();

        //调用插件保存配置
        containerClient.config(plugin.getId(), connectorId, vo.getData());

        PluginConnector connector = new PluginConnector();
        connector.setId(connectorId);
        connector.setPluginId(vo.getPluginId());
        connector.setWorkspaceId(workspaceId);
        connector.setName(vo.getName());
        connector.setDescription(vo.getDescription());
        connector.setIcon(vo.getIcon());
        connector.setStatus(PluginStatus.INIT.getStatus());
        connector.setUserId(LoginContext.currentUserId());
        this.save(connector);
        return connectorId;
    }

    public void updateConnector(PluginConnectorVO vo) {
        PluginConnector connector = editConnector(vo.getId());
        Plugin plugin = pluginService.ensurePluginEnabled(vo.getPluginId());

        //调用插件
        containerClient.config(plugin.getId(), connector.getId(), vo.getData());

        connector.setName(vo.getName());
        connector.setDescription(vo.getDescription());
        connector.setIcon(vo.getIcon());
        this.updateById(connector);
    }

    public void updateConnectorStatus(String connectorId, PluginStatus status) {
        PluginConnector connector = editConnector(connectorId);
        Plugin plugin = pluginService.ensurePluginEnabled(connector.getPluginId());
        boolean offline = status == PluginStatus.DISABLED;

        //call plugin
        containerClient.status(plugin.getId(), connectorId, offline);

        //update db
        connector.setStatus(status.getStatus());
        this.updateById(connector);
    }

    public Object schema(String pluginId) {
        return containerClient.schema(pluginId);
    }

    public Object connectorData(String connectorId) {
        PluginConnector connector = findById(connectorId);
        Object data = containerClient.data(connector.getPluginId(), connectorId);
        if (StrUtil.equals(LoginContext.currentUserId(), connector.getUserId())) {
            return data;
        }
        //如果不是他创建的connector,只能看agent,其他的都不给看
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) data;
        map.forEach((k, v) -> {
            if (!StrUtil.equalsAny(k, "agentId", "agentApiKey", "agentBaseUrl") && v instanceof String) {
                map.put(k, "******");
            }
        });
        return map;
    }

    public void disableConnector(String pluginId) {
        // 插件下线,所有connector也要下线
        List<PluginConnector> list = this.lambdaQuery().projectDisplay(PluginConnector::getId).eq(PluginConnector::getPluginId, pluginId).list();
        list.forEach(i -> updateConnectorStatus(i.getId(), PluginStatus.DISABLED));
    }

    public void deleteConnector(String connectorId) {
        PluginConnector connector = editConnector(connectorId);
        Plugin plugin = pluginService.ensurePluginEnabled(connector.getPluginId());
        containerClient.delete(plugin.getId(), connectorId);

        //db remove value
        this.removeById(connectorId);
    }

    public Object analyze(PluginAnalyzeVO vo) {
        PluginConnector connector = findById(vo.getConnectorId());
        Plugin plugin = pluginService.ensurePluginEnabled(connector.getPluginId());
        return containerClient.analyze(plugin.getId(), connector.getId(), vo.getStartTime(), vo.getEndTime());
    }

    private PluginConnector editConnector(String connectorId) {
        PluginConnector connector = findById(connectorId);
        //谁创建谁有编辑权限,系统管理员除外
        Account account = modelService.userInfo(LoginContext.currentUserId());
        if (ObjectUtil.notEqual(account.getSystemRole(), SystemRoleEnum.ROLE_SYSTEM_ADMIN.getSystemRole())
                && !StrUtil.equals(LoginContext.currentUserId(), connector.getUserId())) {
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION_OPERATE);
        }
        return connector;
    }

    private boolean notUserRole(String workspaceId) {
        //非普通成员都有编辑权限
        String userId = LoginContext.currentUserId();
        RoleEnum role = workspaceMemberService.userRole(workspaceId, userId);
        return role != RoleEnum.ROLE_USER;
    }
}
