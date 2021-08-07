package com.ovo.little.project.rocketmq.api.order.service;

import com.ovo.little.project.rocketmq.api.order.dto.OrderInfoDTO;

/**
 * @author QAQ
 * @date 2021/8/7
 */

public interface OrderEventInformManager {
    /**
     * 通知创建订单事件
     *
     * @param orderInfoDTO 订单信息
     */
    void informCreateOrderEvent(OrderInfoDTO orderInfoDTO);

    /**
     * 通知取消订单事件
     *
     * @param orderInfoDTO 订单信息
     */
    void informCancelOrderEvent(OrderInfoDTO orderInfoDTO);
}
