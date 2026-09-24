package com.vita.app.user.mapper;

import com.vita.app.user.entity.AppUser;
import com.vita.mybatis.mapper.BaseMapperX;
import com.vita.mybatis.wrapper.LambdaQueryWrapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.app.user.mapper
 * @Author: znk
 * @CreateTime: 2026-08-26
 * @Description: App用户Mapper
 * @Version: 1.0
 */
@Mapper
public interface AppUserMapper extends BaseMapperX<AppUser> {

    /**
     * 按全局唯一用户名查询用户。
     *
     * @param userName 规范化后的用户名
     * @return 用户实体，不存在时返回空
     */
    default AppUser selectByUserName(String userName) {
        return selectOne(new LambdaQueryWrapperX<AppUser>()
                .eq(AppUser::getUserName, userName));
    }
}
