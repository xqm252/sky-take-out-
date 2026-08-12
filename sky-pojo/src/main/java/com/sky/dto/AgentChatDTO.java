package com.sky.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * Agent 对话请求 DTO
 */
@Data
@ApiModel(description = "Agent 对话请求")
public class AgentChatDTO implements Serializable {

    @ApiModelProperty(value = "用户消息", required = true, example = "查询昨天销量最高的5个菜品")
    private String message;

    @ApiModelProperty(value = "Agent 类型: sql / bigdata", allowableValues = "sql,bigdata", example = "sql")
    private String agentType;
}
