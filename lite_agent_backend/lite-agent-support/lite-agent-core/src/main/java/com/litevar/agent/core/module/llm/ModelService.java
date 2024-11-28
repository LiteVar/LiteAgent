package com.litevar.agent.core.module.llm;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.ModelDTO;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.entity.QLlmModel;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.repository.AgentRepository;
import com.litevar.agent.base.repository.LlmModelRepository;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.vo.ModelVO;
import com.litevar.agent.core.module.workspace.WorkspaceMemberService;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author uncle
 * @since 2024/8/9 10:08
 */
@Service
public class ModelService {

    @Autowired
    private LlmModelRepository llmModelRepository;
    @Autowired
    private AgentRepository agentRepository;
    @Autowired
    private WorkspaceMemberService workspaceMemberService;

    @Cacheable(value = CacheKey.MODEL_INFO, key = "#id", unless = "#result == null")
    public LlmModel findById(String id) {
        return llmModelRepository.findById(id).orElse(null);
    }

    /**
     * 传入一个userId,查询该用户是否有使用权限
     * 如果模型被取消分享,返回null
     */
    public LlmModel getModelById(String id, String userId) {
        if (StrUtil.isNotEmpty(id)) {
            LlmModel model = proxy().findById(id);
            if (model != null) {
                return getReadPermission(model, userId) ? model : null;
            }
        }
        return null;
    }

    public void addModel(String workspaceId, ModelVO modelVO) {
        LlmModel llmModel = new LlmModel();
        BeanUtil.copyProperties(modelVO, llmModel);
        llmModel.setId(null);
        llmModel.setWorkspaceId(workspaceId);
        llmModel.setUserId(LoginContext.currentUserId());

        llmModelRepository.save(llmModel);
    }

    @CacheEvict(value = CacheKey.MODEL_INFO, key = "#id")
    @Transactional(rollbackFor = Exception.class)
    public void removeModel(String id) {
        LlmModel llmModel = proxy().findById(id);
        Optional.ofNullable(llmModel).orElseThrow();
        Map<String, Boolean> permission = getDeletePermission(List.of(llmModel), llmModel.getWorkspaceId());
        Boolean flag = permission.get(id);
        if (!flag) {
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION_OPERATE);
        }
        llmModelRepository.deleteById(id);

        //agent中有引用到模型的数据,置空
        List<Agent> agentList = agentRepository.findByLlmModelId(id);
        if (!agentList.isEmpty()) {
            agentList.forEach(agent -> agent.setLlmModelId(""));
            agentRepository.saveAll(agentList);
        }
    }

    @CacheEvict(value = CacheKey.MODEL_INFO, key = "#vo.id")
    public void updateModel(ModelVO vo) {
        LlmModel llmModel = proxy().findById(vo.getId());
        Optional.ofNullable(llmModel).orElseThrow();

        if (!getEditPermission(llmModel.getUserId(), llmModel.getWorkspaceId())) {
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION_OPERATE);
        }

        BeanUtil.copyProperties(vo, llmModel);

        llmModelRepository.save(llmModel);
    }

    public PageModel<ModelDTO> modelList(String workspaceId, PageRequest page) {
        QLlmModel qLlmModel = QLlmModel.llmModel;
        BooleanExpression share = qLlmModel.workspaceId.eq(workspaceId).and(qLlmModel.shareFlag.isTrue());
        BooleanExpression me = qLlmModel.workspaceId.eq(workspaceId).and(qLlmModel.userId.eq(LoginContext.currentUserId()));

        BooleanExpression expression = me.or(share);

        Page<LlmModel> all = llmModelRepository.findAll(expression, page);
        List<LlmModel> list = all.getContent();
        List<ModelDTO> res = BeanUtil.copyToList(list, ModelDTO.class);
        Map<String, Boolean> permission = getDeletePermission(list, workspaceId);
        res.forEach(dto -> {
            dto.setCanDelete(permission.get(dto.getId()));
            dto.setCanEdit(getEditPermission(dto.getUserId(), workspaceId));
        });

        return new PageModel<>(page.getPageNumber(), page.getPageSize(), all.getTotalElements(), res);
    }

    /**
     * 删除权限
     */
    private Map<String, Boolean> getDeletePermission(List<LlmModel> modelList, String workspaceId) {
        String userId = LoginContext.currentUserId();
        RoleEnum role = workspaceMemberService.userRole(workspaceId, userId);

        Map<String, Boolean> permission = new HashMap<>();
        //管理员: 可以删除自己及他人分享的模型
        //开发者:只能删除自己创建的模型
        modelList.forEach(model ->
                permission.put(model.getId(), role == RoleEnum.ROLE_ADMIN
                        || (role == RoleEnum.ROLE_DEVELOPER && StrUtil.equals(model.getUserId(), userId))));
        return permission;
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
     * 读权限
     */
    private boolean getReadPermission(LlmModel model, String userId) {
        return StrUtil.equals(userId, model.getUserId())
                || (!StrUtil.equals(userId, model.getUserId()) && model.getShareFlag());
    }

    private ModelService proxy() {
        return (ModelService) AopContext.currentProxy();
    }
}
