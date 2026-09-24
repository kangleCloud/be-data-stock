package com.vita.file.service;

import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.fastjson2.JSONObject;
import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.file.dto.FileUploadRequestDto;
import com.vita.file.enums.FileVisibilityEnum;
import com.vita.file.property.FileProperty;
import com.vita.file.storage.FileStorage;
import com.vita.file.support.*;
import com.vita.file.vo.FileAccessUrlVo;
import com.vita.file.vo.FileUploadVo;
import com.vita.redis.RedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文件应用服务：uploadId 管理、后缀校验、分片编排、URL 组装。
 */
@Slf4j
@Service
public class FileApplicationService {

    private static final String UPLOAD_SESSION_KEY_PREFIX = "vita:file:upload-session:";
    private static final long UPLOAD_EXPIRE_SECONDS = 7 * 86400L;

    private final FileProperty fileProperty;
    private final FileStorage fileStorage;
    private final RedisCache redisCache;
    private final FileObjectKeyGenerator keyGenerator;

    public FileApplicationService(FileProperty fileProperty, FileStorage fileStorage,
                                  RedisCache redisCache, FileObjectKeyGenerator keyGenerator) {
        this.fileProperty = fileProperty;
        this.fileStorage = fileStorage;
        this.redisCache = redisCache;
        this.keyGenerator = keyGenerator;
    }

    /**
     * 获取 uploadId。
     */
    public String getUploadId(FileUploadRequestDto dto) {
        validatePrefixDir(dto.getPrefixDir());
        validateSuffix(dto.getSuffix());
        validateFileSuffix(dto.getSuffix());
        validateFileSize(dto.getFileSize());

        boolean isPublic = FileVisibilityEnum.isPublic(dto.getIsPublic());
        String objectKey = keyGenerator.generate(fileProperty, isPublic, dto.getSuffix(), dto.getPrefixDir());
        String uploadId = UUID.randomUUID().toString().replace("-", "");

        if (isPartUpload(dto.getIsPart())) {
            FileUploadCommand command = FileUploadCommand.builder()
                    .objectKey(objectKey)
                    .isPublic(isPublic)
                    .uploadId(uploadId)
                    .build();
            uploadId = fileStorage.initMultipartUpload(command);
            if (CharSequenceUtil.isBlank(uploadId)) {
                throw new ServiceException(GlobalErrorCode.FILE_UPLOAD_FAILED);
            }
        }

        FileUploadSession session = FileUploadSession.builder()
                .uploadId(uploadId)
                .objectKey(objectKey)
                .isPublic(dto.getIsPublic())
                .isPart(dto.getIsPart())
                .suffix(dto.getSuffix())
                .fileSize(dto.getFileSize())
                .prefixDir(dto.getPrefixDir())
                .originName(dto.getOriginName())
                .build();
        cacheUploadSession(session);
        return uploadId;
    }

    /**
     * 校验 uploadId 有效性。
     */
    public boolean validateUploadId(String uploadId) {
        return CharSequenceUtil.isNotBlank(redisCache.getCacheObject(getUploadSessionKey(uploadId)));
    }

    /**
     * 上传单个文件。
     */
    public FileUploadVo uploadSingleFile(MultipartFile file, String uploadId) {
        FileUploadSession session = getAndValidateUploadSession(uploadId);
        if (isPartUpload(session.getIsPart())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "该 uploadId 为分片上传使用");
        }
        validateFileSuffix(session.getSuffix());
        validateFileSize(file.getSize());

        FileUploadCommand command = FileUploadCommand.builder()
                .objectKey(session.getObjectKey())
                .isPublic(FileVisibilityEnum.isPublic(session.getIsPublic()))
                .uploadId(uploadId)
                .build();
        fileStorage.upload(file, command);

