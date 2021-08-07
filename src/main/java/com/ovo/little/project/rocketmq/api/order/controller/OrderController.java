package com.ovo.little.project.rocketmq.api.order.controller;

import com.ovo.little.project.rocketmq.api.order.dto.CreateOrderResponseDTO;
import com.ovo.little.project.rocketmq.api.order.dto.OrderInfoDTO;
import com.ovo.little.project.rocketmq.api.order.service.OrderService;
import com.ruyuan.little.project.common.dto.CommonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@RestController
@RequestMapping("/api/order")
public class OrderController {
    /**
     * 订单service组件
     */
    @Autowired
    private OrderService orderService;

    /**
     * 预定房间 创建订单
     *
     * @param orderInfoDTO 订单信息
     * @return 结果   订单号
     */
    @PostMapping(value = "createOrder")
    public CommonResponse<CreateOrderResponseDTO> createOrder(OrderInfoDTO orderInfoDTO) {
        return orderService.createOrder(orderInfoDTO);
    }

}
