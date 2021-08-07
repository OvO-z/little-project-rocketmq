package com.ovo.little.project.rocketmq.api.order.dto;

import lombok.Data;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Data
public class CreateOrderResponseDTO {
    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单id
     */
    private Integer orderId;
}
