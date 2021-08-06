package com.ovo.little.project.rocketmq.admin.dto;

import lombok.Data;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Data
public class AdminRoomDescription {
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
