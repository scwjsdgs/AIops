package com.opsagent.tool;


import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class KubernetesToolBase {
    protected KubernetesClient client;
    protected String namespace;

    @Value("${opsagent.kubernetes.namespace:default}")
    private String namespaceConfig;

    @PostConstruct
    public void init() {
        // 必须以 Config.autoConfigure(null) 为基底再改超时。
        // 直接 new ConfigBuilder() 会得到一份默认配置，把 kubeconfig 里读到的
        // apiserver 地址、证书、token 全部丢掉，集群一接就认证失败。
        Config base = Config.autoConfigure(null);
        Config config = new ConfigBuilder(base)
                // 没有集群时这里不会抛异常（fabric8 会回落到 kubernetes.default.svc），
                // 只在真正调用时失败，由各工具 catch 成 success=false
                .withConnectionTimeout(5_000)
                .withRequestTimeout(15_000)
                .build();
        this.client = new KubernetesClientBuilder().withConfig(config).build();
        this.namespace = namespaceConfig;
    }
}
