package com.sky.controller.admin;

import com.sky.agent.core.ToolRegistry;
import com.sky.agent.service.BigDataAgentService;
import com.sky.agent.service.SqlAgentService;
import com.sky.dto.AgentChatDTO;
import com.sky.result.Result;
import com.sky.vo.AgentChatVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI Agent 对话接口
 * 支持 SQL Agent (MySQL) 和 BigData Agent (HiveQL/Spark SQL) 两种模式
 */
@RestController
@RequestMapping("/admin/agent")
@Slf4j
@Api(tags = "AI Agent 相关接口")
public class AgentController {

    @Autowired
    private SqlAgentService sqlAgentService;

    @Autowired
    private BigDataAgentService bigDataAgentService;

    @Autowired
    private ToolRegistry toolRegistry;

    /**
     * Agent 对话（支持 SQL 和 BigData 两种模式）
     */
    @PostMapping("/chat")
    @ApiOperation("AI Agent 自然语言对话（支持 sql/bigdata 两种模式）")
    public Result<AgentChatVO> chat(@RequestBody AgentChatDTO dto) {
        log.info("Agent chat request: agentType={}, message={}", dto.getAgentType(), dto.getMessage());

        AgentChatVO result;
        String agentType = dto.getAgentType();

        if ("bigdata".equalsIgnoreCase(agentType)) {
            result = bigDataAgentService.chat(dto.getMessage());
        } else {
            // 默认使用 SQL Agent（兼容未传 agentType 的情况）
            result = sqlAgentService.chat(dto.getMessage());
        }

        return Result.success(result);
    }

    /**
     * 获取可用工具列表
     */
    @GetMapping("/tools")
    @ApiOperation("获取 Agent 可用工具列表")
    public Result<List<String>> getTools() {
        return Result.success(toolRegistry.getToolNames());
    }
}
