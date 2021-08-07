package com.ovo.little.project.rocketmq.api.pay.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Data
public class QueryPayStatusResponse {
    /**
     * 用户手机号
     */
    private String phoneNumber;

    /**
     * 订单编号
     */
    private String orderNo;
    /**
     * 用户支付账号
     */
    private String userPayAccount;

    /**
     * 订单应付金额
     */
    private BigDecimal payableAmount;

    /**
     * 第三方支付交易流水号
     */
    private String  transactionNumber;
    /**
     * 第三方支付完成支付的时间
     */
    private Date finishPayTime;
    /**
     * 第三方支付响应状态码
     */
    private String  responseCode;
    /**
     * 支付交易状态
     */
    private Integer payTransactionStatus;
}
