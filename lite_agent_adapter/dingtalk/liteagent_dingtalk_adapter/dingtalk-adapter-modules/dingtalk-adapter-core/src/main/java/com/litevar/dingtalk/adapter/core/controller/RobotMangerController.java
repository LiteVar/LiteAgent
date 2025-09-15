package com.litevar.dingtalk.adapter.core.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.litevar.dingtalk.adapter.common.core.web.R;
import com.litevar.dingtalk.adapter.core.dto.AgentRobotRefDTO;
import com.litevar.dingtalk.adapter.core.dto.RobotPermissionsDTO;
import com.litevar.dingtalk.adapter.core.service.IAgentRobotService;
import com.litevar.dingtalk.adapter.core.service.IRobotPermissionsService;
import com.mongoplus.model.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * agent 信息
 *
 * @author Teoan
 * @since 2025/8/13 17:27
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/robot")
@Tag(name = "Robot管理", description = "Robot管理接口")
public class RobotMangerController {

    private final IRobotPermissionsService robotPermissionsService;
    private final IAgentRobotService agentRobotService;


    /**
     * 创建Robot权限
     */
    @PostMapping("/permissions")
    @Operation(summary = "创建Robot权限", description = "创建一个新的Robot权限配置")
    @SaCheckLogin
    public R<?> createRobotPermission(@Validated @RequestBody RobotPermissionsDTO agentPermissionsDTO) {
        robotPermissionsService.createRobotPermission(agentPermissionsDTO);
        return R.ok("创建成功");
    }


    /**
     * 更新Robot权限
     */
    @PutMapping("/permissions")
    @Operation(summary = "更新Robot权限", description = "更新Robot权限配置")
    @SaCheckLogin
    public R<?> updateAgentPermission(@Validated @RequestBody RobotPermissionsDTO robotPermissionsDTO) {
        robotPermissionsService.updateRobotPermission(robotPermissionsDTO);
        return R.ok("更新成功");
    }

    /**
     * 删除Robot权限
     */
    @DeleteMapping("/permissions/{id}")
    @Operation(summary = "删除Robot权限", description = "根据ID删除指定的Robot权限配置")
    @SaCheckLogin
    public R<?> deleteAgentPermission(@NotBlank(message = "权限ID不能为空") @PathVariable("id") String id) {
        robotPermissionsService.deleteRobotPermission(id);
        return R.ok("删除成功");
    }

    /**
     * 根据Robot code获取权限信息
     */
    @GetMapping("/permissions/{robotCode}")
    @Operation(summary = "根据Robot code获取权限信息", description = "根据Robot code获取权限信息")
    @Parameter(name = "robotCode", description = "robotCode", required = true)
    @SaCheckLogin
    public R<RobotPermissionsDTO> getAgentPermissions(@NotBlank(message = "robotCode不能为空") @PathVariable("robotCode") String robotCode) {
        return R.ok(robotPermissionsService.getRobotPermissions(robotCode));
    }


    /**
     * 创建Agent与机器人绑定
     */
    @PostMapping("/agentRef")
    @Operation(summary = "创建Agent与机器人绑定", description = "创建一个新的Agent与机器人的绑定")
    @SaCheckLogin
    public R<?> createAgentRobotRef(@Validated @RequestBody AgentRobotRefDTO agentRobotRefDTO) {
        agentRobotService.createAgentRobotRef(agentRobotRefDTO);
        return R.ok("绑定成功");
    }


    /**
     * 更新Agent与机器人绑定
     */
    @PutMapping("/agentRef")
    @Operation(summary = "更新Agent与机器人绑定", description = "更新一个Agent与机器人的绑定")
    @SaCheckLogin
    public R<?> updateAgentRobotRef(@Validated @RequestBody AgentRobotRefDTO agentRobotRefDTO) {
        agentRobotService.updateAgentRobotRef(agentRobotRefDTO);
        return R.ok("更新成功");
    }


    /**
     * 删除Agent与机器人绑定
     */
    @DeleteMapping("/agentRef")
    @Operation(summary = "删除Agent与机器人绑定", description = "删除一个Agent与机器人的绑定")
    @SaCheckLogin
    public R<?> deleteAgentRobotRef(@NotBlank(message = "robotCode不能为空")
                                      @RequestParam(value = "robotCode") String robotCode) {
        agentRobotService.deleteAgentRobotRef(robotCode);
        return R.ok("删除成功");
    }


    /**
     * 获取Agent与机器人绑定列表
     */
    @GetMapping("/agentRef")
    @Operation(summary = "获取Agent与机器人绑定列表")
    @SaCheckLogin
    @Parameter(name = "pageSize", description = "每页数量，最大100", example = "10")
    @Parameter(name = "search", description = "查找内容")
    @Parameter(name = "pageNum", description = "当前页", example = "1")
    public R<PageResult<AgentRobotRefDTO>> listStarredAgents(
            @NotNull(message = "当前页不能为空") @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @NotNull(message = "每页数量不能为空") @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "search") String search) {
        PageResult<AgentRobotRefDTO> page = agentRobotService.getAgentRobotRefList(pageNum, pageSize,search);
        page.getContentData().forEach(agentRobotRefDTO -> {
            agentRobotRefDTO.setRobotPermissionsDTO(robotPermissionsService.getRobotPermissions(agentRobotRefDTO.getRobotCode()));
        });
        return R.ok(page);
    }


}