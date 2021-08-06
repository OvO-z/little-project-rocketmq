package com.ovo.little.project.rocketmq.admin.producer;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Configuration
public class HotelRoomProducerConfiguration {
    @Value("${rocketmq.namesrv.address}")
    private String namesrvAddress;

    @Value("${rocketmq.hotelRoom.producer.group}")
    private String hotelRoomProducerGroup;

    /**
     * 登录生产者
     *
     * @return 登录消息rocketmq的生产者对象
     */
    @Bean(value = "hotelRoomMqProducer")
    public DefaultMQProducer hotelRoomMqProducer() throws MQClientException {
        DefaultMQProducer producer = new DefaultMQProducer(hotelRoomProducerGroup);
        producer.setNamesrvAddr(namesrvAddress);
        producer.start();
        return producer;
    }
}
