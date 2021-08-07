package com.ovo.little.project.rocketmq.api.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.ovo.little.project.rocketmq.api.coupon.service.CouponService;
import com.ovo.little.project.rocketmq.api.hotel.dto.HotelRoom;
import com.ovo.little.project.rocketmq.api.hotel.service.HotelRoomService;
import com.ovo.little.project.rocketmq.api.order.dto.CreateOrderResponseDTO;
import com.ovo.little.project.rocketmq.api.order.dto.OrderInfoDTO;
import com.ovo.little.project.rocketmq.api.order.dto.OrderItemDTO;
import com.ovo.little.project.rocketmq.api.order.enums.OrderBusinessErrorCodeEnum;
import com.ovo.little.project.rocketmq.api.order.enums.OrderStatusEnum;
import com.ovo.little.project.rocketmq.api.order.service.OrderEventInformManager;
import com.ovo.little.project.rocketmq.api.order.service.OrderService;
import com.ovo.little.project.rocketmq.common.constant.StringPoolConstant;
import com.ovo.little.project.rocketmq.common.exception.BusinessException;
import com.ruyuan.little.project.common.dto.CommonResponse;
import com.ruyuan.little.project.common.enums.ErrorCodeEnum;
import com.ruyuan.little.project.common.enums.LittleProjectTypeEnum;
import com.ruyuan.little.project.mysql.api.MysqlApi;
import com.ruyuan.little.project.mysql.dto.MysqlRequestDTO;
import com.ruyuan.little.project.redis.api.RedisApi;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.ovo.little.project.rocketmq.common.constant.RedisKeyConstant.ORDER_LOCK_KEY_PREFIX;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Service
public class OrderServiceImpl implements OrderService {

    /**
     * 日志
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    /**
     * TODO 正常获取酒店房间数据 应该调用酒店服务的rpc接口 由于没分模块则本地调用
     */
    @Autowired
    private HotelRoomService hotelRoomService;

    /**
     * TODO 本质上是走rpc远程调用 这里由于没拆分模块即本地调用
     */
    @Autowired
    private CouponService couponService;

    @Autowired
    private OrderEventInformManager orderEventInformManager;

    /**
     * mysql dubbo服务
     */
    @Reference(version = "1.0.0",
            interfaceClass = MysqlApi.class,
            cluster = "failfast")
    private MysqlApi mysqlApi;

    /**
     * redis dubbo服务
     */
    @Reference(version = "1.0.0",
            interfaceClass = RedisApi.class,
            cluster = "failfast")
    private RedisApi redisApi;

    @Override
    public CommonResponse<CreateOrderResponseDTO> createOrder(OrderInfoDTO orderInfoDTO) {
        // TODO 1.校验库存 由于我们后台系统配置为不减库存 这里库存不做校验

        // TODO 可以通过状态模式来校验订单的流转和保存订单操作日志
        // 保存订单数据
        this.saveOrderInfo(orderInfoDTO);

        // 保存订单商品数据
        this.saveOrderItemInfo(orderInfoDTO);

        // 调用优惠券服务更新优惠券状态
        couponService.usedCoupon(orderInfoDTO.getId(), orderInfoDTO.getCouponId(), orderInfoDTO.getPhoneNumber());

        // 发送订单消息到mq中
        orderEventInformManager.informCreateOrderEvent(orderInfoDTO);

        CreateOrderResponseDTO createOrderResponseDTO = new CreateOrderResponseDTO();
        createOrderResponseDTO.setOrderNo(orderInfoDTO.getOrderNo());
        createOrderResponseDTO.setOrderId(orderInfoDTO.getId());
        return CommonResponse.success(createOrderResponseDTO);
    }

    @Override
    public CommonResponse cancelOrder(String orderNo, String phoneNumber) {
        // 校验订单的状态是否可以取消
        // TODO 可以通过状态模式来优化订单的流转和保存订单操作日志
        OrderInfoDTO orderInfo = this.getOrderInfo(orderNo, phoneNumber);

        // TODO 检查时并发问题可以通过分布式锁来解决
        if (!Objects.equals(orderInfo.getStatus(), OrderStatusEnum.WAITING_FOR_PAY.getStatus())) {
            throw new BusinessException("订单不是未支付状态不能取消,订单号:" + orderNo);
        }

        // 更新订单状态
        long cancelTime = System.currentTimeMillis() / 1000;
        this.updateOrderStatusAndCancelTime(orderNo, cancelTime, phoneNumber);

        // 判断订单是否使用优惠券 进行优惠券退回
        if (!Objects.isNull(orderInfo.getCouponId())) {
            // 退回优惠券
            couponService.backUsedCoupon(orderInfo.getCouponId(), phoneNumber);
        }

        // 发送通知消息
        orderInfo.setCancelTime((int) cancelTime);
        orderEventInformManager.informCancelOrderEvent(orderInfo);

        return CommonResponse.success();
    }

