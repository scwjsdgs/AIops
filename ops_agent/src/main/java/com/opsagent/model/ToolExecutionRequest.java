package com.opsagent.model;

import lombok.Data;
import java.util.Map;

@Data
public class ToolExecutionRequest {
    private String toolName;
    private Map<String, Object> parameters;
    private String contextId;   // 关联任务或告警
}