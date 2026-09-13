package com.opsagent.tool;

import com.opsagent.model.ToolExecutionResult;

import java.util.LinkedHashMap;
import java.util.Map;

public interface Tool {
    String getName();

    String getDescription();

    ToolExecutionResult execute(Map<String, Object> parameters);

    /**
     * 幂等工具失败后可以安全重试。
     *
     * 查询类工具返回 true；restart / scale / rollback / clear_cache 一律 false ——
     * 对一个不幂等的运维动作做自动重试，等于在故障现场反复摘流量、反复改副本数。
     * 正确做法是如实返回 success=false 交给 LLM 判断（有记录、可审计）。
     */
    default boolean isIdempotent() {
        return false;
    }

    /** 高危操作：会改变线上状态，需要审批边界。 */
    default boolean isDangerous() {
        return false;
    }

    /**
     * 统一的失败返回值。
     *
     * data 必须是空 Map 而不是 null：Python 侧对返回体做 {@code data.get("status")}，
     * 是 null 会抛 AttributeError，被 except 吞掉后静默降级成"（模拟数据）"，
     * 让运维人员以为工具跑过了。
     */
    static ToolExecutionResult failure(String message, long startMillis) {
        return new ToolExecutionResult(
                false, message, new LinkedHashMap<String, Object>(),
                System.currentTimeMillis() - startMillis);
    }

    static ToolExecutionResult success(String message, Map<String, Object> data, long startMillis) {
        return new ToolExecutionResult(
                true, message, data == null ? new LinkedHashMap<String, Object>() : data,
                System.currentTimeMillis() - startMillis);
    }
}
