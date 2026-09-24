package com.vita.file.config;

import cn.hutool.core.text.CharSequenceUtil;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.file.property.FileProperty;
import com.vita.file.property.MinioStorageProperty;
import com.vita.file.storage.VitaMinioClient;
import com.vita.file.support.FileUrlSupport;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * MinIO 客户端工厂，负责解析最终配置并按需重建客户端。
 */
@Slf4j
public class MinioClientFactory {

    private final FileProperty fileProperty;
    private final FileUrlSupport fileUrlSupport;

    private volatile MinioClientContext clientContext;

    public MinioClientFactory(FileProperty fileProperty, FileUrlSupport fileUrlSupport) {
        this.fileProperty = fileProperty;
        this.fileUrlSupport = fileUrlSupport;
    }

    /**
     * 获取当前生效的 MinIO 配置。
     */
    public ResolvedMinioConfig resolveAndValidateConfig() {
        ResolvedMinioConfig config = resolveConfig();
        validateConfig(config);
        return config;
    }

    /**
     * 获取当前客户端上下文；配置变化时自动重建。
     */
    public MinioClientContext getClientContext() {
        ResolvedMinioConfig config = resolveAndValidateConfig();
        MinioClientContext current = clientContext;
        if (current != null && current.getConfig().equals(config)) {
            return current;
        }

        synchronized (this) {
            current = clientContext;
            if (current != null && current.getConfig().equals(config)) {
                return current;
            }
            MinioClientContext rebuilt = buildContext(config);
            clientContext = rebuilt;
            return rebuilt;
        }
    }

    /**
     * 确保 bucket 已准备就绪。
     */
    public MinioClientContext ensureBucketReady(MinioClientContext context) {
        if (context.isBucketReady()) {
            return context;
        }
        synchronized (context) {
            if (context.isBucketReady()) {
                return context;
            }
            ensureBucket(context);
            context.setBucketReady(true);
            return context;
        }
    }

    private MinioClientContext buildContext(ResolvedMinioConfig config) {
        MinioClient uploadClient = buildClient(config.getEndpoint(), config);
        String signEndpoint = CharSequenceUtil.isNotBlank(config.getPrivateEndpoint())
                ? config.getPrivateEndpoint() : config.getPublicEndpoint();
        MinioClient signClient = buildClient(fileUrlSupport.extractOrigin(signEndpoint), config);

        MinioClientContext context = new MinioClientContext(config,
                new VitaMinioClient(uploadClient), new VitaMinioClient(signClient));
        log.info("MinIO client initialized. endpoint={}, bucket={}, region={}, publicEndpoint={}, privateEndpoint={}, accessKey={}",
                config.getEndpoint(), config.getBucket(), config.getRegion(),
                config.getPublicEndpoint(), config.getPrivateEndpoint(), maskAccessKey(config.getAccessKey()));
        return context;
    }

    private MinioClient buildClient(String endpoint, ResolvedMinioConfig config) {
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(config.getAccessKey(), config.getSecretKey());
        if (CharSequenceUtil.isNotBlank(config.getRegion())) {
            builder.region(config.getRegion());
        }
        return builder.build();
    }

