package com.litevar.dingtalk.adapter.core.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.json.JSONObject;
import com.dingtalk.api.response.OapiUserListsimpleResponse;
import com.litevar.dingtalk.adapter.common.core.web.R;
import com.litevar.dingtalk.adapter.core.dto.GetDepartmentUserListDTO;
import com.litevar.dingtalk.adapter.core.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 * @author Teoan
 * @since 2025/8/13 15:49
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/department")
@Tag(name = "部门信息", description = "部门信息")
public class DepartmentController {


    private final DepartmentService departmentService;



    /**
     * 根据robotCode获取部门列表
     */
    @GetMapping("/list")
    @Operation(summary = "根据robotCode获取部门列表", description = "根据robotCode获取部门列表")
    @SaCheckLogin
    public R<List<JSONObject>> getVersionInfo(@RequestParam(value = "deptId",required = false)Long deptId
    , @RequestParam(value = "robotCode")String robotCode) {
        return R.ok(departmentService.getDepartmentList(deptId,robotCode));
    }




    /**
     * 获取部门用户列表
     */
    @PostMapping("/user/list")
    @Operation(summary = "获取部门用户列表", description = "获取部门用户列表")
    @SaCheckLogin
    public R<OapiUserListsimpleResponse.PageResult> getVersionInfo(@RequestBody @Validated GetDepartmentUserListDTO getDepartmentUserListDTO) {
        return R.ok(departmentService.getDepartmentUserList(getDepartmentUserListDTO));
    }














}
