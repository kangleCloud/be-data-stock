package com.vita.file.controller;

import com.vita.core.CommonResult;
import com.vita.file.dto.FileAccessUrlRequestDto;
import com.vita.file.dto.FileUploadRequestDto;
import com.vita.file.service.FileApplicationService;
import com.vita.file.vo.FileAccessUrlVo;
import com.vita.file.vo.FileUploadVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传控制器。
 */
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    private final FileApplicationService fileApplicationService;

    public FileController(FileApplicationService fileApplicationService) {
        this.fileApplicationService = fileApplicationService;
    }

    /**
     * 获取 uploadId。
     */
    @GetMapping("/upload-id")
    public CommonResult<String> getUploadId(@Validated FileUploadRequestDto dto) {
        String uploadId = fileApplicationService.getUploadId(dto);
        return CommonResult.success(uploadId);
    }

    /**
     * 校验 uploadId 有效性。
     */
    @GetMapping("/valid/upload-id")
    public CommonResult<Boolean> validUploadId(@RequestParam String uploadId) {
        boolean valid = fileApplicationService.validateUploadId(uploadId);
        return CommonResult.success(valid);
    }

    /**
     * 单文件上传。
     */
    @PostMapping("/upload")
    public CommonResult<FileUploadVo> uploadSingleFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam String uploadId) {
        FileUploadVo result = fileApplicationService.uploadSingleFile(file, uploadId);
        return CommonResult.success(result);
    }

    /**
     * 分片上传。
     */
    @PostMapping("/upload/part")
    public CommonResult<FileUploadVo> uploadPart(
            @RequestParam("file") MultipartFile file,
            @RequestParam String uploadId,
            @RequestParam Integer partNum,
            @RequestParam Integer totalParts) {
        FileUploadVo result = fileApplicationService.uploadPartFile(file, uploadId, partNum, totalParts);
        return CommonResult.success(result);
    }

    /**
     * 完成分片上传。
     */
    @GetMapping("/upload/complete")
    public CommonResult<FileUploadVo> completeUpload(@RequestParam String uploadId) {
        FileUploadVo result = fileApplicationService.completeUpload(uploadId);
        return CommonResult.success(result);
    }

    /**
     * 取消上传。
     */
    @PostMapping("/upload/abort")
    public CommonResult<Void> abortUpload(@RequestParam String uploadId) {
        fileApplicationService.abortUpload(uploadId);
        return CommonResult.success(null);
    }

    /**
     * 获取分片列表。
     */
    @GetMapping("/parts")
    public CommonResult<List<String>> listParts(@RequestParam String uploadId) {
        List<String> parts = fileApplicationService.listParts(uploadId);
        return CommonResult.success(parts);
    }

    /**
     * 批量获取文件访问 URL（私有文件返回签名 URL）。
     */
    @PostMapping("/access-url")
    public CommonResult<List<FileAccessUrlVo>> getAccessUrls(@RequestBody FileAccessUrlRequestDto dto) {
        List<FileAccessUrlVo> result = fileApplicationService.batchGetAccessUrls(dto.getObjectKeys());
        return CommonResult.success(result);
    }

    /**
     * Base64 图片上传。
     */
    @PostMapping("/upload/base64")
    public CommonResult<FileUploadVo> uploadBase64(
            @RequestParam(required = false, defaultValue = "") String prefixDir,
            @RequestParam("file") String imageBase64) {
        FileUploadVo result = fileApplicationService.uploadBase64(prefixDir, imageBase64);
        return CommonResult.success(result);
    }
}
