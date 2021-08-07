package com.ovo.little.project.rocketmq.api.coupon.service.impl;

import com.alibaba.fastjson.JSON;
import com.ovo.little.project.rocketmq.api.coupon.enums.CouponUsedStatusEnum;
import com.ovo.little.project.rocketmq.api.coupon.service.CouponService;
import com.ovo.little.project.rocketmq.common.utils.DateUtil;
import com.ruyuan.little.project.common.dto.CommonResponse;
import com.ruyuan.little.project.common.enums.LittleProjectTypeEnum;
import com.ruyuan.little.project.mysql.api.MysqlApi;
import com.ruyuan.little.project.mysql.dto.MysqlRequestDTO;
import org.apache.commons.lang.time.DateUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author QAQ
 * @date 2021/8/6
 */
@Service
public class CouponServiceImpl implements CouponService {

    /**
     * 日志组件
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CouponServiceImpl.class);

    /**
     * mysql dubbo服务
     */
    @Reference(version = "1.0.0",
            interfaceClass = MysqlApi.class,
            cluster = "failfast")
    private MysqlApi mysqlApi;

    @Override
    public void distributeCoupon(Integer beid, Integer userId, Integer couponConfigId, Integer validDay, Integer sourceOrderId, String phoneNumber) {
        MysqlRequestDTO mysqlRequestDTO = new MysqlRequestDTO();
        mysqlRequestDTO.setSql("INSERT INTO t_coupon_user ("
                + " coupon_id,"
                + " beid,"
                + " uid,"
                + " begin_date,"
                + " end_date, "
                + " source_order_id "
                + ") "
                + "VALUES "
                + "("
                + " ?,"
                + " ?,"
                + " ?,"
                + " ?,"
                + " ?, "
                + " ? "
                + ")");
        List<Object> params = new ArrayList<>();
        params.add(couponConfigId);
        params.add(beid);
        params.add(userId);
        Date date = new Date();
        // 开始时间
        params.add(DateUtil.getDateFormat(date, DateUtil.Y_M_D_PATTERN));
        // 结束时间
        params.add(DateUtil.getDateFormat(DateUtils.addDays(date, validDay), DateUtil.Y_M_D_PATTERN));
        params.add(sourceOrderId);

        mysqlRequestDTO.setParams(params);
        mysqlRequestDTO.setPhoneNumber(phoneNumber);
        mysqlRequestDTO.setProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);

        // 保存用户优惠券
        LOGGER.info("start save user coupon param:{}", JSON.toJSONString(mysqlRequestDTO));
        CommonResponse<Integer> response = mysqlApi.insert(mysqlRequestDTO);
        LOGGER.info("end save user coupon param:{}, response:{}", JSON.toJSONString(mysqlRequestDTO), JSON.toJSONString(response));
    }

    @Override
    public void usedCoupon(Integer orderId, Integer couponId, String phoneNumber) {
        MysqlRequestDTO mysqlRequestDTO = new MysqlRequestDTO();
        mysqlRequestDTO.setSql("UPDATE t_coupon_user "
                + "SET "
                + "used_time = ?,"
                + "used_order_id = ?,"
                + "used = ? "
                + "WHERE "
                + "id = ?");
        List<Object> params = new ArrayList<>();
        params.add(new Date());
        params.add(orderId);
        params.add(CouponUsedStatusEnum.ALREADY_USED.getStatus());
        params.add(couponId);
        mysqlRequestDTO.setParams(params);
        mysqlRequestDTO.setPhoneNumber(phoneNumber);
        mysqlRequestDTO.setProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);

        // 修改优惠券状态
        LOGGER.info("start used coupon param:{}", JSON.toJSONString(params));
        CommonResponse<Integer> response = mysqlApi.update(mysqlRequestDTO);
        LOGGER.info("end used coupon param:{}, response:{}", JSON.toJSONString(params), JSON.toJSONString(response));
    }

    @Override
    public void backUsedCoupon(Integer couponId, String phoneNumber) {
        MysqlRequestDTO mysqlRequestDTO = new MysqlRequestDTO();
        mysqlRequestDTO.setSql("UPDATE t_coupon_user "
                + "SET "
                + "used_time = '0000-00-00 00:00:00',"
                + "used_order_id = 0,"
                + "used = ? "
                + "WHERE "
                + "id = ?");
        List<Object> params = new ArrayList<>();
        params.add(CouponUsedStatusEnum.NOT_USED.getStatus());
        params.add(couponId);
        mysqlRequestDTO.setParams(params);
        mysqlRequestDTO.setPhoneNumber(phoneNumber);
        mysqlRequestDTO.setProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);

        // 修改优惠券状态
        LOGGER.info("start back used coupon param:{}", JSON.toJSONString(params));
        CommonResponse<Integer> response = mysqlApi.update(mysqlRequestDTO);
        LOGGER.info("end back used coupon param:{}, response:{}", JSON.toJSONString(params), JSON.toJSONString(response));
    }
}
