package com.sky.agent.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 工具定义——对应 OpenAI/DeepSeek Function Calling 的 tools[].function 格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolDefinition implements Serializable {

    /**
     * 类型固定为 "function"
     */
    private String type;

    /**
     * 函数定义
     */
    private FunctionDef function;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FunctionDef implements Serializable {
        /**
         * 工具名称（与 ToolExecutor 的 name 对应）
         */
        private String name;

        /**
         * 工具描述（给 LLM 看的）
         */
        private String description;

        /**
         * 参数 JSON Schema（顶层为 {"type": "object", "properties": {...}, "required": [...]}）
         */
        private Map<String, Object> parameters;
    }

    /**
     * 快捷构造：创建一个 function 类型的工具定义
     */
    public static ToolDefinition of(String name, String description, Map<String, Object> parameters) {
        return ToolDefinition.builder()
                .type("function")
                .function(FunctionDef.builder()
                        .name(name)
                        .description(description)
                        .parameters(parameters)
                        .build())
                .build();
    }
}
