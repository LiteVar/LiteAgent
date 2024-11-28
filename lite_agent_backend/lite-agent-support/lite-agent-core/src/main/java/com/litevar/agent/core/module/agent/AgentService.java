package com.litevar.agent.core.module.agent;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.AgentDTO;
import com.litevar.agent.base.dto.ToolDTO;
import com.litevar.agent.base.entity.*;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.repository.AgentRepository;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.AgentCreateForm;
import com.litevar.agent.base.vo.AgentDetailVO;
import com.litevar.agent.base.vo.AgentUpdateForm;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.core.module.workspace.WorkspaceMemberService;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author reid
 * @since 2024/8/12
 */
@Service
public class AgentService {
    @Autowired
    private AgentRepository repository;
    @Autowired
    private ToolService toolService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private WorkspaceMemberService workspaceMemberService;
    @Autowired
    private LocalAgentService localAgentService;

    public Agent findById(String id) {
        return repository.findById(id).orElseThrow();
    }

    public List<Agent> findByIds(Collection<String> ids) {
        return repository.findAllById(ids);
    }

    public AgentDetailVO info(String agentId) {
        AgentDetailVO vo = new AgentDetailVO();

        Agent agent = repository.findById(agentId).orElseThrow();
        vo.setAgent(agent);

        if (StrUtil.isNotBlank(agent.getLlmModelId())) {
            LlmModel model = modelService.getModelById(agent.getLlmModelId(), agent.getUserId());
            vo.setModel(model);
        }
        if (ObjectUtil.isNotEmpty(agent.getToolIds())) {
            // 只返回能用的tool
            List<ToolProvider> list = toolService.findByIdsWithPermission(agent.getToolIds());
            List<ToolDTO> toolList = BeanUtil.copyToList(list, ToolDTO.class);
            vo.setToolList(toolList);
        }

        return vo;
    }

    public AgentDetailVO adminInfo(String agentId) {
        AgentDetailVO vo = new AgentDetailVO();

        Agent agent = repository.findById(agentId).orElseThrow();
        if (StrUtil.equals(agent.getUserId(), LoginContext.currentUserId())) {
            //为创建者,查缓存草稿
            Agent cache = (Agent) RedisUtil.getValue(String.format(CacheKey.AGENT_DRAFT, agentId));
            if (cache != null) {
                agent = cache;
                vo.setCanRelease(true);
            }
        }
        vo.setAgent(agent);
        if (ObjectUtil.isNotEmpty(agent.getToolIds())) {
            List<ToolProvider> list = toolService.findByIds(agent.getToolIds());
            List<String> toolIds = list.stream().map(ToolProvider::getId).toList();
            vo.getAgent().setToolIds(toolIds);

            List<ToolDTO> toolList = new ArrayList<>();
            for (ToolProvider toolProvider : list) {
                ToolDTO dto = BeanUtil.copyProperties(toolProvider, ToolDTO.class);
                dto.setCanRead(toolProvider.getShareFlag() || StrUtil.equals(toolProvider.getUserId(), LoginContext.currentUserId()));
                toolList.add(dto);
            }
            vo.setToolList(toolList);
        }

        if (StrUtil.isNotBlank(agent.getLlmModelId())) {
            LlmModel model = modelService.getModelById(agent.getLlmModelId(), agent.getUserId());
            vo.setModel(model);
            if (model == null) {
                vo.getAgent().setLlmModelId("");
            }
        }

        vo.setCanDelete(getDeletePermission(agent, agent.getWorkspaceId()));
        vo.setCanEdit(getEditPermission(agent.getUserId(), agent.getWorkspaceId()));

        return vo;
    }

    @Transactional
    public Agent addAgent(String workspaceId, AgentCreateForm form) {
        Agent agent = new Agent();
        BeanUtil.copyProperties(form, agent, CopyOptions.create().setIgnoreNullValue(true));
        agent.setWorkspaceId(workspaceId);
        agent.setUserId(LoginContext.currentUserId());
        repository.save(agent);

        return agent;
    }

