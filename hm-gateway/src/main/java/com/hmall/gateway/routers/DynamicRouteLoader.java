package com.hmall.gateway.routers;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * @Author: zhengfeng
 * @Date: 2025/8/19 19:51
 * @Description: TODO
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteLoader {

    private final NacosConfigManager nacosConfigManager;
    private final RouteDefinitionWriter routeDefinitionWriter;
    private final String dataId = "gateway-routes.json";
    private final String group = "DEFAULT_GROUP";
    private final Set<String> routIds = new HashSet<>();

    @PostConstruct
    public void initRoutConfigListener() throws NacosException {
        // 1.项目启动时，先拉取一次配置，并且添加监听器
        String configInfo = nacosConfigManager.getConfigService()
                .getConfigAndSignListener(dataId, group, 5000, new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        // 2.监听到配置变更，需要去更新路由表
                        updateConfigInfo(configInfo);
                    }
                });
        // 3.第一次读取到配置，也需要更新到路由表
        updateConfigInfo(configInfo);
    }

    /**
     * @description: 更新路由信息
     * @param configInfo 路由表信息
     * @return null
     */
    public void updateConfigInfo(String configInfo){
        log.debug("监听到路由配置信息：{}", configInfo);
        // 1.解析配置文件，转换为RouteDefinition
        List<RouteDefinition> routeDefinitions = JSONUtil.toList(configInfo, RouteDefinition.class);
        // 2.删除旧的路由表
        for (String routId : routIds) {
            routeDefinitionWriter.delete(Mono.just(routId)).subscribe();
        }
        routIds.clear();
        // 3.更新路由表
        for (RouteDefinition routeDefinition : routeDefinitions) {
            // 3.1. 更新路由表
            routeDefinitionWriter.save(Mono.just(routeDefinition)).subscribe();
            // 3.2. 记录更新路由id，方便下次删除路由表
            routIds.add(routeDefinition.getId());
        }
    }
}
