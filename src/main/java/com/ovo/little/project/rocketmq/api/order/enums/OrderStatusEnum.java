package com.ovo.little.project.rocketmq.api.order.enums;

/**
 * @author QAQ
 * @date 2021/8/7
 */

public enum OrderStatusEnum {
    /**
     * 创建订单后订单待付款
     */
    WAITING_FOR_PAY(0, "待付款"),

    /**
     * 预定下单之后取消订单
     */
    CANCELED(1, "已取消"),

    /**
     * 待入住 支付订单后等待入住
     */
    WAITING_FOR_LIVE(2, "待入住"),

    /**
     * 已入住 待退房
     */
    CONFIRM(3, "待退房"),

    /**
     * 入住 退房完成
     */
    FINISHED(4, "已完成");

    private Integer status;

    private String desc;

    OrderStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