    @Transactional
    public void removeAgent(String id) {
        Agent agent = repository.findById(id).orElseThrow();
        if (!getDeletePermission(agent, agent.getWorkspaceId())) {
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION_OPERATE);
        }
        repository.deleteById(id);
    }

    @Transactional
    public Agent updateAgent(String id, AgentUpdateForm form) {
        Agent agent = repository.findById(id).orElseThrow();
        if (!getEditPermission(agent.getUserId(), agent.getWorkspaceId())) {
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION_OPERATE);
        }
        if (ObjectUtil.isNotEmpty(form.getMaxTokens()) && StrUtil.isNotEmpty(form.getLlmModelId())) {
            LlmModel model = modelService.findById(form.getLlmModelId());
            Integer modelMaxToken = (ObjectUtil.isNotEmpty(model) && ObjectUtil.isNotEmpty(model.getMaxTokens())) ?
                    model.getMaxTokens() : 4096;
            if (form.getMaxTokens() > modelMaxToken) {
                throw new ServiceException(ServiceExceptionEnum.MAX_TOKEN_LARGER);
            }
        }
        BeanUtil.copyProperties(form, agent, CopyOptions.create().setIgnoreNullValue(true));

        RedisUtil.setValue(String.format(CacheKey.AGENT_DRAFT, id), agent);

        return agent;
    }

    @Transactional
    public void enableShare(String id) {
        Agent agent = repository.findById(id).orElseThrow();
        if (!getEditPermission(agent.getUserId(), agent.getWorkspaceId())) {
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION_OPERATE);
        }
        Boolean flag = agent.getShareFlag();
        agent.setShareFlag(!flag);
        repository.save(agent);
    }

    public void release(String id) {
        Agent value = (Agent) RedisUtil.getValue(String.format(CacheKey.AGENT_DRAFT, id));
        if (value == null) {
            throw new ServiceException(30001, "请先保存信息再发布");
        }

        if (!getEditPermission(value.getUserId(), value.getWorkspaceId())) {
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION_OPERATE);
        }

        value.setStatus(1);

        repository.save(value);

        RedisUtil.delKey(String.format(CacheKey.AGENT_DRAFT, id));
    }

    public List<AgentDTO> agentList(String workspaceId, Integer tab, String agentName, Integer status) {
        if (tab == 1) {
            //系统: 系统预置,暂为空
            return Collections.emptyList();
        }

        QAgent qAgent = QAgent.agent;
        String userId = LoginContext.currentUserId();
        List<AgentDTO> res = new ArrayList<>();

        if (tab == 4) {
            //本地agent
            String spaceId = localAgentService.userAdminWorkspaceId(userId);
            if (StrUtil.equals(spaceId, workspaceId)) {
                List<LocalAgent> list = localAgentService.agentsByUserId(userId);
                for (LocalAgent agent : list) {
                    AgentDTO agentDTO = new AgentDTO();
                    BeanUtil.copyProperties(agent, agentDTO);
                    res.add(agentDTO);
                }

            } else {
                return Collections.emptyList();
            }
        } else {
            BooleanExpression expression = qAgent.workspaceId.eq(workspaceId);
            if (ObjectUtil.isNotEmpty(status)) {
                expression = expression.and(qAgent.status.eq(status));
            }
            if (StrUtil.isNotBlank(agentName)) {
                expression = expression.and(qAgent.name.like(agentName));
            }
            if (tab == 0) {
                //全部:工作空间内分享的,和自己创建的
                expression = expression.and(qAgent.shareFlag.isTrue().or(qAgent.userId.eq(userId)));

            } else if (tab == 2) {
                //来自分享: 管理员,开发者分享的
                expression = expression.and(qAgent.shareFlag.isTrue());

            } else if (tab == 3) {
                //自己创建的
                expression = expression.and(qAgent.userId.eq(userId));
            }
            Iterable<Agent> list = repository.findAll(expression, Sort.by(Sort.Direction.DESC, "createTime"));
            for (Agent agent : list) {
                AgentDTO dto = BeanUtil.copyProperties(agent, AgentDTO.class);
                dto.setShareTip(agent.getUserId().equals(userId) && agent.getShareFlag());
                res.add(dto);
            }
        }
        return res;
    }

    public List<AgentDTO> agentAdminList(String workspaceId, Integer tab, String agentName) {
        List<AgentDTO> res = agentList(workspaceId, tab, agentName, null);
        String userId = LoginContext.currentUserId();
        //如果agent是自己的,查出缓存的版本
        res.forEach(dto -> {
            if (StrUtil.equals(dto.getUserId(), userId)) {
                Object value = RedisUtil.getValue(String.format(CacheKey.AGENT_DRAFT, dto.getId()));
                if (value != null) {
                    BeanUtil.copyProperties(value, dto, CopyOptions.create().setIgnoreNullValue(true));
                }
            }
        });
        return res;
    }

    /**
     * 编辑权限
     */
    private boolean getEditPermission(String creatorId, String workspaceId) {
        String userId = LoginContext.currentUserId();
        RoleEnum role = workspaceMemberService.userRole(workspaceId, userId);
        //谁创建谁有权限编辑,并且普通成员没有权限修改
        return role != RoleEnum.ROLE_USER && StrUtil.equals(creatorId, LoginContext.currentUserId());
    }

    /**
     * 删除权限
     */
    private boolean getDeletePermission(Agent agent, String workspaceId) {
        String userId = LoginContext.currentUserId();
        RoleEnum role = workspaceMemberService.userRole(workspaceId, userId);

        //管理员: 可以删除自己及他人分享的模型
        //开发者:只能删除自己创建的模型
        return role == RoleEnum.ROLE_ADMIN
                || (role == RoleEnum.ROLE_DEVELOPER && StrUtil.equals(agent.getUserId(), userId));
    }
}
