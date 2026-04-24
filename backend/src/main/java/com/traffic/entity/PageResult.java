package com.traffic.entity;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;  // 当前页数据
    private long total;       // 总记录数
    private int page;         // 当前页码
    private int pageSize;     // 每页大小
    private long pages;       // 总页数

    public PageResult(List<T> records, long total, int page, int pageSize) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.pages = (total + pageSize - 1) / pageSize;
    }

}
