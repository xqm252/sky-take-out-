package com.sky.agent.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sky.agent.core.Collections8;
import com.sky.agent.core.AgentMessage;
import com.sky.agent.core.DeepSeekConfig;
import com.sky.agent.core.DeepSeekResponse;
import com.sky.agent.core.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek API HTTP 客户端
 * 兼容 OpenAI Chat Completions 格式
 */
@Component
@Slf4j
public class DeepSeekClient {

    @Autowired
    private DeepSeekConfig config;

    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    public DeepSeekClient() {
        this.mapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 发送 Chat Completions 请求
     *
     * @param messages 对话消息列表
     * @param tools    可用工具列表
     * @return 响应（包含文本回复或工具调用请求）
     */
    public DeepSeekResponse chat(List<AgentMessage> messages, List<ToolDefinition> tools) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getModel());
            body.put("messages", messages);
            body.put("max_tokens", config.getMaxTokens());
            body.put("temperature", config.getTemperature());

            if (tools != null && !tools.isEmpty()) {
                body.put("tools", tools);
                body.put("tool_choice", "auto");
            }

            String jsonBody = mapper.writeValueAsString(body);
            log.debug("DeepSeek API request: {}", jsonBody);

            Request request = new Request.Builder()
                    .url(config.getBaseUrl() + "/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    log.error("DeepSeek API error: HTTP {} body={}", response.code(), responseBody);
                    throw new IOException("DeepSeek API returned " + response.code() + ": " + responseBody);
                }
                DeepSeekResponse dsr = mapper.readValue(responseBody, DeepSeekResponse.class);
                log.debug("DeepSeek API response: hasToolCalls={}, finishReason={}",
                        dsr.hasToolCalls(),
                        dsr.getChoices() != null && !dsr.getChoices().isEmpty()
                                ? dsr.getChoices().get(0).getFinishReason() : "N/A");
                return dsr;
            }
        } catch (IOException e) {
            log.error("DeepSeek API call failed", e);
            // 返回模拟错误响应
            DeepSeekResponse errResp = new DeepSeekResponse();
            DeepSeekResponse.Choice choice = new DeepSeekResponse.Choice();
            choice.setMessage(AgentMessage.assistant("抱歉，DeepSeek API 调用失败: " + e.getMessage()));
            choice.setFinishReason("stop");
            errResp.setChoices(Collections8.listOf(choice));
            return errResp;
        }
    }
}
