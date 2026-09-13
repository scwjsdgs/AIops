package com.opsagent.controller;

import com.opsagent.dto.ApiResponse;
import com.opsagent.model.ToolExecutionRequest;
import com.opsagent.model.ToolExecutionResult;
import com.opsagent.service.ToolRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolController {


    private final ToolRegistryService toolRegistry;

    @GetMapping("/list")
    public ApiResponse<?> listTools() {
        // 返回工具名称和描述列表
        return ApiResponse.success(toolRegistry.getAllToolsInfo());
    }

    @PostMapping("/execute")
    public Mono<ApiResponse<ToolExecutionResult>> execute(@RequestBody ToolExecutionRequest request) {
        // 工具执行是阻塞的（K8s HTTP 调用、SSH、clear_cache 里的 Thread.sleep），
        // 必须挪到 boundedElastic，否则跑在 Netty event loop 上会打印
        // "Blocking call! ... blocked for Nms" 并拖垮整个服务。
        // 注意这里走的是不带白名单的 execute —— 前端手工调用，generic_command 仍可用。
        return Mono.fromCallable(() -> ApiResponse.success(toolRegistry.execute(request)))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
