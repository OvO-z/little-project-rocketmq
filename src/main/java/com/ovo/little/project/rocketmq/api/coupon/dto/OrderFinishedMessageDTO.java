package com.ovo.little.project.rocketmq.api.coupon.dto;

import lombok.Data;

/**
 * @author QAQ
 * @date 2021/8/8
 */

@Data
public class OrderFinishedMessageDTO {
    /**
     * 订单id
     */
    private Integer id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 店铺的id
     */
    private Integer beid;

    /**
     * 用户id
     */
    private Integer userId;

    /**
     * 手机号
     */
    private String phoneNumber;
}
