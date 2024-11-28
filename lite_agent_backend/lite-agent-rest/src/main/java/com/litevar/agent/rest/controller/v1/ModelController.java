package com.litevar.agent.rest.controller.v1;

import cn.hutool.core.lang.Validator;
import com.litevar.agent.auth.annotation.WorkspaceRole;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.dto.ModelDTO;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.valid.AddAction;
import com.litevar.agent.base.valid.UpdateAction;
import com.litevar.agent.base.vo.ModelVO;
import com.litevar.agent.core.module.llm.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    public ResponseData<String> model(@PathVariable("id") String id) {
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
     * @return
     */
    @GetMapping("/list")
    public ResponseData<PageModel<ModelDTO>> model(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                   @RequestParam(value = "pageNo", defaultValue = "0") Integer pageNo) {

        PageRequest page = PageRequest.of(pageNo, pageSize, Sort.by("createTime").descending());
        PageModel<ModelDTO> res = modelService.modelList(workspaceId, page);
        return ResponseData.success(res);
    }
}
