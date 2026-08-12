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
 * Sqoop 数据同步工具 —— 通过 SSH 触发 MySQL → HDFS 的全量或增量同步
 */
@Component
@Slf4j
public class SqoopSyncTool implements ToolHandler {

    @Autowired
    private SSHClient sshClient;

    private static final String MYSQL_HOST = "192.168.68.1";
    private static final String MYSQL_DB = "sky_take_out";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASS = "root";  // VM 中 MySQL 客户端连接使用的密码
    private static final String HDFS_BASE = "/user/hive/warehouse/sky_take_out.db";

    @Override
    public String getName() {
        return "trigger_sqoop_sync";
    }

    @Override
    public String getDescription() {
        return "触发 Sqoop 数据同步——将 MySQL 业务数据库的数据同步到 HDFS/Hive 数据仓库。" +
                "支持全量同步（所有表或指定表）和增量同步（基于时间戳）。" +
                "\n参数说明：" +
                "\n- table: 要同步的表名，不指定则同步全部 11 张表" +
                "\n- mode: 'full'(全量) 或 'incremental'(增量，基于 update_time)，默认 full" +
                "\n⚠️ 注意：全量同步会覆盖 HDFS 上已有数据。" +
                "\n⚠️ 此操作可能需要几分钟时间。";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Collections8.mapOf(
                "table", Collections8.mapOf(
                        "type", "string",
                        "description", "要同步的表名（如 orders, user, dish 等）。不填则同步全部 11 张表。"
                ),
                "mode", Collections8.mapOf(
                        "type", "string",
                        "description", "同步模式: 'full'(全量同步) 或 'incremental'(增量同步)。默认 full。"
                )
        ));
        schema.put("required", Collections8.listOf());
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String table = (String) arguments.getOrDefault("table", "all");
        String mode = (String) arguments.getOrDefault("mode", "full");

        // 全量同步全部表
        String[] allTables = {
                "address_book", "category", "dish", "dish_flavor", "employee",
                "order_detail", "orders", "setmeal", "setmeal_dish", "shopping_cart", "user"
        };

        // 增量同步使用的基础命令
        String baseCmd = "/opt/module/sqoop-1.4.7/bin/sqoop import " +
                "--connect jdbc:mysql://" + MYSQL_HOST + ":3306/" + MYSQL_DB +
                "?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true " +
                "--username " + MYSQL_USER + " --password " + MYSQL_PASS + " " +
                "--fields-terminated-by ',' --null-string '\\\\N' --null-non-string '\\\\N' " +
                "--m 1 --driver com.mysql.cj.jdbc.Driver ";

        StringBuilder result = new StringBuilder();
        result.append("=== Sqoop 数据同步 ===\n");
        result.append("模式: ").append(mode).append("\n");
        result.append("目标表: ").append(table).append("\n\n");

        if (!"all".equals(table)) {
            // 单表同步
            String cmd;
            if ("incremental".equals(mode)) {
                cmd = baseCmd +
                        "--table " + table + " " +
                        "--target-dir " + HDFS_BASE + "/" + table + " " +
                        "--incremental append " +
                        "--check-column update_time " +
                        "--last-value '2026-01-01 00:00:00' " +
                        "--delete-target-dir";
            } else {
                cmd = baseCmd +
                        "--table " + table + " " +
                        "--target-dir " + HDFS_BASE + "/" + table + " " +
                        "--delete-target-dir";
            }
            result.append("执行命令:\n").append(cmd.length() > 200 ? cmd.substring(0, 200) + "..." : cmd).append("\n\n");
            String output = sshClient.execute(cmd);
            result.append(output);
        } else {
            // 全部表同步
            result.append("开始同步全部 ").append(allTables.length).append(" 张表...\n\n");
            for (String tbl : allTables) {
                String cmd = baseCmd +
                        "--table " + tbl + " " +
                        "--target-dir " + HDFS_BASE + "/" + tbl + " " +
                        "--delete-target-dir";
                result.append("--- ").append(tbl).append(" ---\n");
                String output = sshClient.execute(cmd);
                // 截断输出
                if (output.length() > 500) {
                    output = output.substring(0, 500) + "...[已截断]";
                }
                result.append(output).append("\n");
            }
        }

        result.append("\n✅ 同步任务已提交。");
        return ToolResult.success(null, result.toString());
    }
}
