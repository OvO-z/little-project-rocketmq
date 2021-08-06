package com.ovo.little.project.rocketmq.api.hotel.dto;

import lombok.Data;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Data
public class HotelRoomMessage {
    /**
     * 房间id
     */
    private Long roomId;

    /**
     * 手机号
     */
    private String phoneNumber;

}
