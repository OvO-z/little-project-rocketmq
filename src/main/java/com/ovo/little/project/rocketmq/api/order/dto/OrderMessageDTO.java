package com.ovo.little.project.rocketmq.api.order.dto;

import com.ruyuan.little.project.common.enums.MessageTypeEnum;
import lombok.Data;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Data
public class OrderMessageDTO {
    /**
     * 消息内容
     */
    private String content;

    /**
     * 订单消息推送类型 {@link MessageTypeEnum}
     */
    private MessageTypeEnum messageType;
}
