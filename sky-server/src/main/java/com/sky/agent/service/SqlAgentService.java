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
import java.util.List;

/**
 * SQL Agent 服务
 * 封装 ToolUseLoop，提供 SQL 查询对话能力
 */
@Service
@Slf4j
public class SqlAgentService {

    @Autowired
    private ToolUseLoop toolUseLoop;

    @Autowired
    private DeepSeekConfig config;

    /**
     * SQL Agent 系统提示词（从 classpath 加载并缓存在内存中）
     */
    private String systemPrompt;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("agent/sql-agent-system-prompt.md");
            systemPrompt = new String(Collections8.readAllBytes(resource.getInputStream()), StandardCharsets.UTF_8);
            log.info("SQL Agent system prompt loaded: {} characters", systemPrompt.length());
        } catch (Exception e) {
            log.error("Failed to load SQL agent system prompt", e);
            // 回退到内置简短提示词
            systemPrompt = "你是苍穹外卖 MySQL SQL 专家。使用工具查询数据库，帮助用户编写和优化 SQL。";
        }
    }

    /**
     * 执行一次 SQL Agent 对话
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

        log.info("SQL Agent received: {}", userMessage);
        String answer = toolUseLoop.execute(userMessage, systemPrompt);
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("SQL Agent completed in {}ms", elapsed);

        return AgentChatVO.builder()
                .answer(answer)
                .responseTimeMs(elapsed)
                .toolsUsed(new ArrayList<>())
                .build();
    }
}
