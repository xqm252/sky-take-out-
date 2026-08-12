package com.sky.agent.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Agent 对话消息模型，兼容 DeepSeek / OpenAI Chat Completions 格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentMessage implements Serializable {

    /**
     * 角色类型: system / user / assistant / tool
     */
    private String role;

    /**
     * 消息内容（system/user/assistant/tool 都有）
     */
    private String content;

    /**
     * 工具调用列表（仅 assistant 角色，有 tool_calls 时 content 为 null）
     */
    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;

    /**
     * 工具调用 ID（仅 tool 角色，用于关联 assistant 发起的 tool_call）
     */
    @JsonProperty("tool_call_id")
    private String toolCallId;

    /**
     * 工具名称（仅 tool 角色可选）
     */
    private String name;

    // ===== 工厂方法 =====

    public static AgentMessage system(String content) {
        return AgentMessage.builder().role("system").content(content).build();
    }

    public static AgentMessage user(String content) {
        return AgentMessage.builder().role("user").content(content).build();
    }

    public static AgentMessage assistant(String content) {
        return AgentMessage.builder().role("assistant").content(content).build();
    }

    public static AgentMessage assistantWithToolCalls(List<ToolCall> toolCalls) {
        return AgentMessage.builder()
                .role("assistant")
                .content(null)
                .toolCalls(toolCalls)
                .build();
    }

    public static AgentMessage toolResult(String toolCallId, String content) {
        return AgentMessage.builder()
                .role("tool")
                .toolCallId(toolCallId)
                .content(content)
                .build();
    }

    public boolean isToolCall() {
        return "assistant".equals(role) && toolCalls != null && !toolCalls.isEmpty();
    }

    public boolean isToolResult() {
        return "tool".equals(role);
    }
}
