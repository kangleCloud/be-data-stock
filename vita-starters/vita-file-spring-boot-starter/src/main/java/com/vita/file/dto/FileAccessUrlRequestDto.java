package com.vita.file.dto;

import lombok.Data;

import java.util.List;

/**
 * 文件访问 URL 请求 DTO。
 */
@Data
public class FileAccessUrlRequestDto {

    /**
     * 对象键列表。
     */
    private List<String> objectKeys;
}
