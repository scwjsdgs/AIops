package com.opsagent.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AgentMessage {
    private String type;          // START, STEP, TOOL_CALL, RESULT, ERROR
    private String taskId;
    private String content;
    private String stepName;      // 当前步骤名称
    private LocalDateTime timestamp;
}