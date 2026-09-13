package com.opsagent.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Task {
    private String id;
    private String type;            // ROOT_CAUSE_ANALYSIS, REPAIR, REPORT
    private String alertId;         // 关联告警ID
    private String status;          // PENDING, RUNNING, SUCCESS, FAILED
    private String input;           // 输入参数（JSON）
    private String output;          // 输出结果（JSON）
    private List<String> agentSteps; // Agent 推理步骤摘要
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;       // 系统或用户
}