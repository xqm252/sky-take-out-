package com.sky.agent.core;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 工具执行结果
 */
@Data
@Builder
public class ToolResult implements Serializable {
    private String toolCallId;
    private boolean success;
    private String content;

    public static ToolResult success(String toolCallId, String content) {
        return ToolResult.builder()
                .toolCallId(toolCallId)
                .success(true)
                .content(content)
                .build();
    }

    public static ToolResult error(String toolCallId, String errorMessage) {
        return ToolResult.builder()
                .toolCallId(toolCallId)
                .success(false)
                .content("ERROR: " + errorMessage)
                .build();
    }
}
