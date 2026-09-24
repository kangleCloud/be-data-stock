package com.vita.workflow.reimbursement.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @BelongsProject: be-vita
 * @BelongsPackage: com.vita.workflow.reimbursement.vo
 * @Author: znk
 * @CreateTime: 2026-07-20
 * @Description: 报销单详情展示对象
 * @Version: 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReimbursementDetailVo extends ReimbursementPageVo {

    private String reason;
    private String remark;
}
