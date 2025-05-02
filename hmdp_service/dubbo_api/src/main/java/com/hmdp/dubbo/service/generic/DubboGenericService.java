package com.hmdp.dubbo.service.generic;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.rpc.service.GenericService;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DubboGenericService {

    private static final Map<String, ReferenceConfig<GenericService>> REFERENCE_CONFIG_MAP = new ConcurrentHashMap<>();
    private static final Map<String, GenericService> GENERIC_SERVICE_MAP = new ConcurrentHashMap<>();
    private static final Interner<String> LOCK_INTERNER = Interners.newWeakInterner();

    @PostConstruct
    public void init() {
        // 添加JVM关闭钩子，确保资源正确释放
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            REFERENCE_CONFIG_MAP.values().forEach(ReferenceConfig::destroy);
        }));
    }

    public GenericService getGenericService(String interfaceName, String version) {
        String key = interfaceName + ":" + version;

        // 快速路径：检查缓存
        GenericService service = GENERIC_SERVICE_MAP.get(key);
        if (service != null) {
            return service;
        }

        // 获取规范化的key作为锁对象
        String lockKey = LOCK_INTERNER.intern(key);

        synchronized (lockKey) {
            // 双重检查
            service = GENERIC_SERVICE_MAP.get(key);
            if (service != null) {
                return service;
            }

            // 创建新的ReferenceConfig
            ReferenceConfig<GenericService> reference = new ReferenceConfig<>();
            reference.setInterface(interfaceName);
            reference.setVersion(version);
            reference.setGeneric(true);

            try {
                // 获取GenericService实例
                service = reference.get();

                // 保存配置和服务实例
                REFERENCE_CONFIG_MAP.put(key, reference);
                GENERIC_SERVICE_MAP.put(key, service);

                return service;
            } catch (Exception e) {
                reference.destroy();
                throw new RuntimeException("Failed to create generic service for " + key, e);
            }
        }
    }

    @PreDestroy
    public void destroy() {
        REFERENCE_CONFIG_MAP.values().forEach(ReferenceConfig::destroy);
        REFERENCE_CONFIG_MAP.clear();
        GENERIC_SERVICE_MAP.clear();
    }

    public Object invoke(String interfaceName, String version, String methodName, String[] parameterTypes, Object[] args) {
        try {
            GenericService genericService = getGenericService(interfaceName, version);
            return genericService.$invoke(methodName, parameterTypes, args);
        } catch (Exception e) {
            throw new RuntimeException("泛化调用失败: " + interfaceName + "." + methodName, e);
        }
    }
}
