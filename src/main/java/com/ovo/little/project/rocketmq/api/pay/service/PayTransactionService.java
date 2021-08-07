package com.ovo.little.project.rocketmq.api.pay.service;

import com.ovo.little.project.rocketmq.api.pay.dto.PayTransaction;

/**
 * @author QAQ
 * @date 2021/8/7
 */

public interface PayTransactionService {
    /**
     * 保存支付流水记录
     *
     * @param payTransaction 支付流水
     * @param phoneNumber    手机号
     * @return 记录流水结果
     */
    Boolean save(PayTransaction payTransaction, String phoneNumber);
}
