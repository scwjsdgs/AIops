package com.opsagent.tool;

import java.util.Map;

/**
 * 工具参数的宽松读取。
 *
 * 存在的理由：Python agent 侧会同时发 {@code service} 和 {@code deployment} 两个 key
 * （LLM 视野里的概念是"服务"，K8s 里的概念是"deployment"，两边名称不统一），
 * 各工具只要按自己的习惯传其中任意一个都能被读到，不必让两侧强耦合。
 *
 * 同时它也是 LLM 参数不可信的第一道防线：模型可能把数字传成字符串、
 * 传 null、传空串，这里统一收敛成 Java 类型，读不出就返回 null 由工具报错。
 */
public final class ToolParams {

    private final Map<String, Object> raw;

    private ToolParams(Map<String, Object> raw) {
        this.raw = raw == null ? Map.of() : raw;
    }

    public static ToolParams of(Map<String, Object> raw) {
        return new ToolParams(raw);
    }

    /** 按给定顺序返回第一个非空值，全空返回 null。 */
    public String str(String... keys) {
        for (String key : keys) {
            Object v = raw.get(key);
            if (v != null && !v.toString().isBlank()) {
                return v.toString().trim();
            }
        }
        return null;
    }

    public String strOr(String fallback, String... keys) {
        String v = str(keys);
        return v != null ? v : fallback;
    }

    public Integer integer(String... keys) {
        String v = str(keys);
        if (v == null) {
            return null;
        }
        try {
            return Integer.valueOf(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int integerOr(int fallback, String... keys) {
        Integer v = integer(keys);
        return v != null ? v : fallback;
    }
}
