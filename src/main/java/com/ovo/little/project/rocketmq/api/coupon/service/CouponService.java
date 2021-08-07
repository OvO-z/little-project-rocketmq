package com.ovo.little.project.rocketmq.api.coupon.service;

/**
 * @author QAQ
 * @date 2021/8/6
 */

public interface CouponService {

    /**
     * 分发第一次登陆的优惠券
     *
     * @param beid           业务id
     * @param userId         用户id
     * @param couponConfigId 下发优惠券配置表id
     * @param validDay       有效天数
     * @param sourceOrderId  优惠券来源订单id
     * @param phoneNumber    手机号
     */
    void distributeCoupon(Integer beid,
                          Integer userId,
                          Integer couponConfigId,
                          Integer validDay,
                          Integer sourceOrderId, String phoneNumber);
    /**
     * 使用优惠券
     *
     * @param orderId     订单id
     * @param couponId    优惠券id
     * @param phoneNumber 用户手机号
     */
    void usedCoupon(Integer orderId, Integer couponId, String phoneNumber);

    /**
     * 退回已使用的优惠券
     *
     * @param couponId    优惠券id
     * @param phoneNumber 手机号
     */
    void backUsedCoupon(Integer couponId, String phoneNumber);
}
