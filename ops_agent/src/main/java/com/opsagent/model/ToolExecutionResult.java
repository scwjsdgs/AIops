package com.opsagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor   // 生成全参构造方法
@NoArgsConstructor    // 生成无参构造方法
public class ToolExecutionResult {
    private boolean success;
    private String message;
    private Object data;
    private long durationMs;
}