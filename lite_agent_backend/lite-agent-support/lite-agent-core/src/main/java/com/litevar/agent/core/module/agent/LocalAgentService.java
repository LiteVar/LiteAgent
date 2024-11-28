package com.litevar.agent.core.module.agent;

import cn.hutool.core.bean.BeanUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.LocalAgentInfoDTO;
import com.litevar.agent.base.entity.*;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ToolSchemaType;
import com.litevar.agent.base.repository.*;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.LoginUser;
import com.litevar.agent.base.vo.ModelVO;
import com.litevar.agent.base.vo.ToolVO;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 本地agent
 *
 * @author uncle
 * @since 2024/11/14 15:00
 */
@Service
public class LocalAgentService {

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired
    private LocalAgentRepository localAgentRepository;
    @Autowired
    private LocalModelRepository localModelRepository;
    @Autowired
    private LocalToolRepository localToolRepository;
    @Autowired
    private LocalFunctionRepository localFunctionRepository;

    public void agent(List<LocalAgentInfoDTO> data) {
        LoginUser me = LoginContext.me();
        String workspaceId = proxy().userAdminWorkspaceId(me.getId());
        //过期时间(秒)
        Long ttl = RedisUtil.getKeyTtl(String.format(CacheKey.LOGIN_TOKEN, me.getUuid()));
        List<LocalAgent> res = new ArrayList<>();
        data.forEach(dto -> {
            LocalAgent localAgent = BeanUtil.copyProperties(dto, LocalAgent.class);
            localAgent.setUuid(me.getUuid());
            localAgent.setUserId(me.getId());
            localAgent.setWorkspaceId(workspaceId);
            localAgent.setExpireTime(LocalDateTime.now().plusSeconds(ttl));
            res.add(localAgent);
        });
        if (!res.isEmpty()) {
            localAgentRepository.saveAll(res);
        }
    }

    public void removeAgent(String id) {
        localAgentRepository.findById(id).orElseThrow();
        localAgentRepository.deleteById(id);
    }

    public void model(List<ModelVO> data) {
        LoginUser me = LoginContext.me();
        String workspaceId = proxy().userAdminWorkspaceId(me.getId());
        //过期时间(秒)
        Long ttl = RedisUtil.getKeyTtl(String.format(CacheKey.LOGIN_TOKEN, me.getUuid()));
        List<LocalModel> res = new ArrayList<>();
        data.forEach(model -> {
            LocalModel localModel = BeanUtil.copyProperties(model, LocalModel.class);
            localModel.setUuid(me.getUuid());
            localModel.setUserId(me.getId());
            localModel.setWorkspaceId(workspaceId);
            localModel.setExpireTime(LocalDateTime.now().plusSeconds(ttl));
            res.add(localModel);
        });
        if (!res.isEmpty()) {
            localModelRepository.saveAll(res);
        }
    }