    private void ensureBucket(MinioClientContext context) {
        ResolvedMinioConfig config = context.getConfig();
        try {
            boolean exists = context.getUploadClient().bucketExists(BucketExistsArgs.builder()
                    .bucket(config.getBucket())
                    .build());
            if (exists) {
                return;
            }
            if (!config.isAutoCreateBucket()) {
                throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                        "MinIO bucket 不存在: " + config.getBucket());
            }
            MakeBucketArgs.Builder builder = MakeBucketArgs.builder().bucket(config.getBucket());
            if (CharSequenceUtil.isNotBlank(config.getRegion())) {
                builder.region(config.getRegion());
            }
            context.getUploadClient().makeBucket(builder.build());
            log.info("MinIO bucket auto created. bucket={}", config.getBucket());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(),
                    "MinIO bucket 初始化失败: " + config.getBucket());
        }
    }

    private ResolvedMinioConfig resolveConfig() {
        MinioStorageProperty minio = fileProperty.getMinio();
        boolean secure = Boolean.TRUE.equals(minio.getSecure());
        String endpoint = normalizeServiceEndpoint(trimToNull(minio.getEndpoint()), secure);
        String accessKey = trimToNull(minio.getAccessKey());
        String secretKey = trimToNull(minio.getSecretKey());
        String bucket = trimToNull(minio.getBucket());
        String region = trimToNull(minio.getRegion());
        boolean autoCreateBucket = Boolean.TRUE.equals(minio.getAutoCreateBucket());
        int presignedExpireSeconds = minio.getPresignedExpireSeconds() != null
                ? minio.getPresignedExpireSeconds() : fileProperty.getSignExpireSeconds();
        boolean pathStyleAccess = Boolean.TRUE.equals(minio.getPathStyleAccess());

        String publicEndpoint = fileUrlSupport.normalizeBaseUrl(firstNonBlank(
                trimToNull(minio.getPublicEndpoint()),
                trimToNull(minio.getPublicDomain())
        ));
        String privateEndpoint = fileUrlSupport.normalizeBaseUrl(firstNonBlank(
                trimToNull(minio.getPrivateEndpoint()),
                trimToNull(minio.getPrivateDomain()),
                publicEndpoint
        ));

        return ResolvedMinioConfig.builder()
                .endpoint(endpoint)
                .accessKey(accessKey)
                .secretKey(secretKey)
                .bucket(bucket)
                .region(region)
                .secure(secure)
                .autoCreateBucket(autoCreateBucket)
                .presignedExpireSeconds(presignedExpireSeconds)
                .pathStyleAccess(pathStyleAccess)
                .publicEndpoint(publicEndpoint)
                .privateEndpoint(privateEndpoint)
                .build();
    }

    private void validateConfig(ResolvedMinioConfig config) {
        requireNotBlank(config.getEndpoint(), "vita.file.minio.endpoint 未配置");
        requireNotBlank(config.getAccessKey(), "vita.file.minio.access-key 未配置");
        requireNotBlank(config.getSecretKey(), "vita.file.minio.secret-key 未配置");
        requireNotBlank(config.getBucket(), "vita.file.minio.bucket 未配置");
        requireNotBlank(config.getPublicEndpoint(), "vita.file.minio.public-endpoint 未配置");
        if (config.getPresignedExpireSeconds() <= 0) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "vita.file.minio.presigned-expire-seconds 必须大于 0");
        }
    }

    private void requireNotBlank(String value, String message) {
        if (CharSequenceUtil.isBlank(value)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), message);
        }
    }

    private String trimToNull(String value) {
        return CharSequenceUtil.isBlank(value) ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String normalizeServiceEndpoint(String endpoint, boolean secure) {
        if (CharSequenceUtil.isBlank(endpoint)) {
            return endpoint;
        }
        String normalized = endpoint.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = (secure ? "https://" : "http://") + normalized;
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String maskAccessKey(String accessKey) {
        if (CharSequenceUtil.isBlank(accessKey)) {
            return "";
        }
        if (accessKey.length() <= 6) {
            return accessKey.charAt(0) + "***";
        }
        return accessKey.substring(0, 3) + "***" + accessKey.substring(accessKey.length() - 3);
    }

    @Getter
    public static class MinioClientContext {
        private final ResolvedMinioConfig config;
        private final VitaMinioClient uploadClient;
        private final VitaMinioClient signClient;
        private volatile boolean bucketReady;

        public MinioClientContext(ResolvedMinioConfig config,
                                  VitaMinioClient uploadClient,
                                  VitaMinioClient signClient) {
            this.config = config;
            this.uploadClient = uploadClient;
            this.signClient = signClient;
        }

        public boolean isBucketReady() {
            return bucketReady;
        }

        public void setBucketReady(boolean bucketReady) {
            this.bucketReady = bucketReady;
        }
    }
}
