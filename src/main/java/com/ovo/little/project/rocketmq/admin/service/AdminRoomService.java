package com.ovo.little.project.rocketmq.admin.service;

import com.ovo.little.project.rocketmq.admin.dto.AdminHotelRoom;
import com.ruyuan.little.project.common.dto.CommonResponse;

/**
 * @author QAQ
 * @date 2021/8/7
 */
public interface AdminRoomService {
    /**
     * 添加房间
     *
     * @param adminHotelRoom 房间内容
     * @return 结果
     */
    CommonResponse add(AdminHotelRoom adminHotelRoom);

    /**
     * 更新商品信息
     *
     * @param adminHotelRoom 请求体内容
     * @return
     */
    CommonResponse update(AdminHotelRoom adminHotelRoom);
}
