package com.opsagent.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Alert {
    private String id;
    private String source;      // 监控系统名称
    private String severity;    // CRITICAL, WARNING, INFO
    private String title;
    private String description;
    private String serviceName;
    private String host;
    private String detail;
    private LocalDateTime timestamp;
    private boolean processed;
    private String status;      // PENDING, ANALYZING, RESOLVED, FAILED
}