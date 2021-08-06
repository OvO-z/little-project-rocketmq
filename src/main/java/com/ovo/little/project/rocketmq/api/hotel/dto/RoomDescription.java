package com.ovo.little.project.rocketmq.api.hotel.dto;

import lombok.Data;

/**
 * @author QAQ
 * @date 2021/8/6
 */

@Data
public class RoomDescription {
    /**
     * 面积
     */
    private String area;

    /**
     * 宽高
     */
    private String bed;

    /**
     * 早餐的份数
     */
    private Integer breakfast;
}
