package com.ovo.little.project.rocketmq.api.pay.service.impl;

import com.alibaba.fastjson.JSON;
import com.ovo.little.project.rocketmq.api.pay.dto.PayTransaction;
import com.ovo.little.project.rocketmq.api.pay.service.PayTransactionService;
import com.ruyuan.little.project.common.dto.CommonResponse;
import com.ruyuan.little.project.common.enums.ErrorCodeEnum;
import com.ruyuan.little.project.common.enums.LittleProjectTypeEnum;
import com.ruyuan.little.project.mysql.api.MysqlApi;
import com.ruyuan.little.project.mysql.dto.MysqlRequestDTO;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Service
public class PayTransactionServiceImpl implements PayTransactionService {

    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PayTransactionServiceImpl.class);

    /**
     * mysql dubbo api接口
     */
    @Reference(version = "1.0.0",
            interfaceClass = MysqlApi.class,
            cluster = "failfast")
    private MysqlApi mysqlApi;

    @Override
    public Boolean save(PayTransaction payTransaction, String phoneNumber) {
        MysqlRequestDTO mysqlRequestDTO = new MysqlRequestDTO();
        mysqlRequestDTO.setSql("INSERT INTO pay_transaction("
                + "order_no, "
                + "total_order_amount, "
                + "payable_order_amount, "
                + "user_pay_account, "
                + "transaction_channel, "
                + "transaction_number, "
                + "finish_pay_time, "
                + "response_code, "
                + "status, "
                + "gmt_create, "
                + "gmt_modified "
                + ") VALUES("
                + "?, "
                + "?, "
                + "?, "
                + "?, "
                + "?, "
                + "?, "
                + "?, "
                + "?, "
                + "?, "
                + "?, "
                + "? "
                + ")");
        mysqlRequestDTO.setPhoneNumber(phoneNumber);
        mysqlRequestDTO.setProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);
        List<Object> params = new ArrayList<>();
        params.add(payTransaction.getOrderNo());
        params.add(payTransaction.getPayableAmount());
        params.add(payTransaction.getPayableAmount());
        params.add(payTransaction.getUserPayAccount());
        params.add(payTransaction.getTransactionChannel());
        params.add(payTransaction.getTransactionNumber());
        params.add(payTransaction.getFinishPayTime());
        params.add(payTransaction.getResponseCode());
        params.add(payTransaction.getStatus());
        Date currentDate = new Date();
        params.add(currentDate);
        params.add(currentDate);
        mysqlRequestDTO.setParams(params);

        LOGGER.info("start save pay transaction params:{}", JSON.toJSONString(params));
        CommonResponse<Integer> response = mysqlApi.insert(mysqlRequestDTO);
        LOGGER.info("end save pay transaction response:{}", JSON.toJSONString(response));
        return Objects.equals(response.getCode(), ErrorCodeEnum.SUCCESS.getCode());
    }
}