        deleteUploadSession(uploadId);
        return buildUploadVo(session);
    }

    /**
     * 上传分片。
     */
    public FileUploadVo uploadPartFile(MultipartFile file, String uploadId, Integer partNum, Integer totalParts) {
        FileUploadSession session = getAndValidateUploadSession(uploadId);
        if (!isPartUpload(session.getIsPart())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "该 uploadId 非分片上传使用");
        }
        if (partNum == null || totalParts == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST);
        }
        validateFileSize(file.getSize());

        boolean isPublic = FileVisibilityEnum.isPublic(session.getIsPublic());
        FileUploadCommand command = FileUploadCommand.builder()
                .objectKey(session.getObjectKey())
                .isPublic(isPublic)
                .uploadId(uploadId)
                .partNum(partNum)
                .totalParts(totalParts)
                .partSize(file.getSize())
                .build();
        fileStorage.uploadPart(file, command);

        return FileUploadVo.builder()
                .uploadId(uploadId)
                .uri(session.getObjectKey())
                .bucketName(fileStorage.getBucketName(isPublic))
                .hostName(fileStorage.getHostname(isPublic))
                .build();
    }

    /**
     * 完成分片上传。
     */
    public FileUploadVo completeUpload(String uploadId) {
        FileUploadSession session = getAndValidateUploadSession(uploadId);
        boolean isPublic = FileVisibilityEnum.isPublic(session.getIsPublic());
        FileUploadCommand command = FileUploadCommand.builder()
                .objectKey(session.getObjectKey())
                .isPublic(isPublic)
                .uploadId(uploadId)
                .build();

        validateMultipartTotalSize(command, session.getFileSize());
        fileStorage.completeMultipartUpload(command);

        deleteUploadSession(uploadId);
        return buildUploadVo(session);
    }

    /**
     * 取消上传。
     */
    public void abortUpload(String uploadId) {
        FileUploadSession session = getAndValidateUploadSession(uploadId);
        fileStorage.abortMultipartUpload(uploadId, session.getObjectKey());
        deleteUploadSession(uploadId);
    }

    /**
     * 获取文件访问 URL。
     */
    public FileAccessUrlVo getAccessUrl(String objectKey) {
        boolean isPublic = isPublicByKeyName(objectKey);
        FileAccessCommand command = FileAccessCommand.builder()
                .objectKey(objectKey)
                .isPublic(isPublic)
                .build();
        FileAccessResult result = fileStorage.generateAccessUrl(command);
        return FileAccessUrlVo.builder()
                .objectKey(objectKey)
                .accessUrl(result.getAccessUrl())
                .isPublic(isPublic)
                .build();
    }

    /**
     * 批量获取文件访问 URL。
     */
    public List<FileAccessUrlVo> batchGetAccessUrls(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return Collections.emptyList();
        }
        return objectKeys.stream().map(this::getAccessUrl).collect(Collectors.toList());
    }

    /**
     * 获取分片列表。
     */
    public List<String> listParts(String uploadId) {
        FileUploadSession session = getAndValidateUploadSession(uploadId);
        FileUploadCommand command = FileUploadCommand.builder()
                .objectKey(session.getObjectKey())
                .isPublic(FileVisibilityEnum.isPublic(session.getIsPublic()))
                .uploadId(uploadId)
                .build();
        return fileStorage.listParts(command);
    }

    /**
     * Base64 图片上传。
     */
    public FileUploadVo uploadBase64(String prefixDir, String imageBase64) {
        validatePrefixDir(prefixDir);
        String sanitizedBase64 = parseBase64(imageBase64);
        byte[] bytes = Base64.getDecoder().decode(sanitizedBase64.getBytes());
        validateFileSuffix("png");
        validateFileSize(bytes.length);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            String objectKey = keyGenerator.generate(fileProperty, true, "png", prefixDir);
            FileUploadCommand command = FileUploadCommand.builder()
                    .objectKey(objectKey)
                    .isPublic(true)
                    .build();
            fileStorage.uploadByStream(inputStream, command);

            FileUploadSession session = FileUploadSession.builder()
                    .objectKey(objectKey)
                    .isPublic(FileVisibilityEnum.PUBLIC.getCode())
                    .suffix("png")
                    .build();
            return buildUploadVo(session);
        } catch (Exception e) {
            log.warn("Base64 图片上传失败", e);
            throw new ServiceException(GlobalErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private void validateMultipartTotalSize(FileUploadCommand command, Long expectedSize) {
        if (expectedSize == null || expectedSize <= 0) {
            return;
        }
        long uploadedPartTotalSize = fileStorage.getUploadedPartTotalSize(command);
        if (uploadedPartTotalSize != expectedSize) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(),
                    "分片总大小与源文件大小不一致");
        }
    }

    private void cacheUploadSession(FileUploadSession session) {
        redisCache.setCacheObject(getUploadSessionKey(session.getUploadId()),
                JSONObject.toJSONString(session), UPLOAD_EXPIRE_SECONDS, TimeUnit.SECONDS);
    }

    private FileUploadSession getAndValidateUploadSession(String uploadId) {
        String json = redisCache.getCacheObject(getUploadSessionKey(uploadId));
        if (CharSequenceUtil.isBlank(json)) {
            throw new ServiceException(GlobalErrorCode.FILE_UPLOAD_EXPIRED);
        }
        return JSONObject.parseObject(json, FileUploadSession.class);
    }

    private void deleteUploadSession(String uploadId) {
        redisCache.deleteObject(getUploadSessionKey(uploadId));
    }

    private String getUploadSessionKey(String uploadId) {
        return UPLOAD_SESSION_KEY_PREFIX + uploadId;
    }

    private FileUploadVo buildUploadVo(FileUploadSession session) {
        boolean isPublic = FileVisibilityEnum.isPublic(session.getIsPublic());
        FileAccessCommand command = FileAccessCommand.builder()
                .objectKey(session.getObjectKey())
                .isPublic(isPublic)
                .build();

        String fileUrl = appendOriginName(fileStorage.buildDirectUrl(command), session.getOriginName());
        String signUrl = "";
        if (!isPublic) {
            signUrl = fileStorage.generateAccessUrl(command).getAccessUrl();
        }

        return FileUploadVo.builder()
                .bucketName(fileStorage.getBucketName(isPublic))
                .hostName(fileStorage.getHostname(isPublic))
                .uri(session.getObjectKey())
                .fileUrl(fileUrl)
                .signUrl(signUrl)
                .uploadId(session.getUploadId())
                .build();
    }

    private String appendOriginName(String fileUrl, String originName) {
        if (CharSequenceUtil.isBlank(originName)) {
            return fileUrl;
        }
        return fileUrl + "?originName=" + originName;
    }

    private void validateFileSuffix(String suffix) {
        if (!FileExtensionWhitelist.isAllowed(suffix)) {
            throw new ServiceException(GlobalErrorCode.FILE_FORMAT_NOT_SUPPORTED);
        }
    }

    private void validateSuffix(String suffix) {
        if (CharSequenceUtil.isBlank(suffix)) {
            throw new ServiceException(GlobalErrorCode.FILE_PART_SUFFIX_REQUIRED);
        }
    }

    private void validatePrefixDir(String prefixDir) {
        if (CharSequenceUtil.isBlank(prefixDir)) {
            return;
        }
        if (prefixDir.contains("/")) {
            throw new ServiceException(GlobalErrorCode.FILE_PREFIX_DIR_NOT_SUPPORT_MULTI_LEVEL);
        }
        if (prefixDir.length() > 30) {
            throw new ServiceException(GlobalErrorCode.FILE_PREFIX_DIR_TOO_LONG);
        }
    }

    private void validateFileSize(long fileSize) {
        Long maxFileSize = fileProperty.getMaxFileSize();
        if (maxFileSize != null && fileSize > maxFileSize) {
            throw new ServiceException(GlobalErrorCode.FILE_SIZE_EXCEEDS_LIMIT);
        }
    }

    private boolean isPublicByKeyName(String objectKey) {
        return objectKey.startsWith(fileProperty.getPublicPrefix() + "/");
    }

    private boolean isPartUpload(Integer isPart) {
        return isPart != null && isPart == 1;
    }

    private String parseBase64(String imageBase64) {
        if (CharSequenceUtil.isBlank(imageBase64)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "Base64 图片不能为空");
        }
        int baseIndex = imageBase64.indexOf(";base64,");
        if (baseIndex >= 0) {
            return imageBase64.substring(baseIndex + 8);
        }
        return imageBase64;
    }
}
