package com.ovo.little.project.rocketmq.api.hotel.service.impl;

import com.alibaba.fastjson.JSON;
import com.ovo.little.project.rocketmq.api.hotel.dto.HotelRoom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author QAQ
 * @date 2021/8/6
 */

@Component
public class HotelRoomCacheManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(HotelRoomCacheManager.class);

    private ConcurrentHashMap<Long, HotelRoom> hotelRoomCacheMap = new ConcurrentHashMap<>();

    /**
     * 根据房间id获取房间jvm内存信息
     *
     * @param roomId 房间id
     * @return 结果
     */
    public HotelRoom getHotelRoomFromLocalCache(Long roomId) {
        return hotelRoomCacheMap.get(roomId);
    }

    /**
     * 更新本地缓存
     *
     * @param hotelRoom 酒店房间数据
     */
    public void updateLocalCache(HotelRoom hotelRoom) {
        hotelRoomCacheMap.put(hotelRoom.getId(), hotelRoom);
        LOGGER.info("hotel room local cache data:{}", JSON.toJSONString(hotelRoomCacheMap));
    }
}
