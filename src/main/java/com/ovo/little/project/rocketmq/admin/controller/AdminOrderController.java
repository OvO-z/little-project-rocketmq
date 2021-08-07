package com.ovo.little.project.rocketmq.admin.controller;

import com.ovo.little.project.rocketmq.admin.service.AdminOrderService;
import com.ruyuan.little.project.common.dto.CommonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@RestController
@RequestMapping("/admin/order/")
public class AdminOrderController {
    @Autowired
    private AdminOrderService adminOrderService;

    /**
     * 入住订单
     *
     * @param orderNo     订单号
     * @param phoneNumber 手机号
     * @return 结果
     */
    @GetMapping(value = "/confirmOrder")
    public CommonResponse confirmOrder(@RequestParam(value = "orderNo") String orderNo,
                                       @RequestParam(value = "phoneNumber") String phoneNumber) {
        return adminOrderService.confirmOrder(orderNo, phoneNumber);
    }
}
