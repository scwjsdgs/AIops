package com.opsagent.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {

        ObjectMapper mapper = new ObjectMapper();

        // 1) 没有 JavaTimeModule，序列化 Task.createdAt（LocalDateTime）会直接抛
        //    InvalidDefinitionException —— 是写入阶段就失败，不只是读回来的问题。
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 2) 没有默认类型信息，Jackson2JsonRedisSerializer 读回来的是 LinkedHashMap，
        //    TaskSchedulerService 里的 .cast(Task.class) 会抛 ClassCastException。
        //    注意：激活默认类型信息会带来反序列化 gadget 风险，这里用
        //    BasicPolymorphicTypeValidator 限制可还原的类型范围。
        PolymorphicTypeValidator validator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubType("com.opsagent.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .build();
        mapper.activateDefaultTyping(
                validator,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(mapper);
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisSerializationContext<String, Object> context =
                RedisSerializationContext.<String, Object>newSerializationContext(keySerializer)
                        .value(valueSerializer)
                        .hashKey(keySerializer)
                        .hashValue(valueSerializer)
                        .build();
        return new ReactiveRedisTemplate<>(factory, context);
    }
}
