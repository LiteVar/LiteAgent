package com.litevar.agent.core.module.agent;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.AgentDTO;
import com.litevar.agent.base.entity.*;
import com.litevar.agent.base.enums.AgentType;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.AgentCreateForm;
import com.litevar.agent.base.vo.AgentDetailVO;
import com.litevar.agent.base.vo.AgentUpdateForm;
import com.litevar.agent.base.vo.FunctionVO;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.core.module.local.LocalAgentService;
import com.litevar.agent.core.module.local.ToolFunctionService;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.core.module.workspace.WorkspaceMemberService;
import com.mongoplus.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author reid
 * @since 2024/8/12
 */
@Service
public class AgentService extends ServiceImpl<Agent> {
    @Autowired
    private ToolService toolService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private WorkspaceMemberService workspaceMemberService;
    @Autowired
    private LocalAgentService localAgentService;
    @Autowired
    private ToolFunctionService toolFunctionService;
    @Autowired
    private AgentApiKeyService agentApiKeyService;

    public Agent findById(String id) {
        Agent agent = this.getById(id);
        return Optional.ofNullable(agent).orElseThrow();
    }

    public AgentDetailVO info(String agentId) {
        Agent agent = getById(agentId);
        if (agent == null) {
            agent = localAgentService.getById(agentId);
        }

        AgentDetailVO vo = new AgentDetailVO();
        vo.setAgent(agent);
        return vo;
    }

    public AgentDetailVO adminInfo(String agentId) {
        Agent agent = findById(agentId);

        Agent cache = (Agent) RedisUtil.getValue(String.format(CacheKey.AGENT_DRAFT, agentId));
        if (cache != null) {
            agent = cache;
        }

        return agentDetail(agent);
    }

    public AgentDetailVO agentDetail(Agent agent) {
        AgentDetailVO vo = new AgentDetailVO();

        vo.setAgent(agent);

        List<AgentApiKey> apiKeyList = agentApiKeyService.list(agentApiKeyService.lambdaQuery().eq(AgentApiKey::getAgentId, agent.getId()));
        vo.setApiKeyList(apiKeyList);

        if (ObjectUtil.isNotEmpty(agent.getFunctionList())) {
            List<ToolFunction> functionList = toolFunctionService.getByIds(
                    agent.getFunctionList().stream().map(Agent.AgentFunction::getFunctionId).toList());
            List<String> functionIds = functionList.stream().map(ToolFunction::getId).toList();
            agent.getFunctionList().removeIf(i -> !functionIds.contains(i.getFunctionId()));

            List<String> toolIds = functionList.stream().map(ToolFunction::getToolId).toList();
            Map<String, ToolProvider> toolMap = toolService.getByIds(toolIds).stream().collect(Collectors.toMap(ToolProvider::getId, i -> i));

            Map<String, ToolFunction> functionMap = functionList.stream().collect(Collectors.toMap(ToolFunction::getId, i -> i));

            List<FunctionVO> fvList = new ArrayList<>();
            agent.getFunctionList().forEach(v -> {
                FunctionVO fv = new FunctionVO();
                fv.setMode(v.getMode());
                fv.setFunctionId(v.getFunctionId());
                ToolFunction function = functionMap.get(v.getFunctionId());
                fv.setToolId(function.getToolId());
                fv.setFunctionName(function.getResource());
                fv.setFunctionDesc(function.getDescription());
                fv.setProtocol(function.getProtocol());
                ToolProvider tool = toolMap.get(function.getToolId());
                fv.setToolName(tool.getName());
                fv.setIcon(tool.getIcon());
                fv.setRequestMethod(function.getRequestMethod());
                fvList.add(fv);
            });
            vo.setFunctionList(fvList);
        }

        if (StrUtil.isNotBlank(agent.getLlmModelId())) {
            LlmModel model = modelService.getById(agent.getLlmModelId());
            vo.setModel(model);
            if (model == null) {
                vo.getAgent().setLlmModelId("");
            }
        }

        vo.setCanDelete(getEditPermission(agent.getWorkspaceId()));
        vo.setCanEdit(getEditPermission(agent.getWorkspaceId()));
        vo.setCanRelease(RedisUtil.exists(String.format(CacheKey.AGENT_DRAFT, agent.getId())));

        return vo;
    }

    public Agent addAgent(String workspaceId, AgentCreateForm form) {
        Agent agent = new Agent();
        BeanUtil.copyProperties(form, agent, CopyOptions.create().setIgnoreNullValue(true));
        agent.setWorkspaceId(workspaceId);
        agent.setUserId(LoginContext.currentUserId());
        this.save(agent);

        return agent;
    }

    public void removeAgent(String id) {
        this.removeById(id);
    }

