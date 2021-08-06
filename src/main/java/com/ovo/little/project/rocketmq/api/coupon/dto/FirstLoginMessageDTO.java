package com.ovo.little.project.rocketmq.api.coupon.dto;

import lombok.Data;

/**
 * @author QAQ
 * @date 2021/8/6
 */

@Data
public class FirstLoginMessageDTO {

    private Integer userId;

    private String nickname;

    private Integer beid;

    private String phoneNumber;
}
