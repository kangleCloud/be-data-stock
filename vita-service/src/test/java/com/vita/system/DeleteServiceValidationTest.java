package com.vita.system;

import com.vita.core.exception.ServiceException;
import com.vita.system.sysPermission.dto.SysPermissionBatchDeletedDto;
import com.vita.system.sysPermission.service.impl.SysPermissionServiceImpl;
import com.vita.system.sysRole.dto.SysRoleBatchDeletedDto;
import com.vita.system.sysRole.service.impl.SysRoleServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 删除 Service 请求对象校验测试。
 *
 * @author znk
 */
class DeleteServiceValidationTest {

    @Test
    void batchDeleteShouldRejectMissingRequest() {
        assertThatThrownBy(() -> new SysRoleServiceImpl().deleteBatch(null))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> new SysPermissionServiceImpl().deleteBatch(null))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void batchDeleteShouldRejectEmptyIds() {
        SysRoleBatchDeletedDto roleRequest = new SysRoleBatchDeletedDto();
        roleRequest.setIds(List.of());
        SysPermissionBatchDeletedDto permissionRequest = new SysPermissionBatchDeletedDto();
        permissionRequest.setIds(List.of());

        assertThatThrownBy(() -> new SysRoleServiceImpl().deleteBatch(roleRequest))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> new SysPermissionServiceImpl().deleteBatch(permissionRequest))
                .isInstanceOf(ServiceException.class);
    }
}
