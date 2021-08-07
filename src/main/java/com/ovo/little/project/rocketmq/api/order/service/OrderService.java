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
}
