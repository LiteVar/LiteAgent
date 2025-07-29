package com.litevar.agent.rest.controller.v1;

import cn.hutool.core.lang.Validator;
import com.litevar.agent.auth.annotation.WorkspaceRole;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.dto.ModelDTO;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.enums.AiProvider;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.valid.AddAction;
import com.litevar.agent.base.valid.UpdateAction;
import com.litevar.agent.base.vo.ModelVO;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.rest.service.DatasetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 模型管理
 *
 * @author uncle
 * @since 2024/8/8 11:04
 */
@RestController
@RequestMapping("/v1/model")
public class ModelController {

    @Autowired
    private ModelService modelService;
    @Autowired
    private DatasetService datasetService;

    /**
     * 新建模型
     *
     * @return
     */
    @PostMapping("/add")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> model(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
                                      @Validated(value = AddAction.class) @RequestBody ModelVO modelVO) {
        boolean flag = Validator.isUrl(modelVO.getBaseUrl());
        if (!flag) {
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID);
        }
        modelService.addModel(workspaceId, modelVO);
        return ResponseData.success();
    }

    /**
     * 删除模型
     *
     * @param id 模型id
     * @return
     */
    @DeleteMapping("/{id}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData model(@PathVariable("id") String id) {
        List<Dataset> datasets = datasetService.searchDatasetsByLlmModelId(id);
        if (!datasets.isEmpty()) {
            return ResponseData.error(datasets);
        }

        modelService.removeModel(id);
        return ResponseData.success();
    }

    /**
     * 修改模型
     *
     * @return
     */
    @PutMapping("/update")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> updateModel(@Validated(value = UpdateAction.class) @RequestBody ModelVO vo) {
        boolean flag = Validator.isUrl(vo.getBaseUrl());
        if (!flag) {
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID);
        }
        modelService.updateModel(vo);
        return ResponseData.success();
    }

    /**
     * 模型列表
     *
     * @param pageSize
     * @param pageNo
     * @param type
     * @param autoAgent 是否支持auto agent
     * @return
     */
    @GetMapping("/list")
    public ResponseData<PageModel<ModelDTO>> model(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
                                                   @RequestParam(value = "type", required = false) String type,
                                                   @RequestParam(value = "autoAgent", required = false) Boolean autoAgent,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                   @RequestParam(value = "pageNo", defaultValue = "0") Integer pageNo) {
        PageModel<ModelDTO> res = modelService.modelList(workspaceId, type, autoAgent, pageSize, pageNo);
        return ResponseData.success(res);
    }

    /**
     * 模型厂商列表
     *
     * @return
     */
    @GetMapping("/providers")
    public ResponseData<Map<String, String>> providers() {
        return ResponseData.success(AiProvider.providers());
    }
}
