package com.hmdp.shop.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CanalClient implements InitializingBean {

    @Resource
    private CanalConnector canalConnector;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void afterPropertiesSet() {
        // 开启一个线程执行Canal客户端
        new Thread(this::process).start();
    }

    private void process() {
        try {
            // 连接Canal服务器
            canalConnector.connect();
            // 订阅数据库
            canalConnector.subscribe(".*\\..*");
            // 回滚到未进行ack的地方
            canalConnector.rollback();
            
            while (true) {
                // 获取数据
                Message message = canalConnector.getWithoutAck(100);
                long batchId = message.getId();
                try {
                    List<CanalEntry.Entry> entries = message.getEntries();
                    if (batchId != -1 && entries.size() > 0) {
                        entries.forEach(entry -> {
                            if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                                // 解析处理
                                publishCanalEvent(entry);
                            }
                        });
                    }
                    // 提交确认
                    canalConnector.ack(batchId);
                } catch (Exception e) {
                    // 处理失败，回滚数据
                    canalConnector.rollback(batchId);
                    log.error("Canal处理数据失败", e);
                }
            }
        } catch (Exception e) {
            log.error("Canal客户端异常", e);
        } finally {
            canalConnector.disconnect();
        }
    }

    private void publishCanalEvent(CanalEntry.Entry entry) {
        try {
            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            String tableName = entry.getHeader().getTableName();
            CanalEntry.EventType eventType = rowChange.getEventType();

            // 只处理shop表的数据
            if (!"tb_shop".equals(tableName)) {
                return;
            }

            // 遍历每一行数据
            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                // 获取变更后的数据
                List<CanalEntry.Column> columns = rowData.getAfterColumnsList();
                
                // 构建消息
                Map<String, Object> data = new HashMap<>();
                columns.forEach(column -> 
                    data.put(column.getName(), column.getValue())
                );

                // 构建完整消息
                Map<String, Object> message = new HashMap<>();
                message.put("type", eventType.toString());
                message.put("data", data);

                // 发送消息到RocketMQ
                rocketMQTemplate.convertAndSend(
                    "shop-topic",
                    message
                );
                
                log.info("发送Canal消息到MQ，表：{}，类型：{}", tableName, eventType);
            }
        } catch (Exception e) {
            log.error("处理Canal消息失败", e);
        }
    }
}
