package com.ovo.little.project.rocketmq.api.order.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Data
public class OrderItemDTO {
    /**
     * thumb,
     * beid,
     * orderid,
     * goodsId,
     * title,
     * price,
     * total,
     * order_dates,
     * description
     */
    /**
     * 房间图片地址
     */
    private String thumb;

    /**
     * 小程序店铺id
     */
    private Integer beid;

    /**
     * 订单id
     */
    private Integer orderId;

    /**
     * 房间id
     */
    private Integer roomId;

    /**
     * 房间名称
     */
    private String title;

    /**
     * 订购数量
     */
    private Integer total;

    /**
     * 预定天数
     */
    private String orderDates;

    /**
     * 房间的描述信息
     */
    private String description;

    /**
     * 房间价格
     */
    private BigDecimal price;

}
