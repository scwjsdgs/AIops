package com.opsagent.handler;


import com.opsagent.service.WebSocketPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
public class AgentWebSocketHandler implements WebSocketHandler {

    private final WebSocketPushService pushService;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String taskId = resolveTaskId(session);
        Sinks.Many<String> sink = pushService.register(taskId);
        log.info("WebSocket 已连接 taskId={} sessionId={}", taskId, session.getId());

        // 心跳 Ping（每 15 秒发送一次）
        Flux<WebSocketMessage> pingFlux = Flux.interval(Duration.ofSeconds(15))
                .map(i -> session.pingMessage(d -> d.allocateBuffer()))
                .doOnNext(msg -> log.trace("Ping sent to taskId: {}", taskId));

        // 唯一的出站流：业务推送与心跳在这里合流后交给 session.send。
        // 任何地方都不允许再单独对 session 调 send()。
        Flux<WebSocketMessage> outbound = Flux.merge(
                sink.asFlux().map(session::textMessage),
                pingFlux);

        // 接收 Pong 帧并处理（可记录日志）
        Mono<Void> pongHandler = session.receive()
                .doOnNext(msg -> {
                    if (msg.getType() == WebSocketMessage.Type.PONG) {
                        // 收到 Pong，连接存活
                    }
                })
                .then();

        return Mono.zip(session.send(outbound), pongHandler)
                .doFinally(signalType -> {
                    pushService.unregister(taskId, sink);
                    sink.tryEmitComplete();
                    log.info("WebSocket 已断开 taskId={} signal={}", taskId, signalType);
                })
                .then();
    }

    /**
     * 从 query string 里取 taskId。
     *
     * 原实现直接把整串 query 当 taskId，"?taskId=abc&foo=1" 会变成 "abc&foo=1"，
     * 于是推送永远命中不了任何一个订阅者。
     */
    private String resolveTaskId(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        if (query == null || query.isBlank()) {
            return "default";
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && "taskId".equals(pair.substring(0, eq))) {
                String raw = pair.substring(eq + 1);
                try {
                    return URLDecoder.decode(raw, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return raw;
                }
            }
        }
        return "default";
    }
}
