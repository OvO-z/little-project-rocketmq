package com.ovo.little.project.rocketmq.api.order.service;

import com.ovo.little.project.rocketmq.api.order.dto.CreateOrderResponseDTO;
import com.ovo.little.project.rocketmq.api.order.dto.OrderInfoDTO;
import com.ruyuan.little.project.common.dto.CommonResponse;

/**
 * @author QAQ
 * @date 2021/8/7
 */

public interface OrderService {
    /**
     * 创建订单
     *
     * @param orderInfoDTO 订单信息
     * @return 结果
     */
    CommonResponse<CreateOrderResponseDTO> createOrder(OrderInfoDTO orderInfoDTO);

    /**
     * 取消订单
     *
     * @param orderNo     订单编号
     * @param phoneNumber 手机号
     * @return 结果
     */
    CommonResponse cancelOrder(String orderNo, String phoneNumber);

    /**
     * 支付订单
     *
     * @param orderNo     订单号
     * @param phoneNumber 用户手机号
     * @return 结果 订单id
     */
    Integer informPayOrderSuccessed(String orderNo, String phoneNumber);
}
