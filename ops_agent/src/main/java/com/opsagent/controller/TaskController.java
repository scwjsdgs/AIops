package com.opsagent.controller;

import com.opsagent.dto.ApiResponse;
import com.opsagent.model.Task;
import com.opsagent.service.TaskSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {


    private final TaskSchedulerService taskScheduler;

    @GetMapping("/{id}")
    public Mono<ApiResponse<Task>> getTask(@PathVariable String id) {
        return taskScheduler.getTask(id)
                .map(ApiResponse::<Task>success)
                .switchIfEmpty(Mono.just(ApiResponse.<Task>error(404, "Task not found")));
    }
}