package com.sky.agent.core;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek API 配置属性
 * 对应 application.yml 中的 sky.agent.deepseek.*
 */
@Component
@ConfigurationProperties(prefix = "sky.agent.deepseek")
@Data
public class DeepSeekConfig {
    /**
     * DeepSeek API Key
     */
    private String apiKey;

    /**
     * API 基础地址，默认 https://api.deepseek.com
     */
    private String baseUrl = "https://api.deepseek.com";

    /**
     * 模型名称，默认 deepseek-chat
     */
    private String model = "deepseek-chat";

    /**
     * 最大 token 数
     */
    private Integer maxTokens = 4096;

    /**
     * 温度参数（0-2），SQL 场景建议设低
     */
    private Double temperature = 0.1;

    /**
     * Agent 最大迭代轮数
     */
    private Integer maxIterations = 10;

    /**
     * HTTP 请求超时（秒）
     */
    private Integer requestTimeout = 60;
}
