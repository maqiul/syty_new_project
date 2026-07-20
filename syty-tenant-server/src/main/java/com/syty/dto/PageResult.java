package com.syty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页响应结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 当前页码 */
    private long current;

    /** 每页条数 */
    private long size;

    /** 总记录数 */
    private long total;

    /** 数据列表 */
    private List<T> records;

    /** 总页数 */
    public long getPages() {
        return size > 0 ? (total + size - 1) / size : 0;
    }

    public static <T> PageResult<T> build(long current, long size, long total, long pages, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setCurrent(current);
        result.setSize(size);
        result.setTotal(total);
        result.setRecords(records);
        return result;
    }
}
