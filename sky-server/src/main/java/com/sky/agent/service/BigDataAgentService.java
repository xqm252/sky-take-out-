package com.sky.agent.service;

import com.sky.agent.core.Collections8;
import com.sky.agent.core.DeepSeekConfig;
import com.sky.agent.core.ToolUseLoop;
import com.sky.vo.AgentChatVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * 大数据 Agent 服务
 * 封装 ToolUseLoop，提供 HiveQL/Spark SQL 数据仓库分析对话能力
 */
@Service
@Slf4j
public class BigDataAgentService {

    @Autowired
    private ToolUseLoop toolUseLoop;

    @Autowired
    private DeepSeekConfig config;

    private String systemPrompt;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("agent/bigdata-agent-system-prompt.md");
            systemPrompt = new String(Collections8.readAllBytes(resource.getInputStream()), StandardCharsets.UTF_8);
            log.info("BigData Agent system prompt loaded: {} characters", systemPrompt.length());
        } catch (Exception e) {
            log.error("Failed to load bigdata agent system prompt", e);
            systemPrompt = "你是苍穹外卖大数据分析专家。使用 HiveQL/Spark SQL 工具查询 HDFS 数据仓库。";
        }
    }

    /**
     * 执行一次大数据 Agent 对话
     *
     * @param userMessage 用户问题
     * @return Agent 回复
     */
    public AgentChatVO chat(String userMessage) {
        long startTime = System.currentTimeMillis();

        // 验证 API Key
        if (config.getApiKey() == null || Collections8.isBlank(config.getApiKey())
                || config.getApiKey().startsWith("your-")) {
            return AgentChatVO.builder()
                    .answer("⚠️ DeepSeek API Key 未配置。请在环境变量 DEEPSEEK_API_KEY 中设置有效的 API Key。")
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .toolsUsed(new ArrayList<>())
                    .build();
        }

        log.info("BigData Agent received: {}", userMessage);
        String answer = toolUseLoop.execute(userMessage, systemPrompt);
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("BigData Agent completed in {}ms", elapsed);

        return AgentChatVO.builder()
                .answer(answer)
                .responseTimeMs(elapsed)
                .toolsUsed(new ArrayList<>())
                .build();
    }
}
