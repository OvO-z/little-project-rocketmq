package com.ovo.little.project.rocketmq.api.hotel.listener;

import com.alibaba.fastjson.JSON;
import com.ovo.little.project.rocketmq.api.hotel.dto.HotelRoom;
import com.ovo.little.project.rocketmq.api.hotel.dto.HotelRoomMessage;
import com.ovo.little.project.rocketmq.api.hotel.service.impl.HotelRoomCacheManager;
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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * @author QAQ
 * @date 2021/8/7
 */
public class HotelRoomUpdateMessageListener implements MessageListenerConcurrently {

    private static final Logger LOGGER = LoggerFactory.getLogger(HotelRoomUpdateMessageListener.class);

    @Reference(version = "1.0.0",
            interfaceClass = RedisApi.class,
            cluster = "failfast")
    private RedisApi redisApi;

    /**
     * 酒店房间本地缓存
     */
    @Autowired
    private HotelRoomCacheManager hotelRoomCacheManager;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
        // 处理房间更新成功的消息
        // 从redis中取更新更新房间缓存信息
        for (MessageExt msg : list) {
            // TODO 可以优化为批量查询
            String body = new String(msg.getBody(), StandardCharsets.UTF_8);
            try {
                HotelRoomMessage hotelRoomMessage = JSON.parseObject(body, HotelRoomMessage.class);
                Long roomId = hotelRoomMessage.getRoomId();
                LOGGER.info("receiver room update message roomId:{}", roomId);
                LOGGER.info("start query hotel room from redis cache param:{}", roomId);
                CommonResponse<String> commonResponse = redisApi.get(RedisKeyConstant.HOTEL_ROOM_KEY_PREFIX + roomId,
                        hotelRoomMessage.getPhoneNumber(),
                        LittleProjectTypeEnum.ROCKETMQ);
                LOGGER.info("end query hotel room from redis cache param:{}", roomId);
                if (Objects.equals(commonResponse.getCode(), ErrorCodeEnum.SUCCESS.getCode())) {
                    // 成功 赋值给jvm内存
                    LOGGER.info("update hotel room local cache data:{}", commonResponse.getData());
                    hotelRoomCacheManager.updateLocalCache(JSON.parseObject(commonResponse.getData(), HotelRoom.class));
                }
            } catch (Exception e) {
                // 消费失败
                LOGGER.info("received hotel room update message:{}, consumer fail", body);
                // Failure consumption,later try to consume 消费失败，以后尝试消费
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}
