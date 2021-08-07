package com.ovo.little.project.rocketmq.admin.service.impl;

import com.ovo.little.project.rocketmq.admin.service.AdminOrderService;
import com.ovo.little.project.rocketmq.api.order.service.OrderService;
import com.ruyuan.little.project.common.dto.CommonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Service
public class AdminOrderServiceImpl implements AdminOrderService {

    @Autowired
    private OrderService orderService;

    @Override
    public CommonResponse confirmOrder(String orderNo, String phoneNumber) {
        // TODO 正常调用订单服务的dubbo接口或者操作数据库
        orderService.informConfirmOrder(orderNo, phoneNumber);
        return CommonResponse.success();
    }

    @Override
    public CommonResponse finishedOrder(String orderNo, String phoneNumber) {
        orderService.informFinishedOrder(orderNo, phoneNumber);
        return CommonResponse.success();
    }
}
