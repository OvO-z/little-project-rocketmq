package com.ovo.little.project.rocketmq.api.message.dto;

import lombok.Data;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Data
public class OrderInfo {
    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 订单id
     */
    private Integer id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 订单创建时间 Unix时间
     */
    private Integer createTime;

    /**
     * 订单支付时间 Unix时间
     */
    private Integer payTime;

    /**
     * 订单支付时间 Unix时间
     */
    private Integer cancelTime;

    /**
     * 订单商品
     */
    private OrderItem orderItem;
}
