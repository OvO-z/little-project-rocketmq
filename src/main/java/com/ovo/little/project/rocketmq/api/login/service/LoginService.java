package com.ovo.little.project.rocketmq.api.login.service;

import com.ovo.little.project.rocketmq.api.login.dto.LoginRequestDTO;

/**
 * @author QAQ
 * @date 2021/8/6
 */

public interface LoginService {

    void firstLoginDistributeCoupon(LoginRequestDTO loginRequestDTO);

    void resetFirstLoginStatus(String phoneNumber);
}
