package com.ovo.little.project.rocketmq.admin.controller;

import com.ovo.little.project.rocketmq.admin.dto.AdminHotelRoom;
import com.ovo.little.project.rocketmq.admin.service.AdminRoomService;
import com.ruyuan.little.project.common.dto.CommonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@RestController
@RequestMapping(value = "/admin/hotel/room")
public class AdminHotelRoomController {

    @Autowired
    private AdminRoomService roomAdminService;

    @PostMapping(value = "/add")
    public CommonResponse add(@RequestBody AdminHotelRoom adminHotelRoom) {
        return roomAdminService.add(adminHotelRoom);
    }

    @PostMapping(value = "/update")
    public CommonResponse update(@RequestBody AdminHotelRoom adminHotelRoom) {
        return roomAdminService.update(adminHotelRoom);
    }
}
