package com.litevar.agent.core.module.tool;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.ToolDTO;
import com.litevar.agent.base.entity.QToolProvider;
import com.litevar.agent.base.entity.ToolFunction;
import com.litevar.agent.base.entity.ToolProvider;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.enums.ToolSchemaType;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.repository.ToolFunctionRepository;
import com.litevar.agent.base.repository.ToolProviderRepository;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.vo.ToolVO;
import com.litevar.agent.core.module.tool.parser.ToolParser;
import com.litevar.agent.core.module.workspace.WorkspaceMemberService;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author uncle
 * @since 2024/8/13 17:34
 */
@Service
public class ToolService {

    @Autowired
    private ToolProviderRepository toolProviderRepository;
    @Autowired
    private WorkspaceMemberService workspaceMemberService;
    @Autowired
    private ToolFunctionRepository toolFunctionRepository;

    @Cacheable(value = CacheKey.TOOL_INFO, key = "#id", unless = "#result == null")
    public ToolProvider findById(String id) {
        if (StrUtil.isEmpty(id)) {
            return null;
        }
        return toolProviderRepository.findById(id).orElse(null);
    }

    public List<ToolProvider> findByIds(List<String> ids) {
        if (ObjectUtil.isNotEmpty(ids)) {
            return toolProviderRepository.findAllById(ids);
        }
        return Collections.emptyList();
    }

    /**
     * 只返回能用的tool
     */
    public List<ToolProvider> findByIdsWithPermission(List<String> ids) {
        List<ToolProvider> list = new ArrayList<>();
        ids.forEach(id -> {
            ToolProvider tool = proxy().findById(id);
            if (tool != null && getReadPermission(tool)) {
                list.add(tool);
            }
        });
        return list;
    }

    @Transactional(rollbackFor = Exception.class)
    public void addTool(ToolVO vo, String workspaceId) {
        ToolProvider tool = toolProviderRepository.findByWorkspaceIdAndName(workspaceId, vo.getName());
        if (tool != null) {
            throw new ServiceException(ServiceExceptionEnum.NAME_DUPLICATE);
        }
        tool = new ToolProvider();
        BeanUtil.copyProperties(vo, tool);
        tool.setWorkspaceId(workspaceId);
        tool.setUserId(LoginContext.currentUserId());

        ToolParser parser = ToolHandleFactory.getParseInstance(ToolSchemaType.of(tool.getSchemaType()));
        List<ToolFunction> functionList = parser.parse(tool.getSchemaStr());

        String id = toolProviderRepository.insert(tool).getId();
        functionList.forEach(i -> i.setToolId(id));
        toolFunctionRepository.insert(functionList);
    }

    @CacheEvict(value = CacheKey.TOOL_INFO, key = "#vo.id")
    @Transactional(rollbackFor = Exception.class)
    public void updateTool(ToolVO vo) {
        ToolProvider tool = Optional.ofNullable(proxy().findById(vo.getId())).orElseThrow();

        if (!getEditPermission(tool.getUserId(), tool.getWorkspaceId())) {
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION_OPERATE);
        }

        if (!tool.getName().equals(vo.getName())) {
            ToolProvider dbTool = toolProviderRepository.findByWorkspaceIdAndName(tool.getWorkspaceId(), vo.getName());
            if (dbTool != null) {
                throw new ServiceException(ServiceExceptionEnum.NAME_DUPLICATE);
            }
        }
        BeanUtil.copyProperties(vo, tool);

        ToolParser parser = ToolHandleFactory.getParseInstance(ToolSchemaType.of(tool.getSchemaType()));
        List<ToolFunction> functionList = parser.parse(tool.getSchemaStr());
        functionList.forEach(i -> i.setToolId(tool.getId()));
        List<String> ids = getFunctionList(List.of(tool.getId())).stream().map(ToolFunction::getId).toList();
        if (!ids.isEmpty()) {
            toolFunctionRepository.deleteAllById(ids);
        }
        toolFunctionRepository.insert(functionList);

