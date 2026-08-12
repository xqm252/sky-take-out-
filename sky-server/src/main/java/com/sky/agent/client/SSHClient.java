package com.sky.agent.client;

import com.jcraft.jsch.*;
import com.sky.agent.core.Collections8;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

/**
 * SSH 客户端 —— 通过 JSch 连接 hadoop-master 执行大数据命令
 *
 * <p>使用方式：注入后调用 {@link #execute(String)} 即可在集群主节点执行命令。</p>
 */
@Component
@Slf4j
public class SSHClient {

    @Value("${sky.bigdata.ssh.host:}")
    private String host;

    @Value("${sky.bigdata.ssh.port:22}")
    private int port;

    @Value("${sky.bigdata.ssh.username:hadoop}")
    private String username;

    @Value("${sky.bigdata.ssh.password:}")
    private String password;

    @Value("${sky.bigdata.ssh.private-key-path:}")
    private String privateKeyPath;

    private static final int CONNECT_TIMEOUT = 15_000;   // SSH 连接超时（ms）
    private static final int COMMAND_TIMEOUT = 120_000;  // 命令执行超时（ms）

    /**
     * 创建 SSH 会话（优先使用私钥认证，其次密码认证）
     */
    private Session createSession() throws JSchException {
        JSch jsch = new JSch();

        // 如果配置了私钥路径，使用私钥认证
        if (privateKeyPath != null && !Collections8.isBlank(privateKeyPath)) {
            java.io.File keyFile = new java.io.File(privateKeyPath);
            if (keyFile.exists()) {
                jsch.addIdentity(privateKeyPath);
                log.info("Using SSH private key: {}", privateKeyPath);
            } else {
                log.warn("SSH private key not found: {}, falling back to password", privateKeyPath);
            }
        }

        Session session = jsch.getSession(username, host, port);

        // 如果有密码，作为 fallback
        if (password != null && !Collections8.isBlank(password)) {
            session.setPassword(password);
        }

        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
        session.setTimeout(CONNECT_TIMEOUT);
        return session;
    }

    /**
     * 在 hadoop-master 上执行命令并返回 stdout + stderr 合并结果
     *
     * <p>命令执行前会自动 source /etc/profile.d/bigdata.sh 以确保 Hadoop/Hive/Spark 命令可用。</p>
     *
     * @param command 要执行的 shell 命令
     * @return 命令输出
     * @throws RuntimeException SSH 连接失败或命令执行异常
     */
    public String execute(String command) {
        if (host == null || Collections8.isBlank(host)) {
            return "[ERROR] SSH 主机未配置。请在 application.yml 中设置 sky.bigdata.ssh.host，" +
                    "或通过环境变量 BIGDATA_SSH_HOST 指定。";
        }

        // 自动注入 bigdata 环境变量
        String wrappedCommand = "source /etc/profile.d/bigdata.sh 2>/dev/null; " + command;

        log.info("SSH {}:{} executing command ({} chars)", host, port,
                wrappedCommand.length() > 120 ? wrappedCommand.substring(0, 120) + "..." : wrappedCommand);

        try {
            Session session = createSession();
            session.connect();

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(wrappedCommand);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            channel.setOutputStream(out);
            channel.setErrStream(err);

            channel.connect(COMMAND_TIMEOUT);

            // 等待命令执行完毕
            while (!channel.isClosed()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            int exitCode = channel.getExitStatus();
            channel.disconnect();
            session.disconnect();

            StringBuilder result = new StringBuilder();
            String stdout = out.toString("UTF-8");
            String stderr = err.toString("UTF-8");

            if (stdout != null && stdout.length() > 0) {
                result.append(stdout);
            }
            if (stderr != null && stderr.length() > 0) {
                if (result.length() > 0) result.append("\n");
                result.append("[STDERR] ").append(stderr.trim());
            }
            if (result.length() == 0) {
                result.append("(命令执行完成，无输出) exitCode=").append(exitCode);
            }

            log.info("SSH command completed: exitCode={}, outputLen={}", exitCode, result.length());
            return result.toString();

        } catch (JSchException e) {
            log.error("SSH connection failed: {}@{}:{} — {}", username, host, port, e.getMessage());
            return "[ERROR] SSH 连接失败: " + e.getMessage() +
                    "\n请检查虚拟机是否开机、SSH 服务是否正常、网络是否可达。";
        } catch (Exception e) {
            log.error("SSH execution error", e);
            return "[ERROR] SSH 执行异常: " + e.getMessage();
        }
    }

    /**
     * 测试 SSH 连通性
     *
     * @return true 表示可以正常连接并执行命令
     */
    public boolean testConnection() {
        String result = execute("echo 'SSH_OK'");
        return result.contains("SSH_OK");
    }
}
