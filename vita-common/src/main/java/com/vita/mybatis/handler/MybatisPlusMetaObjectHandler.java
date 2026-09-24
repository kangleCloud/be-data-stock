package com.vita.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.vita.auth.context.LoginUserInfoModelContext;
import com.vita.auth.model.LoginUserInfoModel;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.mybatis.handler
 * @Author: znk
 * @CreateTime: 2025-08-07  10:43:23
 * @Description: MybatisPlus自动填充处理器
 * @Version: 1.0
 */
@Component
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    // 是否启用登录用户相关字段自动填充
    @Value("${mybatis-plus.login:false}" )
    private Boolean login;

    /**
     * 插入数据时自动填充字段
     *
     * @param metaObject 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // 填充创建时间、更新时间和删除标记
        this.strictInsertFill(metaObject, "createTime" , LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime" , LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "isDeleted" , Boolean.class, false);

        // 如果启用登录用户相关字段，填充创建/更新人信息
        LoginUserInfoModel model = LoginUserInfoModelContext.getLoginUserInfo();
        if (model != null) {
            this.strictInsertFill(metaObject, "createById" , Long.class, model.getId());
            this.strictInsertFill(metaObject, "createBy" , String.class, model.getUsername());
            this.strictInsertFill(metaObject, "updateById" , Long.class, model.getId());
            this.strictInsertFill(metaObject, "updateBy" , String.class, model.getUsername());
        }

    }

    /**
     * 更新数据时自动填充字段
     *
     * @param metaObject 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 填充更新时间
        this.strictUpdateFill(metaObject, "updateTime" , LocalDateTime.class, LocalDateTime.now());
        // 如果启用登录用户相关字段，填充更新人信息
        LoginUserInfoModel model = LoginUserInfoModelContext.getLoginUserInfo();
        if (model != null) {
            this.strictUpdateFill(metaObject, "updateById" , Long.class, model.getId());
            this.strictUpdateFill(metaObject, "updateBy" , String.class, model.getUsername());
        }
    }
}