        toolProviderRepository.save(tool);
    }

    @CacheEvict(value = CacheKey.TOOL_INFO, key = "#id")
    @Transactional(rollbackFor = Exception.class)
    public void deleteTool(String id) {
        ToolProvider tool = Optional.ofNullable(proxy().findById(id)).orElseThrow();
        if (!getDeletePermission(tool, tool.getWorkspaceId())) {
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION_OPERATE);
        }
        toolProviderRepository.deleteById(id);

        //delete function
        List<ToolFunction> functionList = getFunctionList(List.of(id));
        List<String> functionIds = functionList.stream().map(ToolFunction::getId).toList();
        toolFunctionRepository.deleteAllById(functionIds);
    }

    public List<ToolDTO> toolList(String workspaceId, String name, Integer tab) {
        if (tab == 1) {
            //系统: 系统预置,暂为空
            return Collections.emptyList();
        }

        String userId = LoginContext.currentUserId();

        QToolProvider qToolProvider = QToolProvider.toolProvider;
        BooleanExpression expression = qToolProvider.workspaceId.eq(workspaceId);

        if (StrUtil.isNotEmpty(name)) {
            expression = expression.and(qToolProvider.name.like(name));
        }
        if (tab == 0) {
            //全部:工作空间内分享的,和自己创建的
            expression = expression.and(qToolProvider.shareFlag.isTrue().or(qToolProvider.userId.eq(userId)));

        } else if (tab == 2) {
            //来自分享: 管理员、开发者分享的
            expression = expression.and(qToolProvider.shareFlag.isTrue());

        } else {
            //我的: 自己创建的
            expression = expression.and(qToolProvider.userId.eq(userId));
        }

        Iterable<ToolProvider> list = toolProviderRepository.findAll(expression, Sort.by(Sort.Direction.DESC, "createTime"));
        List<ToolDTO> res = new ArrayList<>();
        for (ToolProvider toolProvider : list) {
            ToolDTO dto = BeanUtil.copyProperties(toolProvider, ToolDTO.class);
            dto.setShareTip(toolProvider.getUserId().equals(userId) && toolProvider.getShareFlag());
            //权限
            dto.setCanEdit(getEditPermission(toolProvider.getUserId(), toolProvider.getWorkspaceId()));
            dto.setCanDelete(getDeletePermission(toolProvider, workspaceId));
            res.add(dto);
        }

        return res;
    }

    public List<ToolFunction> getFunctionList(List<String> toolIds) {
        return toolFunctionRepository.findByToolIdIn(toolIds);
    }

    /**
     * 删除权限
     */
    private boolean getDeletePermission(ToolProvider tool, String workspaceId) {
        String userId = LoginContext.currentUserId();
        RoleEnum role = workspaceMemberService.userRole(workspaceId, userId);

        //管理员: 可以删除自己及他人分享的工具
        //开发者:只能删除自己创建的工具
        return role == RoleEnum.ROLE_ADMIN
                || (role == RoleEnum.ROLE_DEVELOPER && StrUtil.equals(tool.getUserId(), userId));
    }

    /**
     * 编辑权限
     */
    private boolean getEditPermission(String creatorId, String workspaceId) {
        //谁创建谁有权限编辑,并且普通成员没有权限修改
        String userId = LoginContext.currentUserId();
        RoleEnum role = workspaceMemberService.userRole(workspaceId, userId);

        return role != RoleEnum.ROLE_USER && StrUtil.equals(creatorId, LoginContext.currentUserId());
    }

    private boolean getReadPermission(ToolProvider tool) {
        String userId = LoginContext.currentUserId();
        return StrUtil.equals(userId, tool.getUserId())
                || (!StrUtil.equals(userId, tool.getUserId()) && tool.getShareFlag());
    }

    private ToolService proxy() {
        return (ToolService) AopContext.currentProxy();
    }
}
