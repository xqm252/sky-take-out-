package com.sky.agent.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 工具调用——LLM 返回的 tool_calls[] 条目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolCall implements Serializable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 工具调用唯一 ID（LLM 分配）
     */
    private String id;

    /**
     * 类型固定为 "function"
     */
    private String type;

    /**
     * 函数调用详情
     */
    private FunctionCall function;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FunctionCall implements Serializable {
        /**
         * 工具名称
         */
        private String name;

        /**
         * 工具参数 JSON 字符串
         */
        private String arguments;
    }

    /**
     * 获取工具名称
     */
    @JsonIgnore
    public String getToolName() {
        return function != null ? function.getName() : null;
    }

    /**
     * 将 arguments JSON 字符串解析为 Map
     */
    @JsonIgnore
    public Map<String, Object> getArgumentsMap() {
        try {
            if (function == null || function.getArguments() == null) {
                return Collections8.mapOf();
            }
            return MAPPER.readValue(function.getArguments(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections8.mapOf();
        }
    }

    /**
     * 从 arguments Map 中获取字符串参数
     */
    @JsonIgnore
    public String getStringArg(String key) {
        Object val = getArgumentsMap().get(key);
        return val != null ? val.toString() : null;
    }

    /**
     * 从 arguments Map 中获取整数参数
     */
    @JsonIgnore
    public Integer getIntArg(String key) {
        Object val = getArgumentsMap().get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return null;
    }
}
