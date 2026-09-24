package com.vita.workflow.reimbursement.service;

import com.vita.core.page.PageResponse;
import com.vita.workflow.api.event.WorkflowChangedEvent;
import com.vita.workflow.reimbursement.dto.*;
import com.vita.workflow.reimbursement.vo.ReimbursementDetailVo;
import com.vita.workflow.reimbursement.vo.ReimbursementPageVo;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.reimbursement.service
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 报销工作流业务服务
 * @Version: 1.0
 */
public interface WorkflowReimbursementService {

    /**
     * 创建当前用户的报销草稿。
     *
     * @param createDto 创建参数
     * @return 报销单 ID
     */
    Long create(ReimbursementCreateDto createDto);

    /**
     * 修改当前用户可编辑的报销单。
     *
     * @param id 报销单 ID
     * @param updateDto 修改参数
     */
    void update(Long id, ReimbursementUpdateDto updateDto);

    /**
     * 分页查询当前用户的报销单。
     *
     * @param searchDto 查询参数
     * @return 分页结果
     */
    PageResponse<ReimbursementPageVo> mine(ReimbursementSearchDto searchDto);

    /**
     * 管理端分页查询报销单。
     *
     * @param searchDto 查询参数
     * @return 分页结果
     */
    PageResponse<ReimbursementPageVo> page(ReimbursementSearchDto searchDto);

    /**
     * 查询报销单详情。
     *
     * @param id 报销单 ID
     * @param administrator 是否管理员查询
     * @return 报销单详情
     */
    ReimbursementDetailVo detail(Long id, boolean administrator);

    /**
     * 提交草稿或退回报销单。
     *
     * @param id 报销单 ID
     * @param submitDto 提交参数
     * @return 报销单详情
     */
    ReimbursementDetailVo submit(Long id, ReimbursementSubmitDto submitDto);

    /**
     * 撤回尚未发生审批的报销单。
     *
     * @param id 报销单 ID
     * @param withdrawDto 撤回参数
     * @return 报销单详情
     */
    ReimbursementDetailVo withdraw(Long id, ReimbursementWithdrawDto withdrawDto);

    /**
     * 按业务 ID 重同步报销状态。
     *
     * @param businessId 全局业务 ID
     * @return 报销单详情
     */
    ReimbursementDetailVo resync(String businessId);

    /**
     * 根据提交后工作流事件同步业务状态。
     *
     * @param event 工作流事件
     */
    void syncFromEvent(WorkflowChangedEvent event);
}
