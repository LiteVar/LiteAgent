package com.litevar.dingtalk.adapter.core.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取部门用户基础信息DTO
 * @author Teoan
 * @since 2025/8/13 16:31
 */
@Data
@Schema(description = "获取部门用户基础信息DTO")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GetDepartmentUserListDTO {

    @Schema(description = "分页查询的游标，最开始传0，后续传返回参数中的next_cursor值")
    @NotNull(message = "游标不能为空")
    private Long cursor;


    @Schema(description = "部门ID，如果是根部门，该参数传1")
    @NotNull(message = "部门ID")
    private Long deptId;


    @Schema(description = "机器人robotCode")
    @NotNull(message = "robotCode")
    private String robotCode;


    @Schema(description = "部门成员的排序规则",example ="entry_asc,entry_desc,modify_asc,modify_desc,custom" )
    private String orderField;


    @Schema(description = "分页长度，最大值100。")
    @NotNull(message = "分页长度")
    private Long size;


}
