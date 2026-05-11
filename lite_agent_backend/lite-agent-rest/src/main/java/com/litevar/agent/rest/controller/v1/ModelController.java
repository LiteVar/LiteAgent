package com.litevar.agent.rest.controller.v1;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.litevar.agent.auth.annotation.SystemRole;
import com.litevar.agent.auth.annotation.WorkspaceRole;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.dto.ModelDTO;
import com.litevar.agent.base.entity.Dataset;
import com.litevar.agent.base.entity.LlmModel;
import com.litevar.agent.base.enums.AiProvider;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ServiceExceptionEnum;
import com.litevar.agent.base.enums.SystemRoleEnum;
import com.litevar.agent.base.exception.ServiceException;
import com.litevar.agent.base.response.PageModel;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.valid.AddAction;
import com.litevar.agent.base.valid.UpdateAction;
import com.litevar.agent.base.vo.ModelVO;
import com.litevar.agent.core.module.llm.ModelService;
import com.litevar.agent.rest.service.DatasetService;
import com.litevar.agent.rest.util.FileDownloadUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        LlmModel model = modelService.addModel(workspaceId, modelVO);
        return ResponseData.success(model.getId());
    }

    /**
     * 新建模型(系统管理员)
     *
     * @param modelVO
     * @return
     */
    @PostMapping("/addForSystem")
    @SystemRole(value = {SystemRoleEnum.ROLE_SYSTEM_ADMIN})
    public ResponseData<String> model(@Validated(value = AddAction.class) @RequestBody ModelVO modelVO) {
        boolean flag = Validator.isUrl(modelVO.getBaseUrl());
        if (!flag) {
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID);
        }
        LlmModel model = modelService.addModel("0", modelVO);
        return ResponseData.success(model.getId());
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
     * 删除模型(系统管理员)
     *
     * @param id 模型id
     * @return
     */
    @DeleteMapping("/deleteForSystem/{id}")
    @SystemRole(value = {SystemRoleEnum.ROLE_SYSTEM_ADMIN})
    public ResponseData deleteForSystem(@PathVariable("id") String id) {
        List<Dataset> datasets = datasetService.searchDatasetsByLlmModelId(id);
        if (!datasets.isEmpty()) {
            return ResponseData.error(datasets);
        }
        LlmModel model = modelService.findById(id);
        if (ObjectUtil.notEqual(model.getStatus(), 0)) {
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
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
        boolean flag = !Validator.isUrl(vo.getBaseUrl()) || StrUtil.equals(vo.getApiKey(), "{{<APIKEY>}}");
        if (flag) {
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID);
        }
        modelService.updateModel(vo);
        return ResponseData.success();
    }

    /**
     * 修改模型(系统管理员)
     *
     * @return
     */
    @PutMapping("/updateForSystem")
    @SystemRole(value = {SystemRoleEnum.ROLE_SYSTEM_ADMIN})
    public ResponseData<String> updateModelForSystem(@Validated(value = UpdateAction.class) @RequestBody ModelVO vo) {
        boolean flag = !Validator.isUrl(vo.getBaseUrl()) || StrUtil.equals(vo.getApiKey(), "{{<APIKEY>}}");
        if (flag) {
            throw new ServiceException(ServiceExceptionEnum.ARGUMENT_NOT_VALID);
        }
        LlmModel model = modelService.findById(vo.getId());
        if (ObjectUtil.equal(model.getStatus(), 1)) {
            //已启用不能删除
            throw new ServiceException(ServiceExceptionEnum.OPERATE_FAILURE);
        }
        modelService.updateModel(vo);
        return ResponseData.success();
    }

    /**
     * 模型列表
     *
     * @param pageSize
     * @param pageNo
     * @param type      模型类型
     * @param query     查询条件(支持查模型名字,别名)
     * @param status    模型状态(1-启用, 2-禁用)
     * @param autoAgent 是否支持auto agent
     * @param tab       0-全部,1-系统,2-我的
     * @return
     */
    @GetMapping("/list")
    public ResponseData<PageModel<ModelDTO>> model(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
                                                   @RequestParam(value = "type", required = false) String type,
                                                   @RequestParam(value = "query", required = false) String query,
                                                   @RequestParam(value = "tab", required = false, defaultValue = "0") Integer tab,
                                                   @RequestParam(value = "status", required = false) Integer status,
                                                   @RequestParam(value = "autoAgent", required = false) Boolean autoAgent,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                   @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo) {
        //非系统用户只能看到已启用和停用的模型数据
        List<Integer> statusList;
        if (ObjectUtil.isNotEmpty(status)) {
            statusList = List.of(status);
        } else {
            statusList = List.of(1, 2);
        }
        PageModel<ModelDTO> res = modelService.modelList(workspaceId, type, autoAgent, pageSize, pageNo, query, tab, statusList);
        return ResponseData.success(res);
    }

    /**
     * 模型列表(系统管理员)
     *
     * @param query    查询条件(支持查模型名字,别名)
     * @param pageSize
     * @param pageNo
     * @return
     */
    @GetMapping("/listForSystem")
    @SystemRole(value = {SystemRoleEnum.ROLE_SYSTEM_ADMIN})
    public ResponseData<PageModel<ModelDTO>> model(@RequestParam(value = "query", required = false) String query,
                                                   @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                   @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo) {
        PageModel<ModelDTO> res = modelService.modelList("0", null, null, pageSize, pageNo, query, 1, null);
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

    /**
     * 导出模型配置
     *
     * @param id        模型ID
     * @param plainText 是否明文,默认为false
     */
    @GetMapping("/export/{id}")
    public void exportModel(HttpServletResponse response,
                            @PathVariable("id") String id,
                            @RequestParam(value = "plainText", defaultValue = "false") boolean plainText) {
        LlmModel model = modelService.findById(id);
        byte[] bytes = modelService.exportModel(model, plainText);
        FileDownloadUtil.download(response, model.getName() + ".json", bytes);
    }

    /**
     * 导入模型配置
     *
     * @param workspaceId 工作空间ID
     * @param files       模型配置文件列表
     * @return 导入结果
     */
    @PostMapping("/import")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<Map<String, String>> importModels(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
                                                          @RequestParam("files") MultipartFile[] files) {
        Map<String, String> mapping = modelService.importModels(workspaceId, files);
        return ResponseData.success(mapping);
    }

    /**
     * 导入模型配置(系统管理员)
     *
     * @param files 模型配置文件列表
     * @return 导入结果
     */
    @PostMapping("/importForSystem")
    @SystemRole(value = {SystemRoleEnum.ROLE_SYSTEM_ADMIN})
    public ResponseData<Map<String, String>> importModels(@RequestParam("files") MultipartFile[] files) {
        Map<String, String> mapping = modelService.importModels("0", files);
        return ResponseData.success(mapping);
    }

    /**
     * 切换模型状态(启用/禁用)
     *
     * @param id 模型ID
     * @return
     */
    @PostMapping("/{id}/status/toggle")
    @SystemRole(value = {SystemRoleEnum.ROLE_SYSTEM_ADMIN})
    public ResponseData<String> toggleStatus(
            @PathVariable("id") String id,
            @RequestParam("status") Integer status
    ) {
        modelService.toggleStatus(id, status);
        return ResponseData.success();
    }
}