    public Agent updateAgent(String id, AgentUpdateForm form) {
        Agent agent = findById(id);

        if (ObjectUtil.isNotEmpty(form.getMaxTokens()) && StrUtil.isNotEmpty(form.getLlmModelId())) {
            LlmModel model = modelService.findById(form.getLlmModelId());
            Integer modelMaxToken = (ObjectUtil.isNotEmpty(model) && ObjectUtil.isNotEmpty(model.getMaxTokens())) ?
                    model.getMaxTokens() : 4096;
            if (form.getMaxTokens() > modelMaxToken) {
                throw new ServiceException(ServiceExceptionEnum.MAX_TOKEN_LARGER);
            }
        }

        if (ObjectUtil.isNotEmpty(form.getSequence())) {
            List<String> functionIds = ObjectUtil.isNotEmpty(form.getFunctionList()) ?
                    form.getFunctionList().stream().map(Agent.AgentFunction::getFunctionId).toList()
                    : Collections.emptyList();
            for (String fid : form.getSequence()) {
                if (!functionIds.contains(fid)) {
                    throw new ServiceException(ServiceExceptionEnum.FUNCTION_NOT_CHOOSE);
                }
            }
        }

        if (ObjectUtil.isNotEmpty(form.getSubAgentIds())) {
            //反思agent不能有子agent
            if (ObjectUtil.equal(agent.getType(), AgentType.REFLECTION.getType())) {
                throw new ServiceException(ServiceExceptionEnum.REFLECT_AGENT_WITHOUT_SUB_AGENT);
            }

            //反思agent个数不能超过5个
            List<Agent> subAgentList = this.list(lambdaQuery()
                    .projectDisplay(Agent::getId, Agent::getName, Agent::getType)
                    .in(Agent::getId, form.getSubAgentIds()));
            List<Agent> reflectAgent = subAgentList.parallelStream()
                    .filter(i -> ObjectUtil.equal(i.getType(), AgentType.REFLECTION.type))
                    .toList();
            if (reflectAgent.size() > 5) {
                throw new ServiceException(ServiceExceptionEnum.REFLECT_AGENT_OVERSIZE);
            }
        }

        BeanUtil.copyProperties(form, agent, CopyOptions.create().setIgnoreNullValue(true));

        RedisUtil.setValue(String.format(CacheKey.AGENT_DRAFT, id), agent);
        RedisUtil.delKey(String.format(CacheKey.AGENT_DATASET_DRAFT, id));
        if (ObjectUtil.isNotEmpty(form.getDatasetIds())) {
            RedisUtil.setValue(String.format(CacheKey.AGENT_DATASET_DRAFT, id), form.getDatasetIds());
        }
        return agent;
    }

    public void release(String id) {
        Agent value = (Agent) RedisUtil.getValue(String.format(CacheKey.AGENT_DRAFT, id));
        if (value == null) {
            throw new ServiceException(30001, "请先保存信息再发布");
        }

        if (StrUtil.isBlank(value.getLlmModelId())) {
            throw new ServiceException(ServiceExceptionEnum.MODEL_NOT_EXIST_OR_NOT_SHARE);
        }

        value.setStatus(1);
        this.updateById(value);

        RedisUtil.delKey(String.format(CacheKey.AGENT_DRAFT, id));
    }

    public List<AgentDTO> agentList(String workspaceId, Integer tab, String agentName, Integer status) {
        if (tab == 1) {
            //系统: 系统预置,暂为空
            return Collections.emptyList();
        }

        String userId = LoginContext.currentUserId();
        List<AgentDTO> res = new ArrayList<>();

        if (tab == 4) {
            //本地agent
            String spaceId = localAgentService.userAdminWorkspaceId(userId);
            if (StrUtil.equals(spaceId, workspaceId)) {
                List<LocalAgent> list = localAgentService.list(localAgentService.lambdaQuery()
                        .projectDisplay(Agent::getId, Agent::getUserId, Agent::getName, Agent::getIcon,
                                Agent::getDescription, Agent::getStatus, Agent::getType, Agent::getMode)
                        .eq(Agent::getUserId, userId));
                for (LocalAgent agent : list) {
                    AgentDTO agentDTO = new AgentDTO();
                    BeanUtil.copyProperties(agent, agentDTO);
                    agentDTO.setCreateUser(modelService.userInfo(agentDTO.getUserId()).getName());
                    res.add(agentDTO);
                }

            } else {
                return Collections.emptyList();
            }
        } else {
            List<Agent> list = this.lambdaQuery()
                    .projectDisplay(Agent::getId, Agent::getUserId, Agent::getName, Agent::getIcon, Agent::getDescription,
                            Agent::getStatus, Agent::getType, Agent::getMode)
                    .eq(Agent::getWorkspaceId, workspaceId)
                    .eq(tab == 3, Agent::getUserId, userId)
                    .eq(ObjectUtil.isNotEmpty(status), Agent::getStatus, status)
                    .like(StrUtil.isNotBlank(agentName), Agent::getName, agentName)
                    .orderByDesc(Agent::getCreateTime)
                    .list();
            for (Agent agent : list) {
                AgentDTO dto = BeanUtil.copyProperties(agent, AgentDTO.class);
                dto.setCreateUser(modelService.userInfo(dto.getUserId()).getName());
                res.add(dto);
            }
        }
        return res;
    }

    public List<AgentDTO> agentAdminList(String workspaceId, Integer tab, String agentName) {
        List<AgentDTO> res = agentList(workspaceId, tab, agentName, null);
        res.forEach(dto -> {
            Object value = RedisUtil.getValue(String.format(CacheKey.AGENT_DRAFT, dto.getId()));
            if (value != null) {
                BeanUtil.copyProperties(value, dto, CopyOptions.create().setIgnoreNullValue(true));
            }
        });
        return res;
    }

    /**
     * 编辑权限
     */
    private boolean getEditPermission(String workspaceId) {
        String userId = LoginContext.currentUserId();
        RoleEnum role = workspaceMemberService.userRole(workspaceId, userId);
        //非普通成员都有编辑权限
        return role != RoleEnum.ROLE_USER;
    }
}
