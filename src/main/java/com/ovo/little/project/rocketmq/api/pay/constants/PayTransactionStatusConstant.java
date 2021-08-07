package com.ovo.little.project.rocketmq.api.pay.constants;

/**
 * @author QAQ
 * @date 2021/8/7
 */

public class PayTransactionStatusConstant {
    /**
     * 未付款
     */
    public static final Integer UN_PAYED = 1;

    /**
     * 支付成功
     */
    public static final Integer SUCCESS = 2;

    /**
     * 支付失败
     */
    public static final Integer FAILURE = 3;

    /**
     * 支付交易关闭
     */
    public static final Integer CLOSED = 4;

    private PayTransactionStatusConstant() {

    }

}
