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
 * 集群状态检查工具 —— 通过 SSH 检查 Hadoop/Hive/HBase/Spark 服务状态
 */
@Component
@Slf4j
public class ClusterStatusTool implements ToolHandler {

    @Autowired
    private SSHClient sshClient;

    @Override
    public String getName() {
        return "check_cluster_status";
    }

    @Override
    public String getDescription() {
        return "检查大数据集群的运行状态。返回各节点运行的进程列表、HDFS 容量信息。" +
                "使用场景：用户询问集群是否正常、磁盘空间是否充足、或进行问题排查时。" +
                "\n可选参数：" +
                "\n- check: 检查类型 — 'basic'(默认, jps进程+磁盘), " +
                "'hdfs'(HDFS详细报告), 'yarn'(YARN应用列表), 'full'(全部)";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Collections8.mapOf(
                "check", Collections8.mapOf(
                        "type", "string",
                        "description", "检查类型: basic / hdfs / yarn / full。默认 basic。"
                )
        ));
        schema.put("required", Collections8.listOf());
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String checkType = (String) arguments.getOrDefault("check", "basic");

        StringBuilder result = new StringBuilder();
        result.append("=== 苍穹外卖大数据集群状态报告 ===\n\n");

        // Master 进程
        String masterJps = sshClient.execute("jps | sort");
        result.append("--- hadoop-master (192.168.68.100) 进程 ---\n");
        result.append(masterJps).append("\n\n");

        if ("hdfs".equals(checkType) || "full".equals(checkType)) {
            // HDFS 详细报告
            String hdfsReport = sshClient.execute("hdfs dfsadmin -report 2>&1 | head -40");
            result.append("--- HDFS 集群报告 ---\n");
            result.append(hdfsReport).append("\n\n");
        }

        if ("yarn".equals(checkType) || "full".equals(checkType)) {
            // YARN 应用列表
            String yarnApps = sshClient.execute("yarn application -list -appStates RUNNING 2>&1");
            result.append("--- YARN 运行中的应用 ---\n");
            result.append(yarnApps).append("\n\n");
        }

        if ("full".equals(checkType)) {
            // 表行数统计
            String hiveCounts = sshClient.execute(
                    "hive -e 'USE sky_take_out; " +
                            "SELECT \"orders\" AS tbl, COUNT(*) AS cnt FROM orders UNION ALL " +
                            "SELECT \"order_detail\", COUNT(*) FROM order_detail UNION ALL " +
                            "SELECT \"user\", COUNT(*) FROM `user` UNION ALL " +
                            "SELECT \"dish\", COUNT(*) FROM dish;' 2>&1");
            result.append("--- Hive 关键表行数 ---\n");
            result.append(hiveCounts).append("\n\n");
        }

        return ToolResult.success(null, result.toString());
    }
}
