package com.sky.agent.core;

import com.sky.agent.client.DeepSeekClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool Use 循环引擎
 * 核心算法：think → act → observe → 循环
 *
 * <pre>
 * 1. 把 userMessage + systemPrompt + tools → 发送给 DeepSeek
 * 2. 如果 DeepSeek 返回文本 → 结束循环，返回内容
 * 3. 如果 DeepSeek 返回 tool_calls → 执行工具 → 结果追加到消息 → 回到步骤 1
 * 4. 超过 maxIterations → 强制结束
 * </pre>
 */
@Component
@Slf4j
public class ToolUseLoop {

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private DeepSeekConfig config;

    /**
     * 执行 Agent 对话
     *
     * @param userMessage  用户消息
     * @param systemPrompt 系统提示词
     * @return Agent 最终回复
     */
    public String execute(String userMessage, String systemPrompt) {
        List<AgentMessage> messages = new ArrayList<>();
        messages.add(AgentMessage.system(systemPrompt));
        messages.add(AgentMessage.user(userMessage));

        List<ToolDefinition> tools = toolRegistry.getToolDefinitions();

        int maxIter = config.getMaxIterations() != null ? config.getMaxIterations() : 10;
        for (int round = 0; round < maxIter; round++) {
            log.info("=== Agent round {}/{} ===", round + 1, maxIter);

            // 调用 DeepSeek
            DeepSeekResponse response = deepSeekClient.chat(messages, tools);
            AgentMessage choice = response.getFirstMessage();

            if (choice == null) {
                log.warn("Empty response from DeepSeek");
                return "API 返回了空响应，请重试。";
            }

            // 情况1：LLM 请求工具调用（优先检查——v4-pro 可能同时返回 content 和 tool_calls）
            if (choice.isToolCall()) {
                List<ToolCall> toolCalls = choice.getToolCalls();
                log.info("Agent requests {} tool call(s)", toolCalls.size());

                // 添加 assistant 消息（含 tool_calls，但不带 content，避免 LLM 混淆）
                messages.add(AgentMessage.assistantWithToolCalls(toolCalls));

                // 逐个执行工具调用
                for (ToolCall tc : toolCalls) {
                    log.info("  Calling tool: {} id={}", tc.getToolName(), tc.getId());
                    ToolResult result = toolRegistry.execute(tc);

                    // 截断过长的结果
                    String truncatedContent = truncate(result.getContent(), 4000);

                    // 添加 tool 消息
                    messages.add(AgentMessage.toolResult(tc.getId(), truncatedContent));
                }
                // 继续下一轮循环
                continue;
            }

            // 情况2：LLM 返回了纯文本内容（无工具调用）→ 结束循环
            if (choice.getContent() != null && !choice.getContent().isEmpty()) {
                log.info("Agent finished with text response ({} chars)", choice.getContent().length());
                return choice.getContent();
            }

            // 情况3：既无内容也无工具调用（异常）
            log.warn("Agent returned no content and no tool calls, breaking loop");
            return "Agent 未返回有效响应。";
        }

        // 超过最大迭代次数
        log.warn("Agent exceeded max iterations ({})", maxIter);
        return "抱歉，Agent 执行轮次超过了最大限制（" + maxIter + "轮），请尝试简化问题。";
    }

    /**
     * 截断字符串（保留末尾警告信息）
     */
    private String truncate(String content, int maxLen) {
        if (content == null) return "";
        if (content.length() <= maxLen) return content;
        return content.substring(0, maxLen) + "\n...[结果过长，已截断，显示前 " + maxLen + " 字符]";
    }
}
