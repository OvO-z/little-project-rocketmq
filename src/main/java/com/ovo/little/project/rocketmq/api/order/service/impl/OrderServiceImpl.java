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