    @Override
    public Integer informPayOrderSuccessed(String orderNo, String phoneNumber) {
        OrderInfoDTO orderInfo = null;
        try {
            // 获取订单分布式锁防止订单已取消
            CommonResponse<Boolean> commonResponse = redisApi.lock(ORDER_LOCK_KEY_PREFIX + orderNo,
                    orderNo,
                    10L,
                    TimeUnit.SECONDS,
                    phoneNumber,
                    LittleProjectTypeEnum.ROCKETMQ);
            if (Objects.equals(commonResponse.getCode(), ErrorCodeEnum.SUCCESS.getCode())
                    && Objects.equals(commonResponse.getData(), Boolean.TRUE)) {
                // 获取分布式锁成功

                // 订单信息
                orderInfo = this.getOrderInfo(orderNo, phoneNumber);

                // 判断订单状态是否是待支付
                if (!Objects.equals(OrderStatusEnum.WAITING_FOR_PAY.getStatus(), orderInfo.getStatus())) {
                    throw new BusinessException("订单状态不是待支付该订单不可以支付,订单号：" + orderNo);
                }

                // 修改订单的状态
                long payTime = System.currentTimeMillis() / 1000;
                this.updateOrderStatusAndPayTime(orderNo, payTime, phoneNumber);

                // TODO 实际扣减库存 这里酒店数据为下单不扣减库存则不扣减

                // 发送支付通知
                orderInfo.setPayTime((int) payTime);
                orderEventInformManager.informPayOrderEvent(orderInfo);

            }
        } finally {
            // 释放锁
            redisApi.unlock(ORDER_LOCK_KEY_PREFIX + orderNo,
                    orderNo,
                    phoneNumber,
                    LittleProjectTypeEnum.ROCKETMQ);
        }
        return orderInfo != null ? orderInfo.getId() : null;
    }

    /**
     * 更新订单状态和支付时间
     *
     * @param orderNo     订单号
     * @param payTime     支付时间
     * @param phoneNumber 手机号
     */
    private void updateOrderStatusAndPayTime(String orderNo, long payTime, String phoneNumber) {
        MysqlRequestDTO mysqlRequestDTO = new MysqlRequestDTO();
        mysqlRequestDTO.setSql("update t_shop_order set status = ?,paytime = ? where ordersn = ?");
        List<Object> params = new ArrayList<>();
        params.add(OrderStatusEnum.WAITING_FOR_LIVE.getStatus());
        params.add(payTime);
        params.add(orderNo);
        mysqlRequestDTO.setParams(params);
        mysqlRequestDTO.setPhoneNumber(phoneNumber);
        mysqlRequestDTO.setProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);

