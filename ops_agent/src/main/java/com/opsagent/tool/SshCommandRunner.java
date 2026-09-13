package com.opsagent.tool;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 一次性的 SSH 命令执行通道。
 *
 * 抽出来是因为 query_log 和 clear_cache(ssh 模式) 都要用，
 * 而原来内联在 QueryLogTool 里的写法有两个坑：
 * 1. stderr 被接到 System.err，没人抽干它 —— stderr 写满管道缓冲区时远端进程会挂住，
 *    本地 readLine() 永远等不到 EOF；
 * 2. 没有任何读超时，一条卡住的命令能永久占住调用线程。
 */
@Component
public class SshCommandRunner {

    @Value("${opsagent.ssh.host:localhost}")
    private String host;
    @Value("${opsagent.ssh.port:22}")
    private int port;
    @Value("${opsagent.ssh.username:root}")
    private String username;
    @Value("${opsagent.ssh.password:}")
    private String password;

    public record SshResult(int exitCode, String stdout, String stderr, boolean timedOut) {
        public boolean ok() {
            return !timedOut && exitCode == 0;
        }
    }

    public SshResult run(String command, int connectTimeoutMs, int readTimeoutMs) {
        Session session = null;
        ChannelExec channel = null;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            if (password != null && !password.isEmpty()) {
                session.setPassword(password);
            }
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(connectTimeoutMs);

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();
            channel.connect(connectTimeoutMs);

            byte[] buf = new byte[4096];
            long deadline = System.currentTimeMillis() + readTimeoutMs;
            while (System.currentTimeMillis() < deadline) {
                // 两个流都要抽干，且每轮都抽，避免一边写满堵死
                boolean readSomething = drain(in, stdout, buf);
                readSomething |= drain(err, stderr, buf);
                if (channel.isClosed() && in.available() == 0 && err.available() == 0) {
                    break;
                }
                if (!readSomething) {
                    Thread.sleep(50);
                }
            }

            if (!channel.isClosed()) {
                return new SshResult(-1, text(stdout), text(stderr), true);
            }
            // exitStatus 只有在 channel 关闭后才是最终值，读流期间取到的是 -1
            return new SshResult(channel.getExitStatus(), text(stdout), text(stderr), false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new SshResult(-1, text(stdout), text(stderr), true);
        } catch (Exception e) {
            return new SshResult(-1, text(stdout), e.getMessage() == null ? "SSH error" : e.getMessage(), false);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private boolean drain(InputStream in, ByteArrayOutputStream out, byte[] buf) throws Exception {
        int available = in.available();
        if (available <= 0) {
            return false;
        }
        int read = in.read(buf, 0, Math.min(available, buf.length));
        if (read > 0) {
            out.write(buf, 0, read);
            return true;
        }
        return false;
    }

    private String text(ByteArrayOutputStream stream) {
        return stream.toString(StandardCharsets.UTF_8);
    }
}
