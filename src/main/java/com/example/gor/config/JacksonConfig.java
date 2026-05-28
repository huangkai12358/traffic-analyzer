package com.example.gor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 配置。
 *
 * <p>当前项目没有引入 Web starter，因此不会自动创建 ObjectMapper；这里显式提供一个 Bean，
 * 供导入服务将 headers map 序列化为 headers_json。</p>
 */
@Configuration
public class JacksonConfig {
    /**
     * 创建全局 ObjectMapper。
     *
     * @return Jackson ObjectMapper Bean
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
