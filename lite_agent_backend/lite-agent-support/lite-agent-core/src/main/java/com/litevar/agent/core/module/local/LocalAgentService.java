package com.litevar.agent.core.module.local;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.LocalAgentInfoDTO;
import com.litevar.agent.base.entity.*;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.enums.ToolSchemaType;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.util.RedisUtil;
import com.litevar.agent.base.vo.LoginUser;
import com.litevar.agent.base.vo.ModelVO;
import com.litevar.agent.base.vo.ToolVO;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.agent.core.module.workspace.WorkspaceMemberService;
import com.mongoplus.service.impl.ServiceImpl;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 本地agent
 *
 * @author uncle
 * @since 2024/11/14 15:00
 */
@Service
public class LocalAgentService extends ServiceImpl<LocalAgent> {
    @Autowired
    private LocalModelService localModelService;
    @Autowired
    private LocalToolService localToolService;
    @Autowired
    private LocalFunctionService localFunctionService;
    @Autowired
    private WorkspaceMemberService workspaceMemberService;

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

            if (ObjectUtil.isNotEmpty(dto.getToolFunctionList())) {
                List<Agent.AgentFunction> functionList = new ArrayList<>();
                dto.getToolFunctionList().forEach(f -> {
                    Agent.AgentFunction function = new Agent.AgentFunction();
                    function.setFunctionId(generateFunctionId(f.getToolId(), f.getRequestMethod(), f.getFunctionName()));
                    function.setMode(f.getMode());
                    functionList.add(function);
                });
                localAgent.setFunctionList(functionList);
            }
            res.add(localAgent);
        });
        if (!res.isEmpty()) {
            this.saveOrUpdateBatch(res, true);
        }
    }

    public void removeAgent(String id) {
        Optional.ofNullable(getById(id)).orElseThrow();
        this.removeById(id);
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
            localModelService.saveOrUpdateBatch(res, true);
        }
    }

    public void removeModel(String id) {
        Optional.ofNullable(localModelService.getById(id)).orElseThrow();
        localModelService.removeById(id);
    }

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
            List<ToolFunction> list = new ArrayList<>();
            if (StrUtil.isNotEmpty(tool.getSchemaStr()) && ObjectUtil.isNotEmpty(tool.getSchemaType())) {
                List<ToolFunction> t = ToolHandleFactory.getParseInstance(ToolSchemaType.of(localTool.getSchemaType()))
                        .parse(localTool.getSchemaStr());
                list.addAll(t);
            }
            if (list.isEmpty()) {
                throw new ServiceException(ServiceExceptionEnum.TOOL_NO_FUNCTION);
            }
            list.forEach(t -> {
                LocalFunction localFunction = BeanUtil.copyProperties(t, LocalFunction.class);
                String functionId = generateFunctionId(localTool.getId(), t.getRequestMethod(), t.getResource());
                localFunction.setId(functionId);
                localFunction.setUuid(me.getUuid());
                localFunction.setToolId(localTool.getId());
                localFunction.setExpireTime(LocalDateTime.now().plusSeconds(ttl));
                functionList.add(localFunction);
            });
        });
        if (!res.isEmpty()) {
            localToolService.saveOrUpdateBatch(res, true);

            Set<String> toolIds = res.stream().map(ToolProvider::getId).collect(Collectors.toSet());
            Set<String> functionIds = localFunctionService.list(localFunctionService.lambdaQuery()
                            .in(ToolFunction::getToolId, toolIds))
                    .stream().map(ToolFunction::getId).collect(Collectors.toSet());
            if (!functionIds.isEmpty()) {
                localFunctionService.removeBatchByIds(functionIds);
            }
            localFunctionService.saveBatch(functionList);
        }
    }

    public void removeTool(String id) {
        Optional.ofNullable(localToolService.getById(id)).orElseThrow();
        localToolService.removeById(id);

        localFunctionService.remove(localFunctionService.lambdaUpdate().eq(ToolFunction::getToolId, id));
    }

    public void clearAll() {
        String uuid = LoginContext.me().getUuid();

        //clear model
        localModelService.remove(localModelService.lambdaUpdate().eq(LocalModel::getUuid, uuid));

        //clear tool function
        localToolService.remove(localToolService.lambdaUpdate().eq(LocalTool::getUuid, uuid));
        localFunctionService.remove(localFunctionService.lambdaUpdate().eq(LocalFunction::getUuid, uuid));

        //clear agent
        this.remove(lambdaUpdate().eq(LocalAgent::getUuid, uuid));
    }

    @Cacheable(value = CacheKey.USER_WORKSPACE_ID, key = "#userId")
    public String userAdminWorkspaceId(String userId) {
        return workspaceMemberService.one(workspaceMemberService.lambdaQuery()
                        .eq(WorkspaceMember::getUserId, userId)
                        .eq(WorkspaceMember::getRole, RoleEnum.ROLE_ADMIN.getCode()))
                .getWorkspaceId();
    }

    /**
     * 本地agent没有functionId,需自定义生成一个唯一的id兼容本系统
     * 生成方法: MD5(工具id+方法请求方式+方法名字)
     */
    private String generateFunctionId(String toolId, String requestMethod, String functionName) {
        return SecureUtil.md5(toolId + requestMethod + functionName);
    }

    private LocalAgentService proxy() {
        return (LocalAgentService) AopContext.currentProxy();
    }
}