    public void removeModel(String id) {
        localModelRepository.findById(id).orElseThrow();
        localModelRepository.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void tool(List<ToolVO> data) {
        LoginUser me = LoginContext.me();
        String workspaceId = proxy().userAdminWorkspaceId(me.getId());
        //过期时间(秒)
        Long ttl = RedisUtil.getKeyTtl(String.format(CacheKey.LOGIN_TOKEN, me.getUuid()));
        List<LocalTool> res = new ArrayList<>();
        List<LocalFunction> functionList = new ArrayList<>();
        data.forEach(tool -> {
            LocalTool localTool = BeanUtil.copyProperties(tool, LocalTool.class);
            localTool.setUuid(me.getUuid());
            localTool.setUserId(me.getId());
            localTool.setWorkspaceId(workspaceId);
            localTool.setExpireTime(LocalDateTime.now().plusSeconds(ttl));
            res.add(localTool);

            List<ToolFunction> list = ToolHandleFactory.getParseInstance(ToolSchemaType.of(localTool.getSchemaType()))
                    .parse(localTool.getSchemaStr());
            list.forEach(t -> {
                LocalFunction localFunction = BeanUtil.copyProperties(t, LocalFunction.class);
                localFunction.setUuid(me.getUuid());
                localFunction.setToolId(localTool.getId());
                localFunction.setExpireTime(LocalDateTime.now().plusSeconds(ttl));
                functionList.add(localFunction);
            });
        });
        if (!res.isEmpty()) {
            localToolRepository.saveAll(res);

            Set<String> toolIds = res.stream().map(ToolProvider::getId).collect(Collectors.toSet());
            BooleanExpression expression = QLocalFunction.localFunction.toolId.in(toolIds);
            Set<String> functionIds = StreamSupport.stream(localFunctionRepository.findAll(expression).spliterator(), false)
                    .map(ToolFunction::getId).collect(Collectors.toSet());
            if (!functionIds.isEmpty()) {
                localFunctionRepository.deleteAllById(functionIds);
            }

            localFunctionRepository.insert(functionList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeTool(String id) {
        localToolRepository.findById(id).orElseThrow();
        localToolRepository.deleteById(id);

        BooleanExpression expression = QLocalFunction.localFunction.toolId.eq(id);
        Set<String> functionIds = StreamSupport.stream(localFunctionRepository.findAll(expression).spliterator(), false)
                .map(ToolFunction::getId).collect(Collectors.toSet());
        if (!functionIds.isEmpty()) {
            localFunctionRepository.deleteAllById(functionIds);
        }
    }

    public void clearAll() {
        LoginUser me = LoginContext.me();

        //clear model
        BooleanExpression modelE = QLocalModel.localModel.uuid.eq(me.getUuid());
        Set<String> modelIds = StreamSupport.stream(localModelRepository.findAll(modelE).spliterator(), false)
                .map(LlmModel::getId).collect(Collectors.toSet());
        if (!modelIds.isEmpty()) {
            localModelRepository.deleteAllById(modelIds);
        }

        //clear tool function
        BooleanExpression toolE = QLocalTool.localTool.uuid.eq(me.getUuid());
        Set<String> toolIds = StreamSupport.stream(localToolRepository.findAll(toolE).spliterator(), false)
                .map(ToolProvider::getId).collect(Collectors.toSet());
        if (!toolIds.isEmpty()) {
            localToolRepository.deleteAllById(toolIds);
            BooleanExpression expression = QLocalFunction.localFunction.uuid.eq(me.getUuid());
            Set<String> functionIds = StreamSupport.stream(localFunctionRepository.findAll(expression).spliterator(), false)
                    .map(ToolFunction::getId).collect(Collectors.toSet());
            if (!functionIds.isEmpty()) {
                localFunctionRepository.deleteAllById(functionIds);
            }
        }

        //clear agent
        BooleanExpression agentE = QLocalAgent.localAgent.uuid.eq(me.getUuid());
        Set<String> agentIds = StreamSupport.stream(localAgentRepository.findAll(agentE).spliterator(), false)
                .map(Agent::getId).collect(Collectors.toSet());
        if (!agentIds.isEmpty()) {
            localAgentRepository.deleteAllById(agentIds);
        }
    }

    public LocalAgent agentById(String id) {
        return localAgentRepository.findById(id).orElse(null);
    }

    public List<LocalAgent> agentByIds(Collection<String> ids) {
        return localAgentRepository.findAllById(ids);
    }

    public List<LocalAgent> agentsByUserId(String userId) {
        return localAgentRepository.findByUserId(userId);
    }

    public LocalModel modelById(String id) {
        return localModelRepository.findById(id).orElse(null);
    }

    public List<LocalTool> toolByIds(List<String> ids) {
        return localToolRepository.findAllById(ids);
    }

    public List<ToolFunction> functionByToolIds(List<String> toolIds) {
        BooleanExpression expression = QLocalFunction.localFunction.toolId.in(toolIds);
        return StreamSupport.stream(localFunctionRepository.findAll(expression).spliterator(), false)
                .map(i -> (ToolFunction) i).toList();
    }

    @Cacheable(value = CacheKey.USER_WORKSPACE_ID, key = "#userId")
    public String userAdminWorkspaceId(String userId) {
        return workspaceMemberRepository.findByUserIdAndRole(userId, RoleEnum.ROLE_ADMIN.getCode())
                .get(0).getWorkspaceId();
    }

    private LocalAgentService proxy() {
        return (LocalAgentService) AopContext.currentProxy();
    }
}
