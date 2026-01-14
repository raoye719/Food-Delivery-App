package com.sky.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.serializer.*;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Configuration
@Slf4j
public class RedisConfiguration {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 设置key的序列化方式
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);

        // 设置value的序列化方式，这里以JSON为例
        Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        template.setValueSerializer(valueSerializer);

        // 设置Hash的key和value的序列化方式
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }


        @Bean
        public RedisTemplate<String, Long> longRedisTemplate(RedisConnectionFactory factory) {
            RedisTemplate<String, Long> template = new RedisTemplate<>();
            template.setConnectionFactory(factory);

            // 使用StringRedisSerializer序列化key
            template.setKeySerializer(new StringRedisSerializer());

            // 使用GenericToStringSerializer序列化value为Long
            template.setValueSerializer(new GenericToStringSerializer<>(Long.class));

            // 对于Hash结构也做同样配置
            template.setHashKeySerializer(new GenericToStringSerializer<>(Long.class));
            template.setHashValueSerializer(new GenericToStringSerializer<>(Integer.class));

            template.afterPropertiesSet();
            return template;
        }
    }