        LOGGER.info("start update order status param:{}", JSON.toJSONString(params));
        CommonResponse<Integer> response = mysqlApi.update(mysqlRequestDTO);
        LOGGER.info("end update order status param:{}, response:{}", JSON.toJSONString(params), JSON.toJSONString(response));
    }

    /**
     * 更新订单状态和取消时间  默认更新优惠券id和优惠券金额
     *
     * @param orderNo     订单号
     * @param cancelTime  取消时间
     * @param phoneNumber 手机号
     */
    private void updateOrderStatusAndCancelTime(String orderNo, long cancelTime, String phoneNumber) {
        MysqlRequestDTO mysqlRequestDTO = new MysqlRequestDTO();
        mysqlRequestDTO.setSql("update t_shop_order set status = ?,cancel_time = ?,coupon_id = null,coupon_money = null where ordersn = ?");
        List<Object> params = new ArrayList<>();
        params.add(OrderStatusEnum.CANCELED.getStatus());
        params.add(cancelTime);
        params.add(orderNo);
        mysqlRequestDTO.setParams(params);
        mysqlRequestDTO.setPhoneNumber(phoneNumber);
        mysqlRequestDTO.setProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);

        LOGGER.info("start update order status param:{}", JSON.toJSONString(params));
        CommonResponse<Integer> response = mysqlApi.update(mysqlRequestDTO);
        LOGGER.info("end update order status param:{}, response:{}", JSON.toJSONString(params), JSON.toJSONString(response));
    }

    /**
     * 查询订单详情
     *
     * @param orderNo 订单编号
     * @return 订单信息
     */
    private OrderInfoDTO getOrderInfo(String orderNo, String phoneNumber) {
        MysqlRequestDTO orderInfoRequestDTO = new MysqlRequestDTO();
        orderInfoRequestDTO.setSql("select  "
                + "id, "
                + "address_realname , "
                + "status , "
                + "remark , "
                + "createtime , "
                + "uid , "
                + "beid , "
                + "address_mobile, "
                + "coupon_id "
                + " from t_shop_order "
                + " where "
                + "ordersn = ?");
        List<Object> params = new ArrayList<>();
        params.add(orderNo);
        orderInfoRequestDTO.setParams(params);
        orderInfoRequestDTO.setPhoneNumber(phoneNumber);
        orderInfoRequestDTO.setProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);
        LOGGER.info("start query order info param:{}", JSON.toJSONString(orderInfoRequestDTO));
        CommonResponse<List<Map<String, Object>>> orderInfoResponse = mysqlApi.query(orderInfoRequestDTO);
        LOGGER.info("end query order info param:{}, response:{}", JSON.toJSONString(orderInfoRequestDTO), JSON.toJSONString(orderInfoResponse));
        if (Objects.equals(orderInfoResponse.getCode(), ErrorCodeEnum.SUCCESS.getCode())) {
            if (!CollectionUtils.isEmpty(orderInfoResponse.getData())) {
                Map<String, Object> orderMap = orderInfoResponse.getData().get(0);
                // 订单信息
                OrderInfoDTO orderInfoDTO = new OrderInfoDTO();
                orderInfoDTO.setId(Integer.valueOf(String.valueOf(orderMap.get("id"))));
                orderInfoDTO.setPhoneNumber(phoneNumber);
                orderInfoDTO.setName(String.valueOf(orderMap.get("address_mobile")));
                orderInfoDTO.setRemark(String.valueOf(orderMap.get("remark")));
                orderInfoDTO.setCreateTime(Integer.valueOf(String.valueOf(orderMap.get("createtime"))));
                orderInfoDTO.setStatus(Integer.valueOf(String.valueOf(orderMap.get("status"))));
                Object couponId = orderMap.get("coupon_id");
                if (!Objects.isNull(couponId)) {
                    orderInfoDTO.setCouponId(Integer.valueOf(String.valueOf(couponId)));
                }
                orderInfoDTO.setUserId(Integer.valueOf(String.valueOf(orderMap.get("uid"))));
                orderInfoDTO.setBeid(Integer.valueOf(String.valueOf(orderMap.get("beid"))));
                orderInfoDTO.setOrderNo(orderNo);

                // 查询订单房间
                orderInfoDTO.setOrderItem(this.getOrderItem(orderInfoDTO.getId(), phoneNumber));
                return orderInfoDTO;
            }
        }
        throw new BusinessException(OrderBusinessErrorCodeEnum.ORDER_NO_EXIST.getMsg());
    }

    /**
     * 查询订单房间信息
     *
     * @param orderId     订单号
     * @param phoneNumber 手机号
     * @return 房间信息
     */
    private OrderItemDTO getOrderItem(Integer orderId, String phoneNumber) {
        MysqlRequestDTO orderItemRequestDTO = new MysqlRequestDTO();
        orderItemRequestDTO.setSql("select  "
                + "goodsid , "
                + "title , "
                + "price , "
                + "total  "
                + " from t_shop_order_goods "
                + " where orderid = ?");
        orderItemRequestDTO.setParams(Collections.singletonList(orderId));
        orderItemRequestDTO.setPhoneNumber(phoneNumber);
        orderItemRequestDTO.setProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);

        LOGGER.info("start query order item param:{}", JSON.toJSONString(orderItemRequestDTO));
        CommonResponse<List<Map<String, Object>>> orderItemResponse = mysqlApi.query(orderItemRequestDTO);
        LOGGER.info("end query order item param:{}, response:{}", JSON.toJSONString(orderItemRequestDTO), JSON.toJSONString(orderItemResponse));

        if (Objects.equals(orderItemResponse.getCode(), ErrorCodeEnum.SUCCESS.getCode())) {
            if (!CollectionUtils.isEmpty(orderItemResponse.getData())) {
                Map<String, Object> orderItemMap = orderItemResponse.getData().get(0);
                // 订单信息
                OrderItemDTO orderItemDTO = new OrderItemDTO();
                orderItemDTO.setRoomId(Integer.valueOf(String.valueOf(orderItemMap.get("goodsid"))));
                orderItemDTO.setTitle(String.valueOf(orderItemMap.get("title")));
                orderItemDTO.setPrice((BigDecimal) orderItemMap.get("price"));
                orderItemDTO.setTotal(Integer.valueOf(String.valueOf(orderItemMap.get("total"))));

                return orderItemDTO;
            }
        }
        return null;
    }

    /**
     * 保存订单商品数据
     *
     * @param orderInfoDTO 订单信息
     */
    private void saveOrderItemInfo(OrderInfoDTO orderInfoDTO) {
        OrderItemDTO orderItemDTO = orderInfoDTO.getOrderItem();
        String phoneNumber = orderInfoDTO.getPhoneNumber();
        MysqlRequestDTO mysqlRequestDTO = new MysqlRequestDTO();
        mysqlRequestDTO.setSql("insert into "
                + " t_shop_order_goods"
                + "("
                + "thumb, "
                + "beid, "
                + "orderid, "
                + "goodsId, "
                + "title, "
                + "price, "
                + "total, "
                + "order_dates, "
                + "description, "
                + "createtime "
                + ")"
                + "values( "
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
        List<Object> params = new ArrayList<>();
        params.add(orderItemDTO.getThumb());
        params.add(orderItemDTO.getBeid());
        params.add(orderInfoDTO.getId());
        params.add(orderItemDTO.getRoomId());
        params.add(orderItemDTO.getTitle());
        params.add(orderItemDTO.getPrice());
        params.add(orderItemDTO.getTotal());
        params.add(JSON.toJSONString(Collections.singletonList(orderItemDTO.getOrderDates())));
        params.add(orderItemDTO.getDescription());
        params.add(orderInfoDTO.getCreateTime());
        mysqlRequestDTO.setParams(params);
        mysqlRequestDTO.setPhoneNumber(phoneNumber);
        mysqlRequestDTO.setProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);

        // 保存订单商品
        LOGGER.info("start save orderItem param:{}", JSON.toJSONString(mysqlRequestDTO));
        CommonResponse<Integer> commonResponse = mysqlApi.insert(mysqlRequestDTO);
        LOGGER.info("end save orderItem param:{}, response:{}", JSON.toJSONString(mysqlRequestDTO), JSON.toJSONString(commonResponse));
        if (!Objects.equals(commonResponse.getCode(), ErrorCodeEnum.SUCCESS.getCode())) {
            // 保存订单商品失败
            throw new BusinessException(OrderBusinessErrorCodeEnum.CREATE_ORDER_ITEM_FAIL.getMsg());
        }
    }

    /**
     * 保存订单
     *
     * @param orderInfoDTO 订单数据
     */
    private void saveOrderInfo(OrderInfoDTO orderInfoDTO) {
        String phoneNumber = orderInfoDTO.getPhoneNumber();
        // 订单号
        orderInfoDTO.setOrderNo(UUID.randomUUID().toString().replace(StringPoolConstant.DASH, StringPoolConstant.EMPTY));
        orderInfoDTO.setStatus(OrderStatusEnum.WAITING_FOR_PAY.getStatus());
        orderInfoDTO.setPhoneNumber(phoneNumber);
        // 房间数据
        CommonResponse<HotelRoom> commonResponse = hotelRoomService.getRoomById(orderInfoDTO.getRoomId().longValue(), phoneNumber);
        HotelRoom hotelRoom = commonResponse.getData();
        // 订单总金额
        orderInfoDTO.setTotalPrice(hotelRoom.getProductprice().multiply(BigDecimal.valueOf(orderInfoDTO.getTotal())));
        // 构建订单商品对象
        orderInfoDTO.setOrderItem(this.builderOrderItem(hotelRoom, orderInfoDTO));

        MysqlRequestDTO mysqlRequestDTO = new MysqlRequestDTO();
        mysqlRequestDTO.setSql("insert into "
                + " t_shop_order"
                + "("
                + "beid, "
                + "openid, "
                + "ordersn, "
                + "price, "
                + "status, "
                + "remark, "
                + "address_realname, "
                + "address_mobile, "
                + "desk_num, "
                + "goods_total_price, "
                + "createtime, "
                + "updatetime, "
                + "coupon_id, "
                + "coupon_money, "
                + "uid "
                + ") "
                + "values( "
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
                + "?, "
                + "?, "
                + "?, "
                + "?, "
                + "?"
                + ")");
        ArrayList<Object> params = new ArrayList<>();
        params.add(orderInfoDTO.getBeid());
        params.add(orderInfoDTO.getOpenId());
        params.add(orderInfoDTO.getOrderNo());
        params.add(orderInfoDTO.getTotalPrice());
        params.add(orderInfoDTO.getStatus());
        params.add(orderInfoDTO.getRemark());
        params.add(orderInfoDTO.getName());
        params.add(phoneNumber);
        params.add(orderInfoDTO.getHotelId());
        params.add(orderInfoDTO.getTotalPrice());
        // 时间
        long unixTime = System.currentTimeMillis() / 1000;
        orderInfoDTO.setCreateTime((int) unixTime);
        params.add(unixTime);
        params.add(unixTime);
        params.add(orderInfoDTO.getCouponId());
        params.add(orderInfoDTO.getCouponMoney());
        params.add(orderInfoDTO.getUserId());

        mysqlRequestDTO.setParams(params);
        mysqlRequestDTO.setPhoneNumber(phoneNumber);
        mysqlRequestDTO.setProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);

        LOGGER.info("start save order param:{}", JSON.toJSONString(mysqlRequestDTO));
        CommonResponse<Integer> insertOrderResponse = mysqlApi.insert(mysqlRequestDTO);
        LOGGER.info("end save order param:{}, response:{}", JSON.toJSONString(mysqlRequestDTO), JSON.toJSONString(insertOrderResponse));
        if (Objects.equals(ErrorCodeEnum.SUCCESS.getCode(), insertOrderResponse.getCode())) {
            // 保存订单成功
            // 根据订单号查询订单id
            MysqlRequestDTO queryOrderIdRequestDTO = new MysqlRequestDTO();
            queryOrderIdRequestDTO.setProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);
            queryOrderIdRequestDTO.setPhoneNumber(phoneNumber);
            queryOrderIdRequestDTO.setParams(Collections.singletonList(orderInfoDTO.getOrderNo()));
            queryOrderIdRequestDTO.setSql("select id from t_shop_order where ordersn = ?");
            CommonResponse<List<Map<String, Object>>> response = mysqlApi.query(queryOrderIdRequestDTO);
            if (Objects.equals(response.getCode(), ErrorCodeEnum.SUCCESS.getCode())) {
                List<Map<String, Object>> mapList = response.getData();
                if (!CollectionUtils.isEmpty(mapList)) {
                    orderInfoDTO.setId(Integer.valueOf(String.valueOf(mapList.get(0).get("id"))));
                }
            }
        } else {
            // 订单保存失败
            LOGGER.error("save order fail error message:{}", JSON.toJSONString(insertOrderResponse));
            throw new BusinessException(OrderBusinessErrorCodeEnum.CREATE_ORDER_FAIL.getMsg());
        }
    }

    /**
     * 根据房间信息构建订单商品信息
     *
     * @param hotelRoom    房间
     * @param orderInfoDTO 订单信息
     * @return 订单商品
     */
    private OrderItemDTO builderOrderItem(HotelRoom hotelRoom, OrderInfoDTO orderInfoDTO) {
        OrderItemDTO orderItemDTO = new OrderItemDTO();
        orderItemDTO.setBeid(orderInfoDTO.getBeid());
        orderItemDTO.setDescription(JSON.toJSONString(hotelRoom.getRoomDescription()));
        orderItemDTO.setOrderDates(orderInfoDTO.getEndDate());
        orderItemDTO.setRoomId(hotelRoom.getId().intValue());
        orderItemDTO.setThumb(hotelRoom.getThumbUrl());
        orderItemDTO.setTitle(hotelRoom.getTitle());
        orderItemDTO.setTotal(orderInfoDTO.getTotal());
        orderItemDTO.setPrice(hotelRoom.getProductprice());
        return orderItemDTO;
    }
}
