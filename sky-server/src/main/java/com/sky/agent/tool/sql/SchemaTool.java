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
 * 获取指定表的结构（字段、类型、注释）和索引信息
 */
@Component
@Slf4j
public class SchemaTool implements ToolHandler {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public String getName() {
        return "get_table_schema";
    }

    @Override
    public String getDescription() {
        return "获取指定表的结构信息，包括所有字段（字段名、类型、是否可空、默认值、注释）和索引。使用此工具了解表的列定义，帮助编写正确的 SQL 查询。";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Collections8.mapOf(
                "table_name", Collections8.mapOf(
                        "type", "string",
                        "description", "要查询的表名，如 orders、dish、category 等"
                )
        ));
        schema.put("required", Collections8.listOf("table_name"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String tableName = (String) arguments.get("table_name");
        if (tableName == null || Collections8.isBlank(tableName)) {
            return ToolResult.error(null, "参数 table_name 不能为空");
        }

        // 安全校验：只允许字母、数字、下划线
        if (!tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            return ToolResult.error(null, "无效的表名: " + tableName);
        }

        try {
            StringBuilder sb = new StringBuilder();

            // 1. 获取字段信息
            String columnSql = "SELECT COLUMN_NAME AS name, COLUMN_TYPE AS type, " +
                    "IS_NULLABLE AS nullable, COLUMN_DEFAULT AS defaultVal, " +
                    "COLUMN_COMMENT AS comment, COLUMN_KEY AS keyType, " +
                    "EXTRA AS extra " +
                    "FROM INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'sky_take_out' AND TABLE_NAME = ? " +
                    "ORDER BY ORDINAL_POSITION";

            List<Map<String, Object>> columns = jdbcTemplate.queryForList(columnSql, tableName);
            if (columns.isEmpty()) {
                return ToolResult.error(null, "表 '" + tableName + "' 不存在或没有列信息");
            }

            sb.append("表: ").append(tableName).append("\n\n");
            sb.append(String.format("%-22s %-22s %-8s %-12s %-10s %s\n",
                    "字段名", "类型", "可空", "默认值", "键", "注释"));
            sb.append(Collections8.repeat("─", 100)).append("\n");

            for (Map<String, Object> col : columns) {
                sb.append(String.format("%-22s %-22s %-8s %-12s %-10s %s\n",
                        col.get("name"),
                        col.get("type"),
                        "YES".equals(col.get("nullable")) ? "YES" : "NO",
                        col.get("defaultVal") != null ? col.get("defaultVal").toString() : "",
                        col.get("keyType") != null ? col.get("keyType").toString() : "",
                        col.get("comment") != null ? col.get("comment").toString() : ""));
            }

            // 2. 获取索引信息
            String indexSql = "SHOW INDEX FROM " + tableName;
            List<Map<String, Object>> indexes = jdbcTemplate.queryForList(indexSql);

            if (!indexes.isEmpty()) {
                sb.append("\n索引:\n");
                sb.append(String.format("%-25s %-12s %-15s\n", "索引名", "唯一性", "列"));
                sb.append(Collections8.repeat("─", 55)).append("\n");
                for (Map<String, Object> idx : indexes) {
                    sb.append(String.format("%-25s %-12s %-15s\n",
                            idx.get("Key_name"),
                            "0".equals(String.valueOf(idx.get("Non_unique"))) ? "UNIQUE" : "NON_UNIQUE",
                            idx.get("Column_name")));
                }
            }

            return ToolResult.success(null, sb.toString());
        } catch (Exception e) {
            log.error("get_table_schema failed for table={}", tableName, e);
            return ToolResult.error(null, "获取表结构失败: " + e.getMessage());
        }
    }
}
