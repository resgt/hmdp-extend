package com.hmdp.shop.config;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@Configuration
public class CanalConfig {
    @Value("${canal.server}")
    private String canalServer;

    @Value("${canal.destination}")
    private String destination;

    @Bean
    public CanalConnector canalConnector() {
        String[] serverInfo = canalServer.split(":");
        CanalConnector connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(serverInfo[0], Integer.parseInt(serverInfo[1])),
                destination,
                "",
                ""
        );
        return connector;
    }
}
