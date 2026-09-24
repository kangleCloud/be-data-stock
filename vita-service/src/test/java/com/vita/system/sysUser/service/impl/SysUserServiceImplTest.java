package com.vita.system.sysUser.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.vita.core.exception.ServiceException;
import com.vita.crypto.Sm4Utils;
import com.vita.system.sysUser.dto.SysUserDeletedDto;
import com.vita.system.sysUser.dto.SysUserSearchDto;
import com.vita.system.sysUser.dto.SysUserUpdateDto;
import com.vita.system.sysUser.entity.SysUser;
import com.vita.system.sysUser.mapper.SysUserMapper;
import com.vita.system.sysUser.vo.SysUserDetailVo;
import com.vita.system.sysUser.vo.SysUserListVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户管理服务契约回归测试。
 *
 * @author znk
 */
@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    private SysUserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SysUserServiceImpl();
        ReflectionTestUtils.setField(service, "sysUserMapper", sysUserMapper);
    }

    @Test
    void detailShouldExposeIdWithoutPasswordProperty() {
        SysUser user = user(7L);
        user.setPassword("encrypted");
        when(sysUserMapper.selectById(7L)).thenReturn(user);

        SysUserDetailVo detail = service.get(7L);

        assertThat(detail.getId()).isEqualTo(7L);
        assertThat(SysUserListVo.class.getDeclaredFields())
                .extracting("name")
                .doesNotContain("password");
    }

    @Test
    void blankPasswordShouldKeepExistingPassword() {
        when(sysUserMapper.selectById(7L)).thenReturn(user(7L));
        SysUserUpdateDto updateDto = updateDto(7L);
        updateDto.setPassword(" ");

        service.update(updateDto);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(captor.capture());
        assertThat(captor.getValue().getPassword()).isNull();
        assertThat(captor.getValue().getPwdUpdateDate()).isNull();
        assertThat(captor.getValue().getIsSystem()).isNull();
        assertThat(captor.getValue().getIsSuperAdmin()).isNull();
    }

    @Test
    void newPasswordShouldBeEncryptedAndUpdatePasswordDate() {
        when(sysUserMapper.selectById(7L)).thenReturn(user(7L));
        SysUserUpdateDto updateDto = updateDto(7L);
        updateDto.setPassword("new-password-123");

        service.update(updateDto);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(captor.capture());
        assertThat(captor.getValue().getPassword())
                .isEqualTo(Sm4Utils.encryptToBase64("new-password-123"));
        assertThat(captor.getValue().getPwdUpdateDate()).isNotNull();
    }

    @Test
    void userNameShouldParticipateInListQuery() {
        when(sysUserMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        SysUserSearchDto searchDto = new SysUserSearchDto();
        searchDto.setUserName("operator");

        service.list(searchDto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<SysUser>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(sysUserMapper).selectList(captor.capture());
        assertThat(captor.getValue().getExpression().getNormal()).isNotEmpty();
    }

    @Test
    void protectedUserShouldNotBeDeleted() {
        SysUser systemUser = user(7L);
        systemUser.setIsSystem((byte) 1);
        when(sysUserMapper.selectById(7L)).thenReturn(systemUser);

        SysUserDeletedDto deletedDto = new SysUserDeletedDto();
        deletedDto.setId(7L);

        assertThatThrownBy(() -> service.delete(deletedDto))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("不允许删除");
        verify(sysUserMapper, never()).deleteById(7L);
    }

    @Test
    void deleteShouldRejectMissingRequestId() {
        assertThatThrownBy(() -> service.delete(new SysUserDeletedDto()))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> service.delete(null))
                .isInstanceOf(ServiceException.class);
        verifyNoInteractions(sysUserMapper);
    }

    private SysUser user(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUserName("operator");
        user.setStatus((byte) 1);
        user.setIsSystem((byte) 0);
        user.setIsSuperAdmin((byte) 0);
        return user;
    }

    private SysUserUpdateDto updateDto(Long id) {
        SysUserUpdateDto dto = new SysUserUpdateDto();
        dto.setId(id);
        dto.setUserName("operator");
        dto.setStatus((byte) 1);
        return dto;
    }
}
