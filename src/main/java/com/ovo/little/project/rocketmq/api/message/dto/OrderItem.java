package com.ovo.little.project.rocketmq.api.message.dto;

import lombok.Data;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Data
public class OrderItem {
    /**
     * 房间名称
     */
    private String title;

    /**
     * 订购数量
     */
    private Integer total;

}
