package com.sky.agent.core;

import java.util.Map;

/**
 * 工具执行器接口——每个工具 Bean 都要实现此接口
 * Spring 会自动发现所有实现并注册到 ToolRegistry
 */
public interface ToolHandler {

    /**
     * 工具名称（与 ToolDefinition.function.name 一致）
     */
    String getName();

    /**
     * 工具描述（给 LLM 看的）
     */
    String getDescription();

    /**
     * 参数 JSON Schema Map
     * 格式：{"type": "object", "properties": {...}, "required": [...]}
     */
    Map<String, Object> getParametersSchema();

    /**
     * 执行工具调用
     *
     * @param arguments 工具参数（已解析为 Map）
     * @return 执行结果
     */
    ToolResult execute(Map<String, Object> arguments);

    /**
     * 生成 ToolDefinition（供 ToolUseLoop 注册到 LLM）
     */
    default ToolDefinition toDefinition() {
        return ToolDefinition.of(getName(), getDescription(), getParametersSchema());
    }
}
