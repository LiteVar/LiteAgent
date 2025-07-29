package com.litevar.agent.core.module.tool;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.ToolDTO;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.entity.ToolProvider;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.enums.ToolSchemaType;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.vo.FunctionVO;
import com.litevar.agent.base.vo.ToolVO;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.core.module.tool.parser.ToolParser;
import com.litevar.agent.core.module.workspace.WorkspaceMemberService;
import com.mongoplus.service.impl.ServiceImpl;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author uncle
 * @since 2024/8/13 17:34
 */
@Service
public class ToolService extends ServiceImpl<ToolProvider> {
    @Autowired
    private WorkspaceMemberService workspaceMemberService;
    @Autowired
    private ToolFunctionService toolFunctionService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private CacheManager cacheManager;

    @Cacheable(value = CacheKey.TOOL_INFO, key = "#id", unless = "#result == null")
    public ToolProvider findById(String id) {
        if (StrUtil.isEmpty(id)) {
            return null;
        }
        return Optional.ofNullable(this.getById(id)).orElseThrow();
    }

    @Cacheable(value = CacheKey.TOOL_API_KEY_INFO, key = "#id", unless = "#result == null")
    public String toolApiKey(String id) {
        ToolProvider tool = proxy().findById(id);
        String key = null;
        if (StrUtil.isNotBlank(tool.getApiKey())) {
            String keyType = StrUtil.isNotEmpty(tool.getApiKeyType()) ? tool.getApiKeyType() : "";
            key = (keyType + " " + tool.getApiKey()).trim();
        }
        return key;
    }

    public void addTool(ToolVO vo, String workspaceId) {
        ToolProvider tool = this.one(lambdaQuery()
                .projectDisplay(ToolProvider::getId)
                .eq(ToolProvider::getWorkspaceId, workspaceId)
                .eq(ToolProvider::getName, vo.getName()));
        if (tool != null) {
            throw new ServiceException(ServiceExceptionEnum.NAME_DUPLICATE);
        }
        tool = new ToolProvider();
        BeanUtil.copyProperties(vo, tool);
        tool.setWorkspaceId(workspaceId);
        tool.setUserId(LoginContext.currentUserId());
        List<ToolFunction> functionList = parseFunctionFromTool(tool);

        this.save(tool);
        String id = tool.getId();
        functionList.forEach(i -> i.setToolId(id));
        toolFunctionService.saveBatch(functionList);
    }

    @CacheEvict(value = {CacheKey.TOOL_INFO, CacheKey.TOOL_API_KEY_INFO}, key = "#vo.id")
    public void updateTool(ToolVO vo) {
        ToolProvider tool = Optional.ofNullable(proxy().findById(vo.getId())).orElseThrow();

        if (!tool.getName().equals(vo.getName())) {
            ToolProvider dbTool = this.one(lambdaQuery()
                    .eq(ToolProvider::getWorkspaceId, tool.getWorkspaceId())
                    .eq(ToolProvider::getName, vo.getName()));
            if (dbTool != null) {
                throw new ServiceException(ServiceExceptionEnum.NAME_DUPLICATE);
            }
        }
        BeanUtil.copyProperties(vo, tool);

        List<ToolFunction> functionList = parseFunctionFromTool(tool);

        //以requestMethod(post,get) resource(/abc) 来区分
        Map<String, String> originFunction = getFunctionList(List.of(tool.getId())).stream()
                .collect(Collectors.toMap(i -> i.getRequestMethod() + i.getResource(), ToolFunction::getId));

        List<ToolFunction> updateList = new ArrayList<>();
        List<ToolFunction> insertList = new ArrayList<>();
        functionList.forEach(f -> {
            String key = f.getRequestMethod() + f.getResource();
            f.setToolId(tool.getId());
            String id = originFunction.get(key);
            if (StrUtil.isNotEmpty(id)) {
                f.setId(id);
                updateList.add(f);
                originFunction.remove(key);
            } else {
                insertList.add(f);
            }
        });
        if (!updateList.isEmpty()) {
            toolFunctionService.updateBatchByIds(updateList);
            Cache cache = cacheManager.getCache(CacheKey.TOOL_FUNCTION_INFO);
            if (cache != null) {
                updateList.forEach(i -> cache.evict(i.getId()));
            }
        }
        if (!insertList.isEmpty()) {
            toolFunctionService.saveBatch(insertList);
        }
        if (!originFunction.isEmpty()) {
            Set<String> ids = new HashSet<>(originFunction.values());
            toolFunctionService.removeBatchByIds(ids);
            Cache cache = cacheManager.getCache(CacheKey.TOOL_FUNCTION_INFO);
            if (cache != null) {
                ids.forEach(cache::evict);
            }
        }
        this.updateById(tool);
    }

