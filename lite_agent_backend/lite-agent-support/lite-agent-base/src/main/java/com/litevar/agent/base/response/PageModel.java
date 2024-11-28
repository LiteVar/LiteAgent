package com.litevar.agent.base.response;

import lombok.Data;

import java.util.List;

/**
 * 分页响应给前端
 *
 * @author uncle
 * @since 2024/8/7 14:43
 */
@Data
public class PageModel<T> {
    /**
     * 当前第几页
     */
    private Integer pageNo;

    /**
     * 一页多少条记录
     */
    private Integer pageSize;

    /**
     * 全部数据量
     */
    private Long total;

    /**
     * 数据
     */
    private List<T> list;

    public PageModel(Integer pageNo, Integer pageSize, Long total, List<T> list) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
        this.list = list;
    }
}
