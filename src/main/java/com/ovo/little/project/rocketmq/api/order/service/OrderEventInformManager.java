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

    /**
     * 通知订单支付事件
     *
     * @param orderInfoDTO 订单信息
     */
    void informPayOrderEvent(OrderInfoDTO orderInfoDTO);

    /**
     * 入住成功事件
     *
     * @param orderInfoDTO 订单信息
     */
    void informConfirmOrderEvent(OrderInfoDTO orderInfoDTO);

    /**
     * 订单退房完成 订单已结束事件
     *
     * @param orderInfoDTO 订单信息
     */
    void informOrderFinishEvent(OrderInfoDTO orderInfoDTO);

}
