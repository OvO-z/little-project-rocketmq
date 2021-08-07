package com.ovo.little.project.rocketmq.api.order.listener;

import com.alibaba.fastjson.JSON;
import com.ovo.little.project.rocketmq.api.order.dto.OrderInfoDTO;
import com.ovo.little.project.rocketmq.api.order.enums.OrderStatusEnum;
import com.ovo.little.project.rocketmq.api.order.service.OrderEventInformManager;
import com.ovo.little.project.rocketmq.api.order.service.OrderService;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Component
public class FinishedOrderTransactionListener implements TransactionListener {

    /**
     * 日志组件
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FinishedOrderTransactionListener.class);

    /**
     * 订单service组件
     */
    @Autowired
    private OrderService orderService;

    /**
     * 订单事件消息通知管理组件
     */
    @Autowired
    private OrderEventInformManager orderEventInformManager;

    /**
     * 执行本地事务
     *
     * @param message
     * @param o
     * @return
     */
    @Override
    public LocalTransactionState executeLocalTransaction(Message message, Object o) {
        // TODO 可以通过状态模式来校验订单的流转和保存订单操作日志
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        OrderInfoDTO orderInfoDTO = JSON.parseObject(body, OrderInfoDTO.class);
        String orderNo = orderInfoDTO.getOrderNo();
        String phoneNumber = orderInfoDTO.getPhoneNumber();
        LOGGER.info("callback execute finished order local transaction orderNo:{}", orderNo);
        try {
            // 修改订单的状态
            orderService.updateOrderStatus(orderNo, OrderStatusEnum.FINISHED, phoneNumber);

            // 发送确认通知
            orderEventInformManager.informOrderFinishEvent(orderInfoDTO);

            // 成功 提交prepare消息
            LOGGER.info("finished order local transaction execute success commit orderNo:{}", orderNo);
            return LocalTransactionState.COMMIT_MESSAGE;
        } catch (Exception e) {
            // 执行本地事务失败 回滚prepare消息
            LOGGER.info("finished order local transaction execute fail rollback orderNo:{}", orderNo);
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }

    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt messageExt) {
        String body = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        OrderInfoDTO orderInfoDTO = JSON.parseObject(body, OrderInfoDTO.class);
        String orderNo = orderInfoDTO.getOrderNo();
        String phoneNumber = orderInfoDTO.getPhoneNumber();
        LOGGER.info("callback check finished order local transaction status orderNo:{}", orderNo);
        try {
            Integer orderStatus = orderService.getOrderStatus(orderNo, phoneNumber);
            if (Objects.equals(orderStatus, OrderStatusEnum.FINISHED.getStatus())) {
                LOGGER.info("finished order local transaction check result success commit orderNo:{}", orderNo);
                return LocalTransactionState.COMMIT_MESSAGE;
            } else {
                LOGGER.info("finished order local transaction check result fail rollback orderNo:{}", orderNo);
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }
        } catch (Exception e) {
            // 查询订单状态失败
            LOGGER.info("finished order local transaction check result fail rollback orderNo:{}", orderNo);
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }
}
