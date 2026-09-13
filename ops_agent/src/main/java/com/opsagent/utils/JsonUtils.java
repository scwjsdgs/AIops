package com.opsagent.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JsonUtils {
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

    /**
     * 必须是配置过的 mapper，不能裸 new ObjectMapper()。
     *
     * 裸 mapper 不带 JavaTimeModule：Kafka 告警消息里只要出现 timestamp
     * （LocalDateTime），readValue 就抛 InvalidDefinitionException → fromJson 返回 null
     * → AlertIngestionService.consumeAlert 里 `if (alert != null)` 不成立
     * → 告警被静默丢弃，日志里只有一行 deserialization error，没有任何业务痕迹。
     */
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON serialization error", e);
            return null;
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON deserialization error: {}", json, e);
            return null;
        }
    }
}
