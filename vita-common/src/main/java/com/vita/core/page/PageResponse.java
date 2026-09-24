package com.vita.core.page;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.core.page
 * @Author: znk
 * @CreateTime: 2026-03-04  14:53:48
 * @Description: 分页响应类，包含分页结果的相关信息，如总记录数、当前页数据等
 * @Version: 1.0
 */
@Data
public final class PageResponse<T> implements Serializable {

    private static final long serialVersionUID = -3938601703372465470L;

    /**
     * 分页数据列表，包含当前页的数据记录
     */
    private List<T> list;
    /**
     * 总记录数，表示满足查询条件的总数据条数
     */
    private Long total;

    public PageResponse() {
    }

    public PageResponse(List<T> list, Long total) {
        this.list = list;
        this.total = total;
    }

    public PageResponse(Long total) {
        this.list = new ArrayList<>();
        this.total = total;
    }

    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(0L);
    }

    public static <T> PageResponse<T> empty(Long total) {
        return new PageResponse<>(total);
    }
}