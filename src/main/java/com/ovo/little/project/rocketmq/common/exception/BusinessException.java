package com.ovo.little.project.rocketmq.common.exception;

/**
 * @author QAQ
 * @date 2021/8/6
 */
public class BusinessException extends RuntimeException{
    public BusinessException(String message) {
        super(message);
    }
}
