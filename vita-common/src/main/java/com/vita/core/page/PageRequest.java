package com.vita.core.page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.core.page
 * @Author: znk
 * @CreateTime: 2026-03-04  14:53:15
 * @Description: 分页请求参数类，包含分页相关的参数，如页码、页大小等
 * @Version: 1.0
 */
@Data
public class PageRequest implements Serializable {


    /**
     * 序列化版本 UID，确保类的兼容性
     */
    private static final long serialVersionUID = -7947240335968370666L;

    /**
     * 默认页码和页大小，避免分页参数未传递时导致的错误
     */
    private static final Integer PAGE_NUM = 1;
    private static final Integer PAGE_SIZE = 10;

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小值为 1")
    private Integer pageNum = PAGE_NUM;

    @NotNull(message = "每页条数不能为空")
    @Min(value = 1, message = "每页条数最小值为 1")
    @Max(value = 100, message = "每页条数最大值为 100")
    private Integer pageSize = PAGE_SIZE;
}
