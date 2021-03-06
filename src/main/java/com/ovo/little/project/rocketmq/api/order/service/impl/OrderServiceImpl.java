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
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
     * ??????
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    /**
     * TODO ?????????????????????????????? ???????????????????????????rpc?????? ?????????????????????????????????
     */
    @Autowired
    private HotelRoomService hotelRoomService;

    /**
     * TODO ???????????????rpc???????????? ??????????????????????????????????????????
     */
    @Autowired
    private CouponService couponService;

    /**
     * ??????????????????????????????id
     */
    @Value("${order.finished.couponId}")
    private Integer orderFinishedCouponId;

    /**
     * ??????????????????????????????????????????
     */
    @Value("${order.finished.coupon.day}")
    private Integer orderFinishedCouponDay;

    @Autowired
    private OrderEventInformManager orderEventInformManager;

    /**
     * ????????????????????????topic
     */
    @Value("${rocketmq.order.finished.topic}")
    private String orderFinishedTopic;

    /**
     * ???????????? ??????????????????????????????
     */
    @Autowired
    @Qualifier(value = "orderFinishedTransactionMqProducer")
    private TransactionMQProducer orderFinishedTransactionMqProducer;


    /**
     * mysql dubbo??????
     */
    @Reference(version = "1.0.0",
            interfaceClass = MysqlApi.class,
            cluster = "failfast")
    private MysqlApi mysqlApi;

    /**
     * redis dubbo??????
     */
    @Reference(version = "1.0.0",
            interfaceClass = RedisApi.class,
            cluster = "failfast")
    private RedisApi redisApi;

    @Override
    public CommonResponse<CreateOrderResponseDTO> createOrder(OrderInfoDTO orderInfoDTO) {
        // TODO 1.???????????? ????????????????????????????????????????????? ????????????????????????

        // TODO ???????????????????????????????????????????????????????????????????????????
        // ??????????????????
        this.saveOrderInfo(orderInfoDTO);

        // ????????????????????????
        this.saveOrderItemInfo(orderInfoDTO);

        // ??????????????????????????????????????????
        couponService.usedCoupon(orderInfoDTO.getId(), orderInfoDTO.getCouponId(), orderInfoDTO.getPhoneNumber());

        // ?????????????????????mq???
        orderEventInformManager.informCreateOrderEvent(orderInfoDTO);

        CreateOrderResponseDTO createOrderResponseDTO = new CreateOrderResponseDTO();
        createOrderResponseDTO.setOrderNo(orderInfoDTO.getOrderNo());
        createOrderResponseDTO.setOrderId(orderInfoDTO.getId());
        return CommonResponse.success(createOrderResponseDTO);
    }

    @Override
    public CommonResponse cancelOrder(String orderNo, String phoneNumber) {
        // ???????????????????????????????????????
        // TODO ???????????????????????????????????????????????????????????????????????????
        OrderInfoDTO orderInfo = this.getOrderInfo(orderNo, phoneNumber);

        // TODO ??????????????????????????????????????????????????????
        if (!Objects.equals(orderInfo.getStatus(), OrderStatusEnum.WAITING_FOR_PAY.getStatus())) {
            throw new BusinessException("???????????????????????????????????????,?????????:" + orderNo);
        }

        // ??????????????????
        long cancelTime = System.currentTimeMillis() / 1000;
        this.updateOrderStatusAndCancelTime(orderNo, cancelTime, phoneNumber);

        // ????????????????????????????????? ?????????????????????
        if (!Objects.isNull(orderInfo.getCouponId())) {
            // ???????????????
            couponService.backUsedCoupon(orderInfo.getCouponId(), phoneNumber);
        }

        // ??????????????????
        orderInfo.setCancelTime((int) cancelTime);
        orderEventInformManager.informCancelOrderEvent(orderInfo);

        return CommonResponse.success();
    }

    @Override
    public Integer informPayOrderSuccessed(String orderNo, String phoneNumber) {
        OrderInfoDTO orderInfo = null;
        try {
            // ?????????????????????????????????????????????
            CommonResponse<Boolean> commonResponse = redisApi.lock(ORDER_LOCK_KEY_PREFIX + orderNo,
                    orderNo,
                    10L,
                    TimeUnit.SECONDS,
                    phoneNumber,
                    LittleProjectTypeEnum.ROCKETMQ);
            if (Objects.equals(commonResponse.getCode(), ErrorCodeEnum.SUCCESS.getCode())
                    && Objects.equals(commonResponse.getData(), Boolean.TRUE)) {
                // ????????????????????????

                // ????????????
                orderInfo = this.getOrderInfo(orderNo, phoneNumber);

                // ????????????????????????????????????
                if (!Objects.equals(OrderStatusEnum.WAITING_FOR_PAY.getStatus(), orderInfo.getStatus())) {
                    throw new BusinessException("???????????????????????????????????????????????????,????????????" + orderNo);
                }

                // ?????????????????????
                long payTime = System.currentTimeMillis() / 1000;
                this.updateOrderStatusAndPayTime(orderNo, payTime, phoneNumber);

                // TODO ?????????????????? ??????????????????????????????????????????????????????

                // ??????????????????
                orderInfo.setPayTime((int) payTime);
                orderEventInformManager.informPayOrderEvent(orderInfo);

            }
        } finally {
            // ?????????
            redisApi.unlock(ORDER_LOCK_KEY_PREFIX + orderNo,
                    orderNo,
                    phoneNumber,
                    LittleProjectTypeEnum.ROCKETMQ);
        }
        return orderInfo != null ? orderInfo.getId() : null;
    }

    @Override
    public void informConfirmOrder(String orderNo, String phoneNumber) {
        // TODO ???????????????????????????????????????????????????????????????????????????
        // ?????????????????????
        this.updateOrderStatus(orderNo, OrderStatusEnum.CONFIRM, phoneNumber);

        // ??????????????????
        orderEventInformManager.informConfirmOrderEvent(this.getOrderInfo(orderNo, phoneNumber));
    }

    @Override
    public void informFinishedOrder(String orderNo, String phoneNumber) {
        // ????????????
        OrderInfoDTO orderInfo = this.getOrderInfo(orderNo, phoneNumber);
        Message msg = new Message(orderFinishedTopic, JSON.toJSONString(orderInfo).getBytes(StandardCharsets.UTF_8));
        try {
            // ??????prepare??????
            orderFinishedTransactionMqProducer.sendMessageInTransaction(msg, null);

        } catch (MQClientException e) {
            LOGGER.info("finished order send half message fail error:{}", e);
            // TODO ????????????????????????????????????????????????
        }

    }

    /**
     * ??????????????????
     *
     * @param orderNo         ?????????
     * @param orderStatusEnum ????????????
     * @param phoneNumber     ?????????
     */
    @Override
    public void updateOrderStatus(String orderNo, OrderStatusEnum orderStatusEnum, String phoneNumber) {
        MysqlRequestDTO mysqlRequestDTO = new MysqlRequestDTO();
        mysqlRequestDTO.setSql("update t_shop_order set status = ? where ordersn = ?");
        List<Object> params = new ArrayList<>();
        params.add(orderStatusEnum.getStatus());
        params.add(orderNo);
        mysqlRequestDTO.setParams(params);
        mysqlRequestDTO.setPhoneNumber(phoneNumber);
        mysqlRequestDTO.setProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);

        LOGGER.info("start update order status param:{}", JSON.toJSONString(params));
        CommonResponse<Integer> response = mysqlApi.update(mysqlRequestDTO);
        LOGGER.info("end update order status param:{}, response:{}", JSON.toJSONString(params), JSON.toJSONString(response));
    }

    @Override
    public Integer getOrderStatus(String orderNo, String phoneNumber) {
        MysqlRequestDTO mysqlRequestDTO = new MysqlRequestDTO();
        mysqlRequestDTO.setSql("select status from  t_shop_order where ordersn = ?");
        List<Object> params = new ArrayList<>();
        params.add(orderNo);
        mysqlRequestDTO.setParams(params);
        mysqlRequestDTO.setPhoneNumber(phoneNumber);
        mysqlRequestDTO.setProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);

        LOGGER.info("start select order status param:{}", JSON.toJSONString(params));
        CommonResponse<Integer> response = mysqlApi.update(mysqlRequestDTO);
        LOGGER.info("end select order status param:{}, response:{}", JSON.toJSONString(params), JSON.toJSONString(response));
        if (Objects.equals(response.getCode(), ErrorCodeEnum.SUCCESS.getCode())) {
            return response.getData();
        }
        return null;
    }

    /**
     * ?????????????????????????????????
     *
     * @param orderNo     ?????????
     * @param payTime     ????????????
     * @param phoneNumber ?????????
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
     * ?????????????????????????????????  ?????????????????????id??????????????????
     *
     * @param orderNo     ?????????
     * @param cancelTime  ????????????
     * @param phoneNumber ?????????
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
     * ??????????????????
     *
     * @param orderNo ????????????
     * @return ????????????
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
                // ????????????
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

                // ??????????????????
                orderInfoDTO.setOrderItem(this.getOrderItem(orderInfoDTO.getId(), phoneNumber));
                return orderInfoDTO;
            }
        }
        throw new BusinessException(OrderBusinessErrorCodeEnum.ORDER_NO_EXIST.getMsg());
    }

    /**
     * ????????????????????????
     *
     * @param orderId     ?????????
     * @param phoneNumber ?????????
     * @return ????????????
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
                // ????????????
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
     * ????????????????????????
     *
     * @param orderInfoDTO ????????????
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

        // ??????????????????
        LOGGER.info("start save orderItem param:{}", JSON.toJSONString(mysqlRequestDTO));
        CommonResponse<Integer> commonResponse = mysqlApi.insert(mysqlRequestDTO);
        LOGGER.info("end save orderItem param:{}, response:{}", JSON.toJSONString(mysqlRequestDTO), JSON.toJSONString(commonResponse));
        if (!Objects.equals(commonResponse.getCode(), ErrorCodeEnum.SUCCESS.getCode())) {
            // ????????????????????????
            throw new BusinessException(OrderBusinessErrorCodeEnum.CREATE_ORDER_ITEM_FAIL.getMsg());
        }
    }

    /**
     * ????????????
     *
     * @param orderInfoDTO ????????????
     */
    private void saveOrderInfo(OrderInfoDTO orderInfoDTO) {
        String phoneNumber = orderInfoDTO.getPhoneNumber();
        // ?????????
        orderInfoDTO.setOrderNo(UUID.randomUUID().toString().replace(StringPoolConstant.DASH, StringPoolConstant.EMPTY));
        orderInfoDTO.setStatus(OrderStatusEnum.WAITING_FOR_PAY.getStatus());
        orderInfoDTO.setPhoneNumber(phoneNumber);
        // ????????????
        CommonResponse<HotelRoom> commonResponse = hotelRoomService.getRoomById(orderInfoDTO.getRoomId().longValue(), phoneNumber);
        HotelRoom hotelRoom = commonResponse.getData();
        // ???????????????
        orderInfoDTO.setTotalPrice(hotelRoom.getProductprice().multiply(BigDecimal.valueOf(orderInfoDTO.getTotal())));
        // ????????????????????????
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
        // ??????
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
            // ??????????????????
            // ???????????????????????????id
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
            // ??????????????????
            LOGGER.error("save order fail error message:{}", JSON.toJSONString(insertOrderResponse));
            throw new BusinessException(OrderBusinessErrorCodeEnum.CREATE_ORDER_FAIL.getMsg());
        }
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param hotelRoom    ??????
     * @param orderInfoDTO ????????????
     * @return ????????????
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
