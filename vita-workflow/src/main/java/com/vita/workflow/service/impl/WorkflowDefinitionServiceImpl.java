package com.vita.workflow.service.impl;

import com.vita.core.exception.GlobalErrorCode;
import com.vita.core.exception.ServiceException;
import com.vita.workflow.config.ConditionalOnWorkflowEnabled;
import com.vita.workflow.service.IWorkflowDefinitionService;
import com.vita.workflow.support.WorkflowDefinitionPermissionNormalizer;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.service.DefService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 工作流定义服务实现。
 *
 * @author Codex
 */
@Service
@ConditionalOnWorkflowEnabled
public class WorkflowDefinitionServiceImpl implements IWorkflowDefinitionService {

    private final DefService defService;

    private final WorkflowDefinitionPermissionNormalizer permissionNormalizer;

    public WorkflowDefinitionServiceImpl(DefService defService,
                                         WorkflowDefinitionPermissionNormalizer permissionNormalizer) {
        this.defService = defService;
        this.permissionNormalizer = permissionNormalizer;
    }

    /**
     * 导入 Warm-Flow 流程定义 JSON。
     *
     * @param defJson 流程定义对象
     * @return 导入结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean importJson(DefJson defJson) {
        validateDefJson(defJson);
        DefJson normalizedDefJson = permissionNormalizer.normalizeInitialNodePermissions(defJson);
        String defJsonStr = serializeDefJson(normalizedDefJson);
        try {
            defService.importJson(defJsonStr);
            return true;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException(GlobalErrorCode.BUSINESS_EXCEPTION.getCode(), buildImportErrorMessage(ex));
        }
    }

    private void validateDefJson(DefJson defJson) {
        if (defJson == null) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "工作流定义不能为空");
        }
        if (!StringUtils.hasText(defJson.getFlowCode())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程编码 flowCode 不能为空");
        }
        if (!StringUtils.hasText(defJson.getFlowName())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程名称 flowName 不能为空");
        }
        if (CollectionUtils.isEmpty(defJson.getNodeList())) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "流程节点 nodeList 不能为空");
        }
    }

    private String serializeDefJson(DefJson defJson) {
        if (FlowEngine.jsonConvert == null) {
            throw new ServiceException(GlobalErrorCode.SERVICE_UNAVAILABLE.getCode(), "工作流JSON转换器未初始化");
        }
        return FlowEngine.jsonConvert.objToStr(defJson);
    }

    /**
     * 构建导入错误消息。
     *
     * @param ex 异常对象
     * @return 错误消息字符串
     */
    private String buildImportErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return "工作流JSON导入失败";
        }
        return "工作流JSON导入失败：" + message;
    }

    /**
     * 从类路径资源中读取流程定义 JSON。
     *
     * @param resourcePath 资源路径
     * @return 流程定义 JSON 字符串
     */
    private String readDefinitionResource(String resourcePath) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ServiceException(GlobalErrorCode.NOT_FOUND.getCode(), "流程定义资源不存在：" + resourcePath);
        }
    }
}