    public List<ToolFunction> parseFunctionFromTool(ToolProvider tool) {
        List<ToolFunction> functionList = new ArrayList<>();
        if (StrUtil.isNotEmpty(tool.getSchemaStr()) && ObjectUtil.isNotEmpty(tool.getSchemaType())) {
            ToolParser parser = ToolHandleFactory.getParseInstance(ToolSchemaType.of(tool.getSchemaType()));
            functionList.addAll(parser.parse(tool.getSchemaStr()));
        }
        if (functionList.isEmpty()) {
            throw new ServiceException(ServiceExceptionEnum.TOOL_NO_FUNCTION);
        }
        return functionList;
    }

    @CacheEvict(value = {CacheKey.TOOL_INFO, CacheKey.TOOL_API_KEY_INFO}, key = "#id")
    public void deleteTool(String id) {
        this.removeById(id);

        //delete function
        List<ToolFunction> functionList = getFunctionList(List.of(id));
        List<String> functionIds = functionList.stream().map(ToolFunction::getId).toList();
        toolFunctionService.removeBatchByIds(functionIds);
    }

    public List<ToolDTO> toolList(String workspaceId, String name, Integer tab, Boolean autoAgent) {
        if (tab == 1) {
            //系统: 系统预置,暂为空
            return Collections.emptyList();
        }

        String userId = LoginContext.currentUserId();
        List<ToolProvider> list = this.lambdaQuery()
                .projectNone(ToolProvider::getSchemaStr)
                .eq(ToolProvider::getWorkspaceId, workspaceId)
                .eq(tab == 3, ToolProvider::getUserId, userId)
                .like(StrUtil.isNotEmpty(name), ToolProvider::getName, name)
                .eq(autoAgent != null, ToolProvider::getAutoAgent, autoAgent)
                .orderByDesc(ToolProvider::getCreateTime)
                .list();

        List<ToolDTO> res = new ArrayList<>();
        for (ToolProvider toolProvider : list) {
            ToolDTO dto = BeanUtil.copyProperties(toolProvider, ToolDTO.class);
            dto.setCreateUser(modelService.userInfo(toolProvider.getUserId()).getName());
            //权限
            dto.setCanEdit(getEditPermission(toolProvider.getWorkspaceId()));
            dto.setCanDelete(getEditPermission(toolProvider.getWorkspaceId()));
            res.add(dto);
        }

        return res;
    }

    public List<ToolDTO> toolList(String workspaceId, Integer tab, Boolean autoAgent) {
        List<ToolDTO> res = toolList(workspaceId, null, tab, autoAgent);
        if (ObjectUtil.isEmpty(res)) {
            return res;
        }

        List<String> toolIds = res.stream().map(ToolDTO::getId).toList();
        Map<String, List<ToolFunction>> functionMap = toolFunctionService.list(toolFunctionService.lambdaQuery()
                        .projectDisplay(ToolFunction::getId, ToolFunction::getResource, ToolFunction::getToolId,
                                ToolFunction::getDescription, ToolFunction::getRequestMethod)
                        .in(ToolFunction::getToolId, toolIds))
                .stream().collect(Collectors.groupingBy(ToolFunction::getToolId));

        res.forEach(tool -> {
            List<ToolFunction> functionList = functionMap.get(tool.getId());
            List<FunctionVO> functionVOS = new ArrayList<>();
            if (ObjectUtil.isNotEmpty(functionList)) {
                functionList.forEach(f -> {
                    FunctionVO vo = new FunctionVO();
                    vo.setFunctionName(f.getResource());
                    vo.setFunctionId(f.getId());
                    vo.setFunctionDesc(f.getDescription());
                    vo.setRequestMethod(f.getRequestMethod());
                    functionVOS.add(vo);
                });
            }
            tool.setFunctionList(functionVOS);
        });
        return res;
    }

    public List<ToolFunction> getFunctionList(List<String> toolIds) {
        return toolFunctionService.list(toolFunctionService.lambdaQuery().in(ToolFunction::getToolId, toolIds));
    }

    /**
     * 编辑权限
     */
    private boolean getEditPermission(String workspaceId) {
        //非普通成员都有编辑权限
        String userId = LoginContext.currentUserId();
        RoleEnum role = workspaceMemberService.userRole(workspaceId, userId);
        return role != RoleEnum.ROLE_USER;
    }

    private ToolService proxy() {
        return (ToolService) AopContext.currentProxy();
    }
}
