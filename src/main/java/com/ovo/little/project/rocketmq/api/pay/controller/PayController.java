package com.ovo.little.project.rocketmq.api.pay.controller;

import com.ovo.little.project.rocketmq.api.order.service.OrderService;
import com.ovo.little.project.rocketmq.api.pay.constants.PayTransactionStatusConstant;
import com.ovo.little.project.rocketmq.api.pay.dto.PayTransaction;
import com.ovo.little.project.rocketmq.api.pay.dto.QueryPayStatusResponse;
import com.ovo.little.project.rocketmq.api.pay.service.PayTransactionService;
import com.ovo.little.project.rocketmq.common.constant.PayTypeConstant;
import com.ovo.little.project.rocketmq.common.utils.DateUtil;
import com.ruyuan.little.project.common.dto.CommonResponse;
import com.ruyuan.little.project.common.enums.LittleProjectTypeEnum;
import com.ruyuan.little.project.redis.api.RedisApi;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import static com.ovo.little.project.rocketmq.common.constant.RedisKeyConstant.ORDER_DUPLICATION_KEY_PREFIX;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@RestController
@RequestMapping(value = "/api/pay")
public class PayController {
    @Autowired
    private PayTransactionService payTransactionService;

    @Autowired
    private OrderService orderService;

    /**
     * redis dubbo服务
     */
    @Reference(version = "1.0.0",
            interfaceClass = RedisApi.class,
            cluster = "failfast")
    private RedisApi redisApi;


    @PostMapping(value = "wx/callback")
    public CommonResponse<Integer> wxCallback(QueryPayStatusResponse queryPayStatusResponse) {

        String orderNo = queryPayStatusResponse.getOrderNo();
        String phoneNumber = queryPayStatusResponse.getPhoneNumber();

        PayTransaction payTransaction = new PayTransaction();
        payTransaction.setOrderNo(orderNo);
        payTransaction.setUserPayAccount(queryPayStatusResponse.getUserPayAccount());
        payTransaction.setTransactionNumber(queryPayStatusResponse.getTransactionNumber());
        payTransaction.setFinishPayTime(DateUtil.format(queryPayStatusResponse.getFinishPayTime(), DateUtil.FULL_TIME_SPLIT_PATTERN));
        payTransaction.setResponseCode(queryPayStatusResponse.getResponseCode());
        payTransaction.setTransactionChannel(PayTypeConstant.WX);
        payTransaction.setPayableAmount(queryPayStatusResponse.getPayableAmount());
        Integer status = queryPayStatusResponse.getPayTransactionStatus();
        payTransaction.setStatus(status);
        // 保存支付流水
        if (!payTransactionService.save(payTransaction, phoneNumber)) {
            // 失败 等待微信重试
            return CommonResponse.fail();
        }

        Integer orderId = null;
        if (Objects.equals(status, PayTransactionStatusConstant.SUCCESS)) {
            // 支付成功
            try {
                orderId = orderService.informPayOrderSuccessed(payTransaction.getOrderNo(), phoneNumber);
            } catch (Exception e) {
                // 支付订单异常 删除 幂等的key
                redisApi.del(ORDER_DUPLICATION_KEY_PREFIX + orderNo,
                        phoneNumber,
                        LittleProjectTypeEnum.ROCKETMQ);
                return CommonResponse.fail();
            }
        }
        return CommonResponse.success(orderId);
    }
}
