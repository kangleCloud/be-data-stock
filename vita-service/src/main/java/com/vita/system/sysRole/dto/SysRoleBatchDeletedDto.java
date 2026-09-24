package com.vita.system.sysRole.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色批量删除 DTO。
 *
 * @author znk
 */
@Data
public class SysRoleBatchDeletedDto {

    /**
     * 角色 ID 列表。
     */
    @NotEmpty(message = "角色ID列表不能为空")
    private List<@NotNull(message = "角色ID不能为空") Long> ids;
}
