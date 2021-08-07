package com.ovo.little.project.rocketmq.admin.service;

import com.ruyuan.little.project.common.dto.CommonResponse;

/**
 * @author QAQ
 * @date 2021/8/7
 */
public interface AdminOrderService {
    /**
     * 确认订单入住
     *
     * @param orderNo     订单号
     * @param phoneNumber 手机号
     * @return 结果
     */
    CommonResponse confirmOrder(String orderNo, String phoneNumber);

}
