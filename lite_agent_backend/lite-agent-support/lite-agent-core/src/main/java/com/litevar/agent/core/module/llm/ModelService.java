package com.litevar.agent.core.module.llm;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.ModelDTO;
import com.litevar.agent.base.entity.Account;
import com.litevar.agent.base.entity.Agent;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.vo.ModelVO;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.core.module.workspace.WorkspaceMemberService;
import com.mongoplus.conditions.query.LambdaQueryChainWrapper;
import com.mongoplus.mapper.BaseMapper;
import com.mongoplus.model.PageResult;
import com.mongoplus.service.impl.ServiceImpl;
import com.mongoplus.support.SFunction;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author uncle
 * @since 2024/8/9 10:08
 */
@Service
public class ModelService extends ServiceImpl<LlmModel> {
    @Autowired
    private WorkspaceMemberService workspaceMemberService;
    @Lazy
    @Autowired
    private AgentService agentService;
    @Autowired
    private BaseMapper baseMapper;

    @Cacheable(value = CacheKey.MODEL_INFO, key = "#id", unless = "#result == null")
    public LlmModel findById(String id) {
        return Optional.ofNullable(this.getById(id)).orElseThrow();
    }

    public void addModel(String workspaceId, ModelVO modelVO) {
        LlmModel llmModel = new LlmModel();
        BeanUtil.copyProperties(modelVO, llmModel);
        llmModel.setId(null);
        llmModel.setWorkspaceId(workspaceId);
        llmModel.setUserId(LoginContext.currentUserId());

        this.save(llmModel);
    }

    @CacheEvict(value = CacheKey.MODEL_INFO, key = "#id")
    public void removeModel(String id) {
        LlmModel llmModel = proxy().findById(id);
        Optional.ofNullable(llmModel).orElseThrow();

        if (!llmModel.getUserId().equals(LoginContext.currentUserId())) {
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION_OPERATE);
        }

        this.removeById(id);

        //agent中有引用到模型的数据,置空
        SFunction<Agent, String> getLlmModelId = Agent::getLlmModelId;
        List<Agent> agentList = baseMapper.getByColumn(getLlmModelId.getFieldNameLine(), id, Agent.class);

        if (!agentList.isEmpty()) {
            baseMapper.update(agentService.lambdaUpdate()
                    .set(Agent::getLlmModelId, "")
                    .eq(Agent::getLlmModelId, id), Agent.class);
        }
    }

    @CacheEvict(value = CacheKey.MODEL_INFO, key = "#vo.id")
    public void updateModel(ModelVO vo) {
        LlmModel llmModel = proxy().findById(vo.getId());
        Optional.ofNullable(llmModel).orElseThrow();

        if (!llmModel.getUserId().equals(LoginContext.currentUserId())) {
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION_OPERATE);
        }

        BeanUtil.copyProperties(vo, llmModel);
        this.updateById(llmModel);
    }

    public PageModel<ModelDTO> modelList(String workspaceId, String type, Integer pageSize, Integer pageNo) {
        LambdaQueryChainWrapper<LlmModel> chain = this.lambdaQuery()
                .eq(LlmModel::getWorkspaceId, workspaceId)
                .eq(StrUtil.isNotBlank(type), LlmModel::getType, type)
                .orderByDesc(LlmModel::getCreateTime);

        PageResult<LlmModel> all = this.page(chain, pageNo, pageSize);

        List<LlmModel> list = all.getContentData();
        List<ModelDTO> res = BeanUtil.copyToList(list, ModelDTO.class);
        res.forEach(dto -> {
            dto.setCanDelete(getEditPermission(dto.getUserId(), workspaceId));
            dto.setCanEdit(getEditPermission(dto.getUserId(), workspaceId));
            dto.setCreateUser(proxy().userInfo(dto.getUserId()).getName());
        });

        return new PageModel<>(pageNo, pageSize, all.getTotalSize(), res);
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

    @Cacheable(value = CacheKey.USER_INFO, key = "#id")
    public Account userInfo(String id) {
        return Optional.ofNullable(baseMapper.getById(id, Account.class)).orElseThrow();
    }

    private ModelService proxy() {
        return (ModelService) AopContext.currentProxy();
    }
}
