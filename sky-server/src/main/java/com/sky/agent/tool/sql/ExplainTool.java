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
 * 执行 MySQL EXPLAIN，分析 SQL 执行计划并给出优化建议
 */
@Component
@Slf4j
public class ExplainTool implements ToolHandler {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public String getName() {
        return "explain_sql";
    }

    @Override
    public String getDescription() {
        return "对 SQL 查询执行 EXPLAIN 分析，获取执行计划。使用此工具可以发现查询是否使用了索引、是否存在全表扫描、JOIN 顺序是否合理等问题。" +
                "根据执行计划给出具体的优化建议（如添加索引、改写 SQL 等）。";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Collections8.mapOf(
                "sql", Collections8.mapOf(
                        "type", "string",
                        "description", "要分析的 SELECT 查询语句"
                )
        ));
        schema.put("required", Collections8.listOf("sql"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String sql = (String) arguments.get("sql");
        if (sql == null || Collections8.isBlank(sql)) {
            return ToolResult.error(null, "SQL 不能为空");
        }

        // 简单安全校验
        String trimmed = sql.trim().toUpperCase();
        if (!trimmed.startsWith("SELECT") && !trimmed.startsWith("WITH")) {
            return ToolResult.error(null, "只支持对 SELECT 语句执行 EXPLAIN");
        }

        try {
            String explainSql = "EXPLAIN " + sql;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(explainSql);

            StringBuilder sb = new StringBuilder();
            sb.append("=== EXPLAIN 执行计划分析 ===\n\n");

            // 输出原始执行计划
            sb.append("执行计划:\n");
            for (Map<String, Object> row : rows) {
                sb.append(String.format("  table: %s | type: %s | key: %s | rows: %s | filtered: %s%% | Extra: %s\n",
                        row.get("table"),
                        row.get("type"),
                        row.get("key") != null ? row.get("key") : "NULL",
                        row.get("rows"),
                        row.get("filtered"),
                        row.get("Extra") != null ? row.get("Extra") : ""));
            }

            // 分析并给出优化建议
            sb.append("\n优化分析:\n");

            boolean hasIssue = false;
            for (Map<String, Object> row : rows) {
                String type = (String) row.get("type");
                String key = (String) row.get("key");
                String extra = (String) row.get("Extra");

                // type=ALL => 全表扫描
                if ("ALL".equals(type)) {
                    hasIssue = true;
                    sb.append("  ⚠️ 表 ").append(row.get("table"))
                            .append(" 正在执行全表扫描 (type=ALL)，扫描了约 ")
                            .append(row.get("rows")).append(" 行。\n");
                    sb.append("     建议：在 WHERE 条件中的列上添加索引，或检查现有索引是否可用。\n");
                }

                // key=NULL 但不是全表扫描
                if (key == null && !"ALL".equals(type)) {
                    sb.append("  ⚠️ 表 ").append(row.get("table"))
                            .append(" 未使用索引 (key=NULL)\n");
                    sb.append("     建议：检查 WHERE/JOIN 条件中的列是否有合适的索引。\n");
                }

                // Using filesort
                if (extra != null && extra.contains("Using filesort")) {
                    hasIssue = true;
                    sb.append("  ⚠️ 使用了文件排序 (Using filesort)\n");
                    sb.append("     建议：为 ORDER BY 的列添加索引，避免文件排序操作。\n");
                }

                // Using temporary
                if (extra != null && extra.contains("Using temporary")) {
                    hasIssue = true;
                    sb.append("  ⚠️ 使用了临时表 (Using temporary)\n");
                    sb.append("     建议：为 GROUP BY / DISTINCT 列添加索引，或优化查询逻辑。\n");
                }
            }

            if (!hasIssue) {
                sb.append("  ✅ 执行计划看起来不错，没有明显的性能问题。\n");
            }

            sb.append("\n字段说明:\n");
            sb.append("  type: 连接类型 — system > const > eq_ref > ref > range > index > ALL\n");
            sb.append("  key: 实际使用的索引（NULL 表示未使用索引）\n");
            sb.append("  rows: 预计扫描的行数\n");
            sb.append("  Extra: 额外信息（Using index=覆盖索引, Using filesort=文件排序, Using temporary=临时表）\n");

            return ToolResult.success(null, sb.toString());
        } catch (Exception e) {
            log.error("EXPLAIN failed", e);
            return ToolResult.error(null, "EXPLAIN 执行失败: " + e.getMessage());
        }
    }
}
