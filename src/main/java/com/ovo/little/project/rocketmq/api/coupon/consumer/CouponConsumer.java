package com.ovo.little.project.rocketmq.api.coupon.consumer;

import com.ovo.little.project.rocketmq.api.coupon.listener.FirstLoginMessageListener;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * @author QAQ
 * @date 2021/8/6
 */


public class CouponConsumer {

    @Value("${rocketmq.namesrv.address}")
    private String namesrvAddress;

    /**
     * 登录topic
     */
    @Value("${rocketmq.login.topic}")
    private String loginTopic;

    @Value("${rocketmq.login.consumer.group}")
    private String loginConsumerGroup;

    @Bean("loginConsumer")
    public DefaultMQPushConsumer loginConsumer(@Qualifier("firstLoginMessageListener")FirstLoginMessageListener firstLoginMessageListener) throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(loginConsumerGroup);
        consumer.setNamesrvAddr(namesrvAddress);
        consumer.subscribe(loginTopic, "");
        consumer.setMessageListener(firstLoginMessageListener);
        consumer.start();
        return consumer;
    }
}
