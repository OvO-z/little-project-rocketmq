package com.ovo.little.project.rocketmq.api.hotel.dto;

import lombok.Data;

/**
 * @author QAQ
 * @date 2021/8/6
 */

@Data
public class RoomPicture {
    /**
     * 图片id
     */
    private Integer id;

    /**
     * 图片地址
     */
    private String url;

    private String src;
}
