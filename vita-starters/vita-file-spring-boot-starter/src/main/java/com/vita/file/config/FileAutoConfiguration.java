package com.vita.file.config;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.file.property.FileProperty;
import com.vita.file.support.FileUrlSupport;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * 文件上传自动配置。
 */
@Configuration
@ConditionalOnProperty(name = "vita.file.enabled", havingValue = "true", matchIfMissing = true)
public class FileAutoConfiguration {

    @Bean
    public MinioClientFactory minioClientFactory(FileProperty fileProperty,
                                                 FileUrlSupport fileUrlSupport) {
        return new MinioClientFactory(fileProperty, fileUrlSupport);
    }

    /**
     * 启动校验。
     */
    @Bean
    public InitializingBean fileStartupValidator(FileProperty fileProperty,
                                                 MinioClientFactory minioClientFactory) {
        return () -> {
            String active = fileProperty.getActive();
            if (!"local".equals(active) && !"minio".equals(active)) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                        "vita.file.active 仅支持 local 或 minio");
            }

            // local 模式校验
            if ("local".equals(active)) {
                String basePath = fileProperty.getLocal().getBasePath();
                if (CharSequenceUtil.isBlank(basePath)) {
                    throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                            "vita.file.local.base-path 未配置");
                }
                File dir = new File(basePath);
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(),
                            "本地存储目录无法创建: " + basePath);
                }
                if (CharSequenceUtil.isBlank(fileProperty.getLocal().getPublicDomain())) {
                    throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                            "vita.file.local.public-domain 未配置");
                }
                String secret = fileProperty.getSignature().getSecret();
                if (CharSequenceUtil.isBlank(secret) || "change-me".equals(secret)) {
                    throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                            "vita.file.signature.secret 未配置");
                }
            }

            // minio 模式校验
            if ("minio".equals(active)) {
                minioClientFactory.resolveAndValidateConfig();
            }

            Long maxFileSize = fileProperty.getMaxFileSize();
            if (maxFileSize == null || maxFileSize <= 0) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                        "vita.file.max-file-size 必须大于 0");
            }

            Integer signExpire = fileProperty.getSignExpireSeconds();
            if (signExpire == null || signExpire <= 0) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                        "vita.file.sign-expire-seconds 必须大于 0");
            }
        };
    }
}
