package com.opsagent.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsagent.model.AgentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 推送中枢。
 *
 * 关键设计：这里**不再持有 WebSocketSession**，每个连接拿到一个自己的 Sinks.Many，
 * 由 AgentWebSocketHandler 把它和心跳 merge 成唯一一条出站流。
 *
 * 改造前 pushMessage 里直接 session.send(...).subscribe()，与 handler 里的
 * session.send(pingFlux) 并发写同一个会话，Reactor 会抛
 * IllegalStateException: Concurrent send not allowed —— agent 每推进一步就炸一次。
 * 而且那次 send 的返回值被丢弃，失败也没人知道。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketPushService {

    /** taskId -> 订阅该任务的所有连接。一个任务可能同时被多个页面订阅。 */
    private final Map<String, Set<Sinks.Many<String>>> sinks = new ConcurrentHashMap<>();

    private final Sinks.Many<AgentMessage> messageSink = Sinks.many().replay().latest();
    private final ObjectMapper objectMapper;

    /** 注册一个连接，返回它专属的出站 sink。 */
    public Sinks.Many<String> register(String taskId) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        sinks.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet()).add(sink);
        return sink;
    }

    public void unregister(String taskId, Sinks.Many<String> sink) {
        Set<Sinks.Many<String>> set = sinks.get(taskId);
        if (set == null) {
            return;
        }
        set.remove(sink);
        if (set.isEmpty()) {
            sinks.remove(taskId, set);
        }
    }

    public void pushMessage(String taskId, AgentMessage message) {
        // 无论有没有 WebSocket 订阅者，都往广播流里放一份，供其它订阅者消费
        messageSink.tryEmitNext(message);

        Set<Sinks.Many<String>> set = sinks.get(taskId);
        if (set == null || set.isEmpty()) {
            // 前端没连着不是错误：agent 照常推理，结果仍会写进 Task
            log.debug("任务 {} 当前没有 WebSocket 订阅者，跳过推送", taskId);
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("序列化 AgentMessage 失败，taskId={}", taskId, e);
            return;
        }

        for (Sinks.Many<String> sink : set) {
            Sinks.EmitResult result = sink.tryEmitNext(json);
            if (result.isFailure()) {
                log.warn("推送失败 taskId={} result={}", taskId, result);
            }
        }
    }

    // 广播消息（可选）
    public Flux<AgentMessage> getMessageStream() {
        return messageSink.asFlux();
    }
}
