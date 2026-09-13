package com.opsagent.dto;


import com.opsagent.model.Alert;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlertDTO {
    private String id;
    private String source;
    private String severity;
    private String title;
    private String description;
    private String serviceName;
    private String host;
    private String detail;
    private LocalDateTime timestamp;
    private boolean processed;
    private String status;

    public static AlertDTO fromEntity(Alert alert) {
        AlertDTO dto = new AlertDTO();
        dto.setId(alert.getId());
        dto.setSource(alert.getSource());
        dto.setSeverity(alert.getSeverity());
        dto.setTitle(alert.getTitle());
        dto.setDescription(alert.getDescription());
        dto.setServiceName(alert.getServiceName());
        dto.setHost(alert.getHost());
        dto.setDetail(alert.getDetail());
        dto.setTimestamp(alert.getTimestamp());
        dto.setProcessed(alert.isProcessed());
        dto.setStatus(alert.getStatus());
        return dto;
    }

    public Alert toEntity() {
        Alert alert = new Alert();
        alert.setId(this.id);
        alert.setSource(this.source);
        alert.setSeverity(this.severity);
        alert.setTitle(this.title);
        alert.setDescription(this.description);
        alert.setServiceName(this.serviceName);
        alert.setHost(this.host);
        alert.setDetail(this.detail);
        alert.setTimestamp(this.timestamp);
        alert.setProcessed(this.processed);
        alert.setStatus(this.status);
        return alert;
    }
}