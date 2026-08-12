package com.sky.agent.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * DeepSeek / OpenAI Chat Completions API 响应模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeepSeekResponse implements Serializable {

    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Choice implements Serializable {
        private Integer index;
        private AgentMessage message;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Usage implements Serializable {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    /**
     * 获取第一个 choice 的消息
     */
    @JsonIgnore
    public AgentMessage getFirstMessage() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).getMessage();
        }
        return null;
    }

    /**
     * 是否有工具调用
     */
    @JsonIgnore
    public boolean hasToolCalls() {
        AgentMessage msg = getFirstMessage();
        return msg != null && msg.isToolCall();
    }

    /**
     * 获取工具调用列表
     */
    @JsonIgnore
    public List<ToolCall> getToolCalls() {
        AgentMessage msg = getFirstMessage();
        return msg != null ? msg.getToolCalls() : null;
    }

    /**
     * 获取文本内容
     */
    @JsonIgnore
    public String getContent() {
        AgentMessage msg = getFirstMessage();
        return msg != null ? msg.getContent() : null;
    }
}
