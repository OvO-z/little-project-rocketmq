package com.ovo.little.project.rocketmq.api.coupon.listener;

import com.alibaba.fastjson.JSON;
import com.ovo.little.project.rocketmq.api.coupon.dto.OrderFinishedMessageDTO;
import com.ovo.little.project.rocketmq.api.coupon.service.CouponService;
import com.ovo.little.project.rocketmq.common.constant.RedisKeyConstant;
import com.ruyuan.little.project.common.dto.CommonResponse;
import com.ruyuan.little.project.common.enums.ErrorCodeEnum;
import com.ruyuan.little.project.common.enums.LittleProjectTypeEnum;
import com.ruyuan.little.project.redis.api.RedisApi;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * @author QAQ
 * @date 2021/8/8
 */

@Component
public class OrderFinishedMessageListener implements MessageListenerConcurrently {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderFinishedMessageListener.class);

    /**
     * redis dubbo服务
     */
    @Reference(version = "1.0.0",
            interfaceClass = RedisApi.class,
            cluster = "failfast")
    private RedisApi redisApi;

    /**
     * 优惠券服务service组件
     */
    @Autowired
    private CouponService couponService;

    /**
     * 第一次登陆下发的优惠券id
     */
    @Value("${order.finished.couponId}")
    private Integer orderFinishedCouponId;

    /**
     * 第一次登陆优惠券有效天数
     */
    @Value("${order.finished.coupon.day}")
    private Integer orderFinishedCouponDay;


    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
        for (MessageExt msg : list) {
            String body = new String(msg.getBody(), StandardCharsets.UTF_8);
            LOGGER.info("received order finished message:{}", body);

            OrderFinishedMessageDTO orderFinishedMessageDTO = JSON.parseObject(body, OrderFinishedMessageDTO.class);
            // 权益分发保证接口的幂等
            String orderNo = orderFinishedMessageDTO.getOrderNo();
            String phoneNumber = orderFinishedMessageDTO.getPhoneNumber();
            CommonResponse<Boolean> response = redisApi.setnx(RedisKeyConstant.ORDER_FINISHED_DUPLICATION_KEY_PREFIX + orderNo,
                    orderNo,
                    phoneNumber,
                    LittleProjectTypeEnum.ROCKETMQ);
            if (Objects.equals(response.getCode(), ErrorCodeEnum.SUCCESS.getCode())
                    && Objects.equals(response.getData(), Boolean.FALSE)) {
                // 重复消费订单退房消息 返回
                LOGGER.info("duplicate consumer order finished message orderNo:{}", orderNo);
            } else {
                // 未重复消费 分发半折优惠券权益
                couponService.distributeCoupon(orderFinishedMessageDTO.getBeid(),
                        orderFinishedMessageDTO.getUserId(),
                        orderFinishedCouponId,
                        orderFinishedCouponDay,
                        orderFinishedMessageDTO.getId(), phoneNumber);
                LOGGER.info("distribute orderNo:{} finished order coupon end", orderNo);
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}
