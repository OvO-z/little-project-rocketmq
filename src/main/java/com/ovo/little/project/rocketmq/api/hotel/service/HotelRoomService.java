package com.ovo.little.project.rocketmq.api.hotel.service;

import com.ovo.little.project.rocketmq.api.hotel.dto.HotelRoom;
import com.ruyuan.little.project.common.dto.CommonResponse;

/**
 * @author QAQ
 * @date 2021/8/6
 */
public interface HotelRoomService {

    /**
     * 根据小程序房间id查询房间详情
     *
     * @param id          房间id
     * @param phoneNumber 手机号
     * @return 结果
     */
    CommonResponse<HotelRoom> getRoomById(Long id, String phoneNumber);
}
