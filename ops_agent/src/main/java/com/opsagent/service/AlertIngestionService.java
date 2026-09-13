package com.opsagent.service;

import com.opsagent.entity.AlertEntity;
import com.opsagent.model.Alert;
import com.opsagent.repository.AlertRepository;
import com.opsagent.utils.JsonUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlertIngestionService {

    private final TaskSchedulerService taskScheduler;
    private final AgentCallbackService agentCallbackService;
    private final AlertRepository alertRepository;
    private final MeterRegistry meterRegistry;

    private Counter alertReceivedCounter;
    private Counter alertProcessedCounter;
    private Counter alertFailedCounter;

    @PostConstruct
    public void initMetrics() {
        alertReceivedCounter = Counter.builder("agent.alert.received")
                .description("Total alerts received")
                .register(meterRegistry);
        alertProcessedCounter = Counter.builder("agent.alert.processed")
                .description("Alerts successfully processed")
                .register(meterRegistry);
        alertFailedCounter = Counter.builder("agent.alert.failed")
                .description("Alerts failed to process")
                .register(meterRegistry);
    }

    /**
     * 注意这里没有 @Transactional —— 它对返回 Mono 的方法完全无效，
     * 只会让人误以为 JPA 操作在一个事务里。真正的修正是把阻塞调用挪到
     * boundedElastic，而不是加个不起作用的注解。
     */
    public Mono<Void> processAlert(Alert alert) {
        alertReceivedCounter.increment();
        log.info("Received alert: {}", alert);

        // 1. 保存告警到数据库
        AlertEntity entity = new AlertEntity();
        BeanUtils.copyProperties(alert, entity);
        entity.setId(alert.getId() != null ? alert.getId() : UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setProcessed(false);
        entity.setStatus("PENDING");

        // 2. 创建根因分析任务
        return Mono.fromCallable(() -> alertRepository.save(entity))
                // JPA 是阻塞 IO，直接跑在 Netty event loop 上，并发一上来整个服务会被拖死
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(saved -> {
                    String alertId = saved.getId();
                    return taskScheduler.createTask("ROOT_CAUSE_ANALYSIS", alertId, alert.getDescription())
                            .flatMap(task -> {
                                // 这里是 fire-and-forget，绝不能等 agent 跑完。
                                // ReAct 循环要几十秒到几分钟，而前端 axios 只等 10 秒，
                                // 同步等结果会让 POST /api/alerts 必定超时。
                                agentCallbackService.notifyAgent(task.getId(), alert.getDescription())
                                        .subscribe(
                                                v -> markAlert(alertId, "ANALYZING", true),
                                                e -> markAlert(alertId, "FAILED", false));
                                return Mono.just(task);
                            });
                })
                .then();
    }

    private void markAlert(String alertId, String status, boolean processed) {
        Mono.fromRunnable(() -> alertRepository.findById(alertId).ifPresent(e -> {
                    e.setStatus(status);
                    e.setProcessed(processed);
                    alertRepository.save(e);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(v -> { },
                        e -> log.error("更新告警状态失败 alertId={} status={}", alertId, status, e));
    }

    @KafkaListener(topics = "alerts", groupId = "agent-group")
    public void consumeAlert(String message) {
        Alert alert = JsonUtils.fromJson(message, Alert.class);
        if (alert == null) {
            // JsonUtils 已经打过反序列化失败的日志；这里补上业务后果，
            // 否则这条告警就凭空消失了，排查时毫无线索
            log.error("告警反序列化失败，消息被丢弃: {}", message);
            alertFailedCounter.increment();
            return;
        }
        if (alert.getId() == null || alert.getId().isEmpty()) {
            alert.setId(UUID.randomUUID().toString());
        }
        processAlert(alert).subscribe(
                v -> { },
                e -> {
                    log.error("处理告警失败 alertId={}", alert.getId(), e);
                    alertFailedCounter.increment();
                });
    }
}
