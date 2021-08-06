package com.ovo.little.project.rocketmq.api;

import com.ruyuan.little.project.common.dto.CommonResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author QAQ
 * @date 2021/8/5
 */

@RestController
public class HealthController {
    @GetMapping("/")
    public CommonResponse health() {
        return CommonResponse.success();
    }
}
