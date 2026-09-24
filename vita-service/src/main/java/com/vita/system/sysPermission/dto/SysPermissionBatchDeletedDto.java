package com.vita.system.sysPermission.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 权限批量删除 DTO。
 *
 * @author znk
 */
@Data
public class SysPermissionBatchDeletedDto {

    /**
     * 权限 ID 列表。
     */
    @NotEmpty(message = "权限ID列表不能为空")
    private List<@NotNull(message = "权限ID不能为空") Long> ids;
}
