package com.sky.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Agent 对话响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "Agent 对话响应")
public class AgentChatVO implements Serializable {

    @ApiModelProperty("Agent 回复内容（支持 Markdown 格式）")
    private String answer;

    @ApiModelProperty("处理耗时（毫秒）")
    private Long responseTimeMs;

    @ApiModelProperty("使用的工具调用列表（调试用）")
    private List<String> toolsUsed;
}
