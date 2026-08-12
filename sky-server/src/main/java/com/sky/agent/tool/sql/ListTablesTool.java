package com.sky.agent.tool.sql;

import com.sky.agent.core.Collections8;
import com.sky.agent.core.ToolHandler;
import com.sky.agent.core.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 列出数据库所有表及行数统计
 */
@Component
@Slf4j
public class ListTablesTool implements ToolHandler {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public String getName() {
        return "list_tables";
    }

    @Override
    public String getDescription() {
        return "列出数据库 sky_take_out 中所有表及其行数统计。使用此工具可以了解数据库中有哪些表可用。";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Collections8.mapOf());
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            String sql = "SELECT TABLE_NAME AS tableName, TABLE_ROWS AS rowCount, " +
                    "ROUND(DATA_LENGTH / 1024, 2) AS dataSizeKB " +
                    "FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_SCHEMA = 'sky_take_out' " +
                    "ORDER BY TABLE_NAME";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            StringBuilder sb = new StringBuilder();
            sb.append("数据库 sky_take_out 中的表：\n\n");
            sb.append(String.format("%-25s %-10s %-12s\n", "表名", "行数", "数据大小(KB)"));
            sb.append(Collections8.repeat("─", 50)).append("\n");
            for (Map<String, Object> row : rows) {
                sb.append(String.format("%-25s %-10s %-12s\n",
                        row.get("tableName"),
                        row.get("rowCount") != null ? row.get("rowCount").toString() : "0",
                        row.get("dataSizeKB") != null ? row.get("dataSizeKB").toString() : "0"));
            }
            return ToolResult.success(null, sb.toString());
        } catch (Exception e) {
            log.error("list_tables failed", e);
            return ToolResult.error(null, "获取表列表失败: " + e.getMessage());
        }
    }
}
