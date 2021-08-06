package com.ovo.little.project.rocketmq.api.login.dto;

import lombok.Data;

/**
 * @author QAQ
 * @date 2021/8/6
 */

@Data
public class LoginRequestDTO {

    /**
     * 用户ID
     */
    private Integer userId;

    private String nickName;

    private String phoneNumber;

    private String token;

    private Integer beid;
}
