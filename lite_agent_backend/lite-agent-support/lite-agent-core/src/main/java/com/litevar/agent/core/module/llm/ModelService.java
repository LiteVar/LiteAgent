package com.litevar.agent.core.module.llm;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.litevar.agent.base.constant.CacheKey;
import com.litevar.agent.base.dto.ModelDTO;
import com.litevar.agent.base.entity.*;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.util.LoginContext;
import com.litevar.agent.base.vo.ModelVO;
import com.litevar.agent.core.module.agent.AgentService;
import com.litevar.agent.core.module.workspace.WorkspaceMemberService;
import com.mongoplus.conditions.query.LambdaQueryChainWrapper;
import com.mongoplus.conditions.update.UpdateWrapper;
import com.mongoplus.mapper.BaseMapper;
import com.mongoplus.model.PageResult;
import com.mongoplus.service.impl.ServiceImpl;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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
    @Autowired
    private Validator validator;

    @Cacheable(value = CacheKey.MODEL_INFO, key = "#id", unless = "#result == null")
    public LlmModel findById(String id) {
        return Optional.ofNullable(this.getById(id)).orElseThrow();
    }

    @Cacheable(value = CacheKey.MODEL_INFO, key = "#id", unless = "#result == null")
    public LlmModel findByIdNullable(String id) {
        return this.getById(id);
    }

    public LlmModel addModel(String workspaceId, ModelVO modelVO) {
        return createModel(workspaceId, modelVO);
    }

    public LlmModel createModel(String workspaceId, ModelVO modelVO) {
        LlmModel llmModel = new LlmModel();
        BeanUtil.copyProperties(modelVO, llmModel);
        llmModel.setId(null);
        llmModel.setWorkspaceId(workspaceId);
        llmModel.setUserId(LoginContext.currentUserId());

        //别名不能重复
        long count = this.lambdaQuery()
                .eq(LlmModel::getAlias, modelVO.getAlias())
                .eq(LlmModel::getWorkspaceId, workspaceId)
                .count();
        if (count > 0) {
            throw new ServiceException(ServiceExceptionEnum.MODEL_ALIAS_DUPLICATE);
        }

        this.save(llmModel);
        return llmModel;
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
        agentService.update(agentService.lambdaUpdate().set(Agent::getLlmModelId, "").eq(Agent::getLlmModelId, id));
        agentService.update(agentService.lambdaUpdate().set(Agent::getTtsModelId, "").eq(Agent::getTtsModelId, id));
        agentService.update(agentService.lambdaUpdate().set(Agent::getAsrModelId, "").eq(Agent::getAsrModelId, id));

        //知识库的摘要模型
        baseMapper.update(
                new UpdateWrapper<Dataset>()
                        .set(Dataset::getLlmModelId, "")
                        .set(Dataset::getSummaryCollectionName, "")
                        .eq(Dataset::getLlmModelId, id)
                , Dataset.class);
    }

    @CacheEvict(value = CacheKey.MODEL_INFO, key = "#vo.id")
    public void updateModel(ModelVO vo) {
        LlmModel llmModel = proxy().findById(vo.getId());
        if (!llmModel.getUserId().equals(LoginContext.currentUserId())) {
            throw new ServiceException(ServiceExceptionEnum.NO_PERMISSION_OPERATE);
        }
        if (!StrUtil.equals(llmModel.getAlias(), vo.getAlias())) {
            //别名不能重复
            long count = this.lambdaQuery()
                    .eq(LlmModel::getAlias, vo.getAlias())
                    .eq(LlmModel::getWorkspaceId, llmModel.getWorkspaceId())
                    .ne(LlmModel::getId, llmModel.getId())
                    .count();
            if (count > 0) {
                throw new ServiceException(ServiceExceptionEnum.MODEL_ALIAS_DUPLICATE);
            }
        }

        BeanUtil.copyProperties(vo, llmModel);
        this.updateById(llmModel);
    }

    public PageModel<ModelDTO> modelList(String workspaceId, String type, Boolean autoAgent, Integer pageSize, Integer pageNo) {
        LambdaQueryChainWrapper<LlmModel> chain = this.lambdaQuery()
                .eq(LlmModel::getWorkspaceId, workspaceId)
                .eq(StrUtil.isNotBlank(type), LlmModel::getType, type)
                .eq(autoAgent != null, LlmModel::getAutoAgent, autoAgent)
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

    public byte[] exportModel(String id, boolean plainText) {
        LlmModel model = proxy().findById(id);
        return exportModel(model, plainText);
    }

    public byte[] exportModel(LlmModel model, boolean plainText) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", model.getName());
        data.put("alias", model.getAlias());
        data.put("baseUrl", model.getBaseUrl());
        data.put("apiKey", model.getApiKey());
        data.put("type", model.getType());
        data.put("provider", model.getProvider());
        data.put("fieldMapping", model.getFieldMapping());
        data.put("maxTokens", model.getMaxTokens());
        data.put("autoAgent", model.getAutoAgent());
        data.put("toolInvoke", model.getToolInvoke());
        data.put("deepThink", model.getDeepThink());

        if (!plainText) {
            // 敏感信息脱敏处理: 使用占位符
            data.put("apiKey", "{{<APIKEY>}}");
            data.put("baseUrl", "{{<ENDPOINT>}}");
        }

        String jsonStr = JSONUtil.toJsonPrettyStr(data);
        return jsonStr.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 导入模型配置
     */
    public Map<String, String> importModels(String workspaceId, MultipartFile[] files) {
        List<String> errorList = new ArrayList<>();
        Map<String, ModelVO> dataList = new HashMap<>();

        for (MultipartFile file : files) {
            try {
                String fileName = file.getOriginalFilename();
                if (fileName == null || !fileName.endsWith(".json")) {
                    errorList.add(fileName + ": 文件格式不支持，仅支持JSON文件");
                    continue;
                }

                String content = new String(file.getBytes(), StandardCharsets.UTF_8);
                ModelVO modelVO = JSONUtil.toBean(content, ModelVO.class);

                Set<ConstraintViolation<ModelVO>> violations = validator.validate(modelVO);
                if (!violations.isEmpty()) {
                    String violationMessages = violations.stream()
                            .map(ConstraintViolation::getMessage)
                            .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                            .orElse("验证失败");
                    errorList.add(fileName + ": " + violationMessages);
                    continue;
                }
                dataList.put(StrUtil.removeSuffix(fileName, ".json"), modelVO);
            } catch (IOException e) {
                errorList.add(file.getOriginalFilename() + ": 文件读取失败");
            } catch (Exception e) {
                errorList.add(file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        if (!errorList.isEmpty()) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), StrUtil.join("\n", errorList));
        }

        Map<String, String> idMapping = new HashMap<>();
        List<String> nameList = dataList.values().stream().map(ModelVO::getAlias).toList();
        Map<String, String> repeatName = this.lambdaQuery().projectDisplay(LlmModel::getId, LlmModel::getAlias)
                .in(LlmModel::getWorkspaceId, workspaceId).in(LlmModel::getName, nameList).list()
                .stream().collect(Collectors.toMap(LlmModel::getAlias, LlmModel::getId));

        try {
            dataList.forEach((filename, vo) -> {
                String alias = repeatName.get(vo.getAlias());
                if (alias != null) {
                    vo.setAlias(vo.getAlias() + "_1");
                }
                LlmModel model = addModel(workspaceId, vo);
                idMapping.put(filename, model.getId());
            });
        } catch (Exception e) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE.getCode(), "保存过程中发生错误: " + e.getMessage());
        }
        return idMapping;
    }

    private ModelService proxy() {
        return (ModelService) AopContext.currentProxy();
    }
}
