package com.opsagent.tool;

import com.opsagent.model.ToolExecutionResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 高危操作的人工审批闸口。
 *
 * mode=auto（默认）：立即批准，但生成 requestId 并落审计 —— 行为上跟
 * agent 原来那句"模拟批准"等价，区别是这条批准现在有据可查。
 * mode=manual：返回 approved=false，需要前端审批 UI 配合，本轮只保留配置开关。
 *
 * Python 侧读 data.approved / data.mode / data.requestId。
 */
@Component
public class HumanApprovalTool implements Tool {

    @Value("${opsagent.approval.mode:auto}")
    private String mode;

    @Override
    public String getName() {
        return "request_human_approval";
    }

    @Override
    public String getDescription() {
        return "Request human approval before executing a high-risk operation.";
    }

    @Override
    public boolean isIdempotent() {
        return true;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters) {
        long start = System.currentTimeMillis();
        ToolParams p = ToolParams.of(parameters);
        String operation = p.str("operation");
        if (operation == null) {
            return Tool.failure("Missing 'operation' parameter", start);
        }
        String reason = p.strOr("（未说明原因）", "reason");

        boolean auto = "auto".equalsIgnoreCase(mode == null ? "" : mode.trim());
        String requestId = "apr-" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("approved", auto);
        data.put("requestId", requestId);
        data.put("mode", auto ? "auto" : "manual");
        data.put("operation", operation);
        data.put("reason", reason);

        String message = auto
                ? "Auto-approved (mode=auto, requestId=" + requestId + "): " + operation
                : "Pending manual approval (requestId=" + requestId + "): " + operation;

        // 请求本身送达成功即 success=true；批没批由 data.approved 表达。
        // 两者混在一起会让 Python 侧无法区分"审批被拒"和"审批服务挂了"。
        return Tool.success(message, data, start);
    }
}
