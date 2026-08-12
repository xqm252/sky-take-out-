package com.sky.agent.tool.bigdata;

import com.sky.agent.client.SSHClient;
import com.sky.agent.core.Collections8;
import com.sky.agent.core.ToolHandler;
import com.sky.agent.core.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spark SQL 执行工具 —— 通过 SSH 在 hadoop-master 上执行 Spark SQL 查询
 *
 * <p>适合需要高性能计算的复杂聚合查询，Spark 利用内存计算比 Hive MapReduce 快 10-100 倍。</p>
 */
@Component
@Slf4j
public class SparkSQLTool implements ToolHandler {

    @Autowired
    private SSHClient sshClient;

    private static final int MAX_RESULT_ROWS = 500;

    @Override
    public String getName() {
        return "execute_sparksql";
    }

    @Override
    public String getDescription() {
        return "在 Spark SQL 引擎上执行查询（只读）。比 HiveQL 更快，适合复杂聚合和大数据集分析。" +
                "同样查询 HDFS 上的 sky_take_out 数据仓库。" +
                "结果最多返回 " + MAX_RESULT_ROWS + " 行。" +
                "\n⚠️ 注意：" +
                "\n- Spark SQL 语法与 HiveQL 基本兼容" +
                "\n- 仍不允许 DROP / INSERT / OVERWRITE 等写操作" +
                "\n- 使用 `spark-sql` 命令（YARN 模式）" +
                "\n- 执行完成后会打印 Spark 任务耗时等信息，可以忽略";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Collections8.mapOf(
                "sql", Collections8.mapOf(
                        "type", "string",
                        "description", "要执行的 Spark SQL 查询语句。只允许 SELECT / SHOW / DESCRIBE / EXPLAIN。" +
                                "务必添加 LIMIT 子句（最大 " + MAX_RESULT_ROWS + "）。"
                )
        ));
        schema.put("required", Collections8.listOf("sql"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String sql = (String) arguments.get("sql");
        if (sql == null || Collections8.isBlank(sql)) {
            return ToolResult.error(null, "Spark SQL 查询不能为空");
        }

        // === 安全校验 ===
        String trimmedUpper = sql.trim().toUpperCase();
        String[] allowed = {"SELECT", "SHOW", "DESCRIBE", "EXPLAIN", "WITH", "USE"};
        boolean allowedPrefix = false;
        for (String prefix : allowed) {
            if (trimmedUpper.startsWith(prefix)) {
                allowedPrefix = true;
                break;
            }
        }
        if (!allowedPrefix) {
            return ToolResult.error(null, "安全拦截：只允许 SELECT / SHOW / DESCRIBE / EXPLAIN / WITH / USE 语句。");
        }

        String[] dangerous = {"DROP", "DELETE", "INSERT", "UPDATE", "ALTER", "TRUNCATE", "CREATE", "LOAD", "OVERWRITE"};
        for (String keyword : dangerous) {
            if (trimmedUpper.contains(keyword)) {
                return ToolResult.error(null, "安全拦截：Spark SQL 中不允许包含 " + keyword + " 操作。");
            }
        }

        String escapedSql = sql.replace("'", "'\\''");
        String command = "spark-sql --master yarn -e 'USE sky_take_out; " + escapedSql + "'";

        try {
            log.info("Executing Spark SQL (length={}): {}", sql.length(),
                    sql.length() > 200 ? sql.substring(0, 200) + "..." : sql);

            String output = sshClient.execute(command);

            if (output != null && output.length() > 8000) {
                output = output.substring(0, 8000) +
                        "\n\n[...输出过长已截断。请增加 LIMIT 缩小结果集。]";
            }

            if (output != null && output.startsWith("[ERROR]")) {
                return ToolResult.error(null, output);
            }

            return ToolResult.success(null, output);
        } catch (Exception e) {
            log.error("Spark SQL execution failed", e);
            return ToolResult.error(null, "Spark SQL 执行异常: " + e.getMessage());
        }
    }
}
