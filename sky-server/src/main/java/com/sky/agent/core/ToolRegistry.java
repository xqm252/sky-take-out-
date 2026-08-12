package com.sky.agent.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册中心
 * 自动发现所有实现了 ToolHandler 接口的 Spring Bean，统一管理工具注册和调用
 */
@Component
@Slf4j
public class ToolRegistry {

    /**
     * Spring 自动注入所有 ToolHandler 实现
     */
    @Autowired(required = false)
    private List<ToolHandler> toolHandlers = new ArrayList<>();

    /**
     * 工具注册表：name -> ToolHandler
     */
    private final Map<String, ToolHandler> registry = new ConcurrentHashMap<>();

    /**
     * 工具定义缓存：避免每次请求都重新生成 ToolDefinition 列表
     */
    private List<ToolDefinition> definitions = new ArrayList<>();

    @PostConstruct
    public void init() {
        for (ToolHandler handler : toolHandlers) {
            register(handler);
        }
        log.info("ToolRegistry initialized: {} tools registered", registry.size());
        for (String name : registry.keySet()) {
            log.info("  - {}", name);
        }
    }

    /**
     * 手动注册一个工具
     */
    public void register(ToolHandler handler) {
        String name = handler.getName();
        registry.put(name, handler);
        definitions.add(handler.toDefinition());
        log.debug("Registered tool: {}", name);
    }

    /**
     * 获取工具定义列表（用于发送给 LLM）
     */
    public List<ToolDefinition> getToolDefinitions() {
        return definitions;
    }

    /**
     * 根据工具名称获取执行器
     */
    public ToolHandler getHandler(String name) {
        return registry.get(name);
    }

    /**
     * 执行一个工具调用
     */
    public ToolResult execute(ToolCall call) {
        String name = call.getToolName();
        ToolHandler handler = registry.get(name);
        if (handler == null) {
            return ToolResult.error(call.getId(), "Unknown tool: " + name);
        }
        try {
            Map<String, Object> args = call.getArgumentsMap();
            log.info("Executing tool: {} with args={}", name, args);
            ToolResult result = handler.execute(args);
            result.setToolCallId(call.getId());
            log.info("Tool {} result: success={}, content length={}",
                    name, result.isSuccess(),
                    result.getContent() != null ? result.getContent().length() : 0);
            return result;
        } catch (Exception e) {
            log.error("Tool {} execution failed", name, e);
            return ToolResult.error(call.getId(), "Tool execution error: " + e.getMessage());
        }
    }

    /**
     * 获取已注册的工具名称列表
     */
    public List<String> getToolNames() {
        return new ArrayList<>(registry.keySet());
    }
}
