package com.ovo.little.project.rocketmq.api.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Data
public class OrderInfoDTO {
    /**
     * 主键id
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
     * 开放id
     */
    private String openId;

    /**
     * 房间id
     */
    private Integer roomId;

    /**
     * 酒店id
     */
    private Integer hotelId;

    /**
     * 酒店名字
     */
    private String hotelName;

    /**
     * 订购数量
     */
    private Integer total;

    /**
     * 订单总金额
     */
    private BigDecimal totalPrice;

    /**
     * 房主姓名
     */
    private String name;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 预定时间
     */
    private String remark;

    /**
     * 入住开始时间
     */
    private String beginDate;

    /**
     * 入住结束时间
     */
    private String endDate;

    /**
     * 订单的状态
     */
    private Integer status;

    /**
     * 订单创建时间 Unix时间
     */
    private Integer createTime;

    /**
     * 订单支付时间 Unix时间
     */
    private Integer payTime;

    /**
     * 订单取消时间 Unix时间
     */
    private Integer cancelTime;

    /**
     * 订单商品信息
     */
    private OrderItemDTO orderItem;

    /**
     * 优惠券id
     */
    private Integer couponId;

    /**
     * 优惠券金额
     */
    private BigDecimal couponMoney;

    /**
     * 用户id
     */
    private Integer userId;
}
