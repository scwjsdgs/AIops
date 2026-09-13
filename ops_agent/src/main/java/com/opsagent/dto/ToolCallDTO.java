package com.opsagent.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ToolCallDTO {
    private String toolName;
    private Map<String, Object> parameters;
    private String callbackUrl;   // Agent 回调地址（可选）
}