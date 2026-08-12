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
 * HiveQL 执行工具 —— 通过 SSH 在 hadoop-master 上执行 HiveQL 只读查询
 *
 * <p>使用 hive 命令行（嵌入式模式，不需要 HiveServer2），对 HDFS 上的 sky_take_out 数据仓库执行分析查询。</p>
 */
@Component
@Slf4j
public class HiveQLTool implements ToolHandler {

    @Autowired
    private SSHClient sshClient;

    private static final int MAX_RESULT_ROWS = 200;
    private static final int COMMAND_TIMEOUT_MS = 120_000;

    @Override
    public String getName() {
        return "execute_hiveql";
    }

    @Override
    public String getDescription() {
        return "在 Hadoop 数据仓库上执行 HiveQL 查询（只读）。使用此工具对大数据平台上的 sky_take_out 数据仓库进行分析。" +
                "查询的是 Hive 外表，数据存储在 HDFS 上，与 MySQL 业务库完全一致。" +
                "结果最多返回 " + MAX_RESULT_ROWS + " 行。" +
                "典型场景：月度/季度营业额统计、用户增长趋势、订单完成率分析、Top-N 菜品排行。" +
                "\n⚠️ HiveQL 注意事项：" +
                "\n- 不能使用 MySQL 的 INTERVAL 语法，使用 add_months() 或 date_sub() 代替" +
                "\n- 日期字段是 STRING 类型，需要用 SUBSTR() 截取比较" +
                "\n- 只允许 SELECT / SHOW / DESCRIBE / EXPLAIN，禁止 DROP / ALTER / INSERT 等写操作" +
                "\n- 表名 `user` 是 Hive 保留字，需要使用反引号 \\`user\\`";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Collections8.mapOf(
                "hiveql", Collections8.mapOf(
                        "type", "string",
                        "description", "要执行的 HiveQL 查询语句。只能使用 SELECT / SHOW / DESCRIBE / EXPLAIN。" +
                                "务必在查询末尾添加 LIMIT 子句（最大 " + MAX_RESULT_ROWS + "）。" +
                                "日期字段为 STRING 格式 'YYYY-MM-DD HH:MM:SS.s'。"
                )
        ));
        schema.put("required", Collections8.listOf("hiveql"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String hiveql = (String) arguments.get("hiveql");
        if (hiveql == null || Collections8.isBlank(hiveql)) {
            return ToolResult.error(null, "HiveQL 查询不能为空");
        }

        // === 安全校验 ===
        String trimmedUpper = hiveql.trim().toUpperCase();
        String[] allowed = {"SELECT", "SHOW", "DESCRIBE", "EXPLAIN", "WITH", "USE"};
        boolean allowedPrefix = false;
        for (String prefix : allowed) {
            if (trimmedUpper.startsWith(prefix)) {
                allowedPrefix = true;
                break;
            }
        }
        if (!allowedPrefix) {
            return ToolResult.error(null, "安全拦截：只允许 SELECT / SHOW / DESCRIBE / EXPLAIN / WITH / USE 语句。" +
                    "\n禁止的 SQL: " + hiveql.substring(0, Math.min(80, hiveql.length())));
        }

        // 禁止危险操作
        String[] dangerous = {"DROP", "DELETE", "INSERT", "UPDATE", "ALTER", "TRUNCATE", "CREATE", "LOAD", "OVERWRITE"};
        for (String keyword : dangerous) {
            if (trimmedUpper.contains(keyword)) {
                return ToolResult.error(null, "安全拦截：HiveQL 中不允许包含 " + keyword + " 操作。" +
                        "\n只允许只读查询。");
            }
        }

        // 构建 hive 命令（使用嵌入式模式，连接指定数据库）
        String escapedSql = hiveql.replace("'", "'\\''");
        String command = "hive --hiveconf hive.cli.print.header=true -e 'USE sky_take_out; " + escapedSql + "'";

        try {
            log.info("Executing HiveQL (length={}): {}", hiveql.length(),
                    hiveql.length() > 200 ? hiveql.substring(0, 200) + "..." : hiveql);

            String output = sshClient.execute(command);

            // 截断过长结果
            if (output != null && output.length() > 6000) {
                output = output.substring(0, 6000) +
                        "\n\n[...输出过长已截断，显示前 6000 字符。请增加 LIMIT 缩小结果集。]";
            }

            if (output != null && output.startsWith("[ERROR]")) {
                return ToolResult.error(null, output);
            }

            return ToolResult.success(null, output);
        } catch (Exception e) {
            log.error("HiveQL execution failed", e);
            return ToolResult.error(null, "HiveQL 执行异常: " + e.getMessage());
        }
    }
}
