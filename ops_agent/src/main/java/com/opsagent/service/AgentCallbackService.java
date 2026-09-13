package com.opsagent.service;

import com.opsagent.model.AgentMessage;
import com.opsagent.model.ToolExecutionRequest;
import com.opsagent.model.ToolExecutionResult;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.LinkedHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCallbackService {

    private final ToolRegistryService toolRegistry;
    private final TaskSchedulerService taskScheduler;
    private final WebSocketPushService webSocketPushService;
    private final WebClient webClient;

    @Value("${opsagent.agent.url:http://localhost:5000}")
    private String agentBaseUrl;
    @Value("${opsagent.agent.read-timeout:10000}")
    private int readTimeout;

    @Retry(name = "agentCall", fallbackMethod = "fallbackNotifyAgent")
    @CircuitBreaker(name = "agentCall")
    @Bulkhead(name = "agentCall", type = Bulkhead.Type.SEMAPHORE)
    public Mono<Void> notifyAgent(String taskId, String alertDescription) {
        AgentRequest request = new AgentRequest(taskId, alertDescription);
        return webClient.post()
                .uri(agentBaseUrl + "/api/agent/start")
                .bodyValue(request)
                .exchangeToMono(resp -> {
                    int code = resp.statusCode().value();
                    if (resp.statusCode().is2xxSuccessful()) {
                        // Python 侧已改成立即返回 202，这里收到 2xx 就算通知成功
                        return resp.releaseBody().then();
                    }
                    if (code == 409) {
                        // 409 = 同一 taskId 的 ReAct 循环正在跑。这不是失败，
                        // 而且正是我们要的互斥结果，重发只会制造第二份推理。
                        log.warn("任务 {} 已在 agent 侧执行中（409），不再重复投递", taskId);
                        return resp.releaseBody().then();
                    }
                    return resp.createException().flatMap(Mono::error);
                })
                // 刻意不加 retryWhen：/api/agent/start 会拉起一整个 ReAct 循环，
                // 它不是幂等操作。原实现叠了 @Retry(3) × retryWhen(3) = 最多 12 次投递，
                // 一次网络抖动就能让同一个告警被分析 12 遍、重启被执行 12 次。
                .timeout(Duration.ofMillis(readTimeout))
                .doOnSuccess(v -> log.info("Successfully notified Agent for task {}", taskId))
                .doOnError(e -> log.error("Failed to notify Agent for task {}", taskId, e));
    }

    // 降级方法
    public Mono<Void> fallbackNotifyAgent(String taskId, String alertDescription, Throwable t) {
        log.error("通知 Agent 失败，任务 {} 标记为 FAILED: {}", taskId, t.getMessage());
        // 原实现只是 log 一行然后 Mono.empty() —— 任务永远停在 PENDING，
        // 前端看到一个"正在分析中"的僵尸任务，实际什么都没发生
        return taskScheduler.completeTask(taskId, "通知 agent 失败：" + t.getMessage(), "FAILED").then();
    }

    public Mono<Void> handleAgentStep(AgentMessage message) {
        webSocketPushService.pushMessage(message.getTaskId(), message);
        // 这一步同时完成两件事：把任务从 PENDING 推到 RUNNING，并落一条 agentSteps。
        // 改造前 updateTaskStatus 从未被调用过，前端状态标签永远是 PENDING。
        return taskScheduler.appendStepAndMarkRunning(message.getTaskId(), describe(message))
                .then();
    }

    public Mono<ToolExecutionResult> executeTool(ToolExecutionRequest request) {
        // forAgent=true：按 opsagent.tools.agent-exposed 白名单校验，
        // generic_command（执行任意 shell）不在这份名单里
        return Mono.fromCallable(() -> toolRegistry.execute(request, true))
                // 工具是阻塞的（K8s HTTP、SSH、restart 里的 sleep），
                // 跑在 event loop 上会打印 "Blocking call! blocked for 3000ms"
                .subscribeOn(Schedulers.boundedElastic())
                // 刻意不加 retryWhen：这个调用下面挂着 restart_service / scale_up / rollback，
                // 对它们自动重试就是在故障现场反复改线上状态。
                // 失败就以 success=false 返回给 LLM，由 LLM 显式决定是否再来一次（可审计）。
                .onErrorResume(e -> {
                    log.error("工具执行失败 tool={}", request.getToolName(), e);
                    return Mono.just(new ToolExecutionResult(
                            false, "Tool execution failed: " + e.getMessage(),
                            new LinkedHashMap<String, Object>(), 0));
                });
    }

    public Mono<Void> taskComplete(String taskId, String result) {
        return taskComplete(taskId, result, "SUCCESS");
    }

    /**
     * @param status SUCCESS 或 FAILED。
     *               agent 侧推理失败时必须落 FAILED —— 原实现无论成败都写 SUCCESS，
     *               一个失败的分析在界面上看起来是"已完成"。
     */
    public Mono<Void> taskComplete(String taskId, String result, String status) {
        return taskScheduler.completeTask(taskId, result, status).then();
    }

    /** 把 AgentMessage 压成一行便于前端展示与排查的摘要。 */
    private String describe(AgentMessage m) {
        StringBuilder sb = new StringBuilder();
        if (m.getStepName() != null && !m.getStepName().isBlank()) {
            sb.append('[').append(m.getStepName()).append("] ");
        }
        if (m.getType() != null && !m.getType().isBlank()) {
            sb.append('(').append(m.getType()).append(") ");
        }
        if (m.getContent() != null) {
            sb.append(m.getContent());
        }
        return sb.toString().trim();
    }

    static class AgentRequest {
        public String taskId;
        public String description;
        public AgentRequest(String taskId, String description) {
            this.taskId = taskId;
            this.description = description;
        }
    }
}
