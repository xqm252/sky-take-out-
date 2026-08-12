package com.sky.agent.tool.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.regex.Pattern;

/**
 * 执行 MySQL SELECT 查询（只读，含 SQL 注入防护）
 */
@Component
@Slf4j
public class QueryTool implements ToolHandler {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * DML/DDL 危险关键词黑名单
     */
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
            "\\b(DROP|DELETE|INSERT|UPDATE|ALTER|CREATE|TRUNCATE|GRANT|REVOKE|EXEC|EXECUTE|REPLACE|LOAD|RENAME|CALL)\\b",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 多语句注入检测
     */
    private static final Pattern MULTI_STMT = Pattern.compile(";.*\\s*(SELECT|DROP|DELETE|INSERT)", Pattern.CASE_INSENSITIVE);

    /**
     * 注释注入检测
     */
    private static final Pattern COMMENT_INJECTION = Pattern.compile("(--|/\\*|\\*/)");

    private static final int MAX_RESULT_ROWS = 200;

    @Override
    public String getName() {
        return "execute_sql";
    }

    @Override
    public String getDescription() {
        return "执行 MySQL SELECT 查询（只读）。使用此工具查询 sky_take_out 数据库中的数据。" +
                "必须使用完整的 SELECT 语句。结果最多返回 " + MAX_RESULT_ROWS + " 行。" +
                "常见查询示例：\n" +
                "- 昨日销量 Top10: SELECT od.name, SUM(od.number) AS total FROM order_detail od " +
                "JOIN orders o ON od.order_id = o.id WHERE o.status = 5 AND DATE(o.order_time) = CURDATE() - INTERVAL 1 DAY " +
                "GROUP BY od.name ORDER BY total DESC LIMIT 10";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Collections8.mapOf(
                "sql", Collections8.mapOf(
                        "type", "string",
                        "description", "要执行的 MySQL SELECT 查询语句。只能使用 SELECT(SHOW/DESCRIBE/EXPLAIN)。" +
                                "务必在查询末尾添加 LIMIT 子句。"
                )
        ));
        schema.put("required", Collections8.listOf("sql"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String sql = (String) arguments.get("sql");
        if (sql == null || Collections8.isBlank(sql)) {
            return ToolResult.error(null, "SQL 查询不能为空");
        }

        // === 安全校验 ===

        // 1. 检测危险关键词
        if (DANGEROUS_PATTERN.matcher(sql).find()) {
            log.warn("SQL injection blocked: dangerous keyword detected in: {}", sql);
            return ToolResult.error(null, "SQL 安全拦截：只允许执行 SELECT 查询，不允许 DML/DDL 操作。");
        }

        // 2. 检测多语句注入
        if (MULTI_STMT.matcher(sql).find()) {
            log.warn("SQL injection blocked: multi-statement detected in: {}", sql);
            return ToolResult.error(null, "SQL 安全拦截：不允许执行多条语句。");
        }

        // 3. 检测注释注入
        if (COMMENT_INJECTION.matcher(sql).find()) {
            log.warn("SQL injection blocked: comment injection detected in: {}", sql);
            return ToolResult.error(null, "SQL 安全拦截：SQL 中不允许包含注释符号。");
        }

        // 4. 必须以 SELECT/SHOW/DESCRIBE/EXPLAIN/WITH 开头
        String trimmed = sql.trim().toUpperCase();
        if (!trimmed.startsWith("SELECT") && !trimmed.startsWith("SHOW")
                && !trimmed.startsWith("DESCRIBE") && !trimmed.startsWith("EXPLAIN")
                && !trimmed.startsWith("WITH")) {
            return ToolResult.error(null, "SQL 安全拦截：只允许 SELECT / SHOW / DESCRIBE / EXPLAIN 语句。");
        }

        try {
            log.info("Executing SQL query (length={}): {}", sql.length(),
                    sql.length() > 200 ? sql.substring(0, 200) + "..." : sql);

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

            if (rows.size() > MAX_RESULT_ROWS) {
                rows = rows.subList(0, MAX_RESULT_ROWS);
                String json = mapper.writeValueAsString(rows);
                return ToolResult.success(null,
                        json + "\n\n⚠️ 结果超过 " + MAX_RESULT_ROWS + " 行，已截断。共返回前 " + MAX_RESULT_ROWS + " 行。");
            }

            String json = mapper.writeValueAsString(rows);
            return ToolResult.success(null,
                    "查询返回 " + rows.size() + " 行结果：\n" + json);
        } catch (Exception e) {
            log.error("SQL execution failed: {}", e.getMessage());
            return ToolResult.error(null, "SQL 执行错误: " + e.getMessage() +
                    "\n请检查 SQL 语法后重试。提示：表名和字段名不要加多余的引号。");
        }
    }
}
