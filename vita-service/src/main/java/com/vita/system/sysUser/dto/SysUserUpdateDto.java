package com.vita.system.sysUser.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户更新请求。密码为空时保留原密码，系统标记与登录审计字段不允许通过该接口修改。
 *
 * @author znk
 */
@Data
public class SysUserUpdateDto {

    /**
     * 用户 ID。
     */
    @NotNull(message = "【用户ID】不能为空")
    private Long id;

    /**
     * 部门 ID。
     */
    private Long deptId;

    /**
     * 昵称。
     */
    private String nickName;

    /**
     * 用户名。
     */
    @NotBlank(message = "【用户名称】不能为空")
    @Size(min = 4, max = 32, message = "【用户名称】长度必须在4到32位之间")
    private String userName;

    /**
     * 可选的新密码。
     */
    @Size(min = 8, max = 64, message = "【密码】长度必须在8到64位之间")
    private String password;

    private String mobile;

    private String email;

    private String avatarUrl;

    private String gender;

    private Byte status;

    private String remark;
}
