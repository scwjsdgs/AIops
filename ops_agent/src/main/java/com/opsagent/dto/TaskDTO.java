package com.opsagent.dto;


import com.opsagent.model.Task;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskDTO {
    private String id;
    private String type;
    private String alertId;
    private String status;
    private String input;
    private String output;
    private List<String> agentSteps;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;

    public static TaskDTO fromEntity(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setType(task.getType());
        dto.setAlertId(task.getAlertId());
        dto.setStatus(task.getStatus());
        dto.setInput(task.getInput());
        dto.setOutput(task.getOutput());
        dto.setAgentSteps(task.getAgentSteps());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        dto.setCreatedBy(task.getCreatedBy());
        return dto;
    }

    public Task toEntity() {
        Task task = new Task();
        task.setId(this.id);
        task.setType(this.type);
        task.setAlertId(this.alertId);
        task.setStatus(this.status);
        task.setInput(this.input);
        task.setOutput(this.output);
        task.setAgentSteps(this.agentSteps);
        task.setCreatedAt(this.createdAt);
        task.setUpdatedAt(this.updatedAt);
        task.setCreatedBy(this.createdBy);
        return task;
    }
}