package com.vita.system.sysDept.dto;

import com.vita.core.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: vita
 * @BelongsPackage: com.vita.system.sysDept.dto
 * @Author znk
 * @CreateTime 2026-03-31 16:29:38
 * @Description: 部门表 搜索 DTO
 * @version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDeptSearchDto extends PageRequest {

    /**
     * 关键字。
     */
    private String keyword;
}
