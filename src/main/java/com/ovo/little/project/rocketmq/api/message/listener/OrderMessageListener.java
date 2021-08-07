package com.ovo.little.project.rocketmq.api.message.listener;

import com.alibaba.fastjson.JSON;
import com.ovo.little.project.rocketmq.api.message.dto.OrderInfo;
import com.ovo.little.project.rocketmq.api.message.dto.OrderMessage;
import com.ovo.little.project.rocketmq.common.utils.DateUtil;
import com.ruyuan.little.project.common.dto.CommonResponse;
import com.ruyuan.little.project.common.enums.LittleProjectTypeEnum;
import com.ruyuan.little.project.common.enums.MessageTypeEnum;
import com.ruyuan.little.project.message.api.WxSubscribeMessageApi;
import com.ruyuan.little.project.message.dto.ValueDTO;
import com.ruyuan.little.project.message.dto.WaitPayOrderMessageDTO;
import com.ruyuan.little.project.message.dto.WxSubscribeMessageDTO;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Component
public class OrderMessageListener implements MessageListenerOrderly {

    /**
     * 日志组件
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderMessageListener.class);

    /**
     * mysql dubbo api接口
     */
    @Reference(version = "1.0.0",
            interfaceClass = WxSubscribeMessageApi.class,
            cluster = "failfast")
    private WxSubscribeMessageApi wxSubscribeMessageApi;


    @Override
    public ConsumeOrderlyStatus consumeMessage(List<MessageExt> list, ConsumeOrderlyContext consumeOrderlyContext) {
        for (MessageExt msg: list) {
            String content = new String(msg.getBody(), StandardCharsets.UTF_8);
            LOGGER.info("received order message:{}", content);

            // 订单消息
            OrderMessage orderMessage = JSON.parseObject(content, OrderMessage.class);

            // 订单内容
            OrderInfo orderInfo = JSON.parseObject(orderMessage.getContent(), OrderInfo.class);
            MessageTypeEnum messageType = orderMessage.getMessageType();

            // 发送消息
            try {
                if (Objects.equals(messageType, MessageTypeEnum.WX_CREATE_ORDER)) {
                    WaitPayOrderMessageDTO waitPayOrderMessageDTO = this.builderWaitPayOrderMessage(orderInfo);
                    this.send(waitPayOrderMessageDTO, orderInfo.getPhoneNumber(), messageType);
                }
            }catch (Exception e) {
                LOGGER.error("push wx message fail error message:{}", e.getMessage());
                // 发送消息失败 Suspend current queue a moment
                return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
            }
        }
        return ConsumeOrderlyStatus.SUCCESS;
    }

    /**
     * 创建待支付订单消息
     *
     * @param orderInfo 订单信息
     * @return 待支付消息
     */
    private WaitPayOrderMessageDTO builderWaitPayOrderMessage(OrderInfo orderInfo) {
        WaitPayOrderMessageDTO waitPayOrderMessageDTO = new WaitPayOrderMessageDTO();
        ValueDTO number1 = new ValueDTO();
        // TODO 由于模板字段只能为数字这里用订单id
        number1.setValue(orderInfo.getId());
        waitPayOrderMessageDTO.setNumber1(number1);

        ValueDTO time2 = new ValueDTO();
        long createTime = orderInfo.getCreateTime() * 1000L;
        time2.setValue(DateUtil.format(new Date(createTime), DateUtil.FULL_TIME_SPLIT_PATTERN));
        waitPayOrderMessageDTO.setTime2(time2);

        ValueDTO time10 = new ValueDTO();
        // 创建时间30分钟之后
        long validTime = (orderInfo.getCreateTime() + 30 * 60) * 1000L;
        time10.setValue(DateUtil.format(new Date(validTime), DateUtil.FULL_TIME_SPLIT_PATTERN));
        waitPayOrderMessageDTO.setTime10(time10);

        ValueDTO thing3 = new ValueDTO();
        thing3.setValue(orderInfo.getOrderItem().getTitle());
        waitPayOrderMessageDTO.setThing3(thing3);
        return waitPayOrderMessageDTO;
    }

    /**
     * 实际发送消息
     *
     * @param wxOrderMessage 消息内容
     * @param phoneNumber    手机号
     * @param messageType    消息类型
     */
    private <T> void send(T wxOrderMessage, String phoneNumber, MessageTypeEnum messageType) {
        WxSubscribeMessageDTO<T> subscribeMessageDTO = new WxSubscribeMessageDTO<>();
        subscribeMessageDTO.setContent(wxOrderMessage);
        subscribeMessageDTO.setLittleProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);
        subscribeMessageDTO.setMessageTypeEnum(messageType);
        subscribeMessageDTO.setPhoneNumber(phoneNumber);
        LOGGER.info("start push order message to weixin param:{}", JSON.toJSONString(subscribeMessageDTO));
        CommonResponse response = wxSubscribeMessageApi.send(subscribeMessageDTO);
        LOGGER.info("end push order message to weixin param:{}, response:{}", JSON.toJSONString(subscribeMessageDTO), JSON.toJSONString(response));
    }
}
