package com.ovo.little.project.rocketmq.api.login.controller;

import com.alibaba.fastjson.JSON;
import com.ovo.little.project.rocketmq.api.login.dto.LoginRequestDTO;
import com.ovo.little.project.rocketmq.api.login.service.LoginService;
import com.ruyuan.little.project.common.dto.CommonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author QAQ
 * @date 2021/8/6
 */

@RestController
@RequestMapping("/api/login")
public class LoginController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    /**
     * 登陆接口
     */
    @Autowired
    private LoginService loginService;


    /**
     * 登录请求
     *
     * @param loginRequestDTO 登录请求信息
     * @return 结果
     */
    @PostMapping(value = "/wxLogin")
    public CommonResponse wxLogin(@RequestBody LoginRequestDTO loginRequestDTO) {
        LOGGER.info("login success user info: {}", JSON.toJSONString(loginRequestDTO));

        loginService.firstLoginDistributeCoupon(loginRequestDTO);

        return CommonResponse.success();
    }

    /**
     * 重置登录状态 TODO 方便测试使用
     *
     * @param phoneNumber 用户手机号
     * @return 结果
     */
    @GetMapping(value = "/resetLoginStatus")
    public CommonResponse resetFirstLoginStatus(@RequestParam(value = "phoneNumber") String phoneNumber) {
        LOGGER.info("reset user first login status phoneNumber:{}", phoneNumber);
        loginService.resetFirstLoginStatus(phoneNumber);
        return CommonResponse.success();
    }
}
