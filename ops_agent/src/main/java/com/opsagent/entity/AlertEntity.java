package com.opsagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertEntity {
    @Id
    private String id;

    private String source;

    private String severity;

    private String title;

    private String description;

    @Column(name = "service_name")
    private String serviceName;

    private String host;

    @Column(columnDefinition = "TEXT")
    private String detail;

    private LocalDateTime timestamp;

    private boolean processed;

    private String status;   // PENDING, ANALYZING, RESOLVED, FAILED

    @Column(name = "create_time")
    private LocalDateTime createTime;
}