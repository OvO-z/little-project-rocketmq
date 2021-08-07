package com.ovo.little.project.rocketmq.api.pay.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Data
public class PayTransaction {
    /**
     * id
     */
    private Long id;
    /**
     * 订单编号
     */
    private String orderNo;
    /**
     * 订单应付金额
     */
    private BigDecimal payableAmount;

    /**
     * 用户支付账号
     */
    private String  userPayAccount;
    /**
     * 交易渠道
     */
    private Integer transactionChannel;
    /**
     * 第三方支付交易编号
     */
    private String  transactionNumber;
    /**
     * 第三方支付完成支付的时间
     */
    private String  finishPayTime;
    /**
     * 第三方支付的响应状态码
     */
    private String  responseCode;
    /**
     * 支付交易状态
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date gmtCreate;
    /**
     * 修改时间
     */
    private Date gmtModified;

}
