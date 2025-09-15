package com.litevar.wechat.adapter.core.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.litevar.wechat.adapter.common.core.web.R;
import com.litevar.wechat.adapter.core.dto.AgentWxRefDTO;
import com.litevar.wechat.adapter.core.service.IAgentWxService;
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
@RequestMapping("/api/agentWxRef")
@Tag(name = "agent与服务号绑定管理", description = "agent与服务号绑定管理接口")
public class AgentWxMangerController {
    
    private final IAgentWxService agentWxService;
    


    /**
     * 创建Agent与服务号绑定
     */
    @PostMapping("/agentRef")
    @Operation(summary = "创建Agent与服务号绑定", description = "创建一个新的Agent与服务号的绑定")
    @SaCheckLogin
    public R<?> createAgentWxRef(@Validated @RequestBody AgentWxRefDTO agentWxRefDTO) {
        agentWxService.createAgentWxRef(agentWxRefDTO);
        return R.ok("绑定成功");
    }


    /**
     * 更新Agent与服务号绑定
     */
    @PutMapping("/agentRef")
    @Operation(summary = "更新Agent与服务号绑定", description = "更新一个Agent与服务号的绑定")
    @SaCheckLogin
    public R<?> updateAgentWxRef(@Validated @RequestBody AgentWxRefDTO agentWxRefDTO) {
        agentWxService.updateAgentWxRef(agentWxRefDTO);
        return R.ok("更新成功");
    }


    /**
     * 删除Agent与服务号绑定
     */
    @DeleteMapping("/agentRef")
    @Operation(summary = "删除Agent与服务号绑定", description = "删除一个Agent与服务号的绑定")
    @SaCheckLogin
    public R<?> deleteAgentWxRef(@NotBlank(message = "Id不能为空")
                                      @RequestParam(value = "Id") String Id) {
        agentWxService.deleteAgentWxRef(Id);
        return R.ok("删除成功");
    }


    /**
     * 获取Agent与服务号绑定列表
     */
    @GetMapping("/agentRef")
    @Operation(summary = "获取Agent与服务号绑定列表")
    @SaCheckLogin
    @Parameter(name = "pageSize", description = "每页数量，最大100", example = "10")
    @Parameter(name = "search", description = "查找内容")
    @Parameter(name = "pageNum", description = "当前页", example = "1")
    public R<PageResult<AgentWxRefDTO>> listStarredAgents(
            @NotNull(message = "当前页不能为空") @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @NotNull(message = "每页数量不能为空") @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "search") String search) {
        PageResult<AgentWxRefDTO> page = agentWxService.getAgentWxRefList(pageNum, pageSize,search);
        return R.ok(page);
    }


}