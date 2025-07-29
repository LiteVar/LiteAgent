package com.litevar.agent.rest.controller.v1;

import com.litevar.agent.auth.annotation.WorkspaceRole;
import com.litevar.agent.base.constant.CommonConstant;
import com.litevar.agent.base.dto.ToolDTO;
import com.litevar.agent.base.entity.ToolProvider;
import com.litevar.agent.base.enums.RoleEnum;
import com.litevar.agent.base.enums.ToolSchemaType;
import com.litevar.agent.base.response.ResponseData;
import com.litevar.agent.base.valid.AddAction;
import com.litevar.agent.base.valid.UpdateAction;
import com.litevar.agent.base.vo.ToolVO;
import com.litevar.agent.core.module.tool.ToolHandleFactory;
import com.litevar.agent.core.module.tool.ToolService;
import com.litevar.agent.core.module.tool.parser.ToolParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 工具管理
 *
 * @author uncle
 * @since 2024/8/12 16:36
 */
@RestController
@RequestMapping("/v1/tool")
public class ToolController {
    @Autowired
    private ToolService toolService;

    /**
     * 工具列表
     *
     * @param name 工具名称
     * @param tab  0-全部,1-系统,2-来自分享,3-我的
     * @return
     */
    @GetMapping("/list")
    public ResponseData<List<ToolDTO>> tool(@RequestParam(value = "name", required = false) String name,
                                            @RequestParam(value = "tab", defaultValue = "0") Integer tab,
                                            @RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId) {
        List<ToolDTO> list = toolService.toolList(workspaceId, name, tab, null);
        return ResponseData.success(list);
    }

    /**
     * 工具列表(带function)
     *
     * @param tab         0-全部,1-系统,2-来自分享,3-我的
     * @param autoAgent   是否支持auto agent
     * @param workspaceId
     * @return
     */
    @GetMapping("/listWithFunction")
    public ResponseData<List<ToolDTO>> tool(@RequestParam(value = "tab", defaultValue = "0") Integer tab,
                                            @RequestParam(value = "autoAgent", required = false) Boolean autoAgent,
                                            @RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId) {
        List<ToolDTO> list = toolService.toolList(workspaceId, tab, autoAgent);
        return ResponseData.success(list);
    }

    /**
     * 工具详细信息
     *
     * @param id 工具id
     * @return
     */
    @GetMapping("/detail/{id}")
    public ResponseData<ToolProvider> detail(@PathVariable("id") String id) {
        ToolProvider toolProvider = Optional.ofNullable(toolService.findById(id)).orElseThrow();
        return ResponseData.success(toolProvider);
    }

    /**
     * 新增工具
     *
     * @param workspaceId
     * @param vo
     * @return
     */
    @PostMapping("/add")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> tool(@RequestHeader(CommonConstant.HEADER_WORKSPACE_ID) String workspaceId,
                                     @RequestBody @Validated(AddAction.class) ToolVO vo) {
        toolService.addTool(vo, workspaceId);
        return ResponseData.success();
    }

    /**
     * 修改工具
     *
     * @param vo
     * @return
     */
    @PutMapping("/update")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> tool(@RequestBody @Validated(UpdateAction.class) ToolVO vo) {
        toolService.updateTool(vo);
        return ResponseData.success();
    }

    /**
     * 删除工具
     *
     * @param id 工具id
     * @return
     */
    @DeleteMapping("/{id}")
    @WorkspaceRole(value = {RoleEnum.ROLE_DEVELOPER, RoleEnum.ROLE_ADMIN})
    public ResponseData<String> tool(@PathVariable("id") String id) {
        toolService.deleteTool(id);
        return ResponseData.success();
    }

    /**
     * 检查工具schema是否有效
     *
     * @param vo
     * @return
     */
    @PostMapping("/checkSchema")
    public ResponseData<String> checkSchema(@RequestBody ToolVO vo) {
        ToolParser parser = ToolHandleFactory.getParseInstance(ToolSchemaType.of(vo.getSchemaType()));
        parser.parse(vo.getSchemaStr());
        return ResponseData.success();
    }
}
