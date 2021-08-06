package com.ovo.little.project.rocketmq.admin.service.impl;

import com.alibaba.fastjson.JSON;
import com.ovo.little.project.rocketmq.admin.dto.AdminHotelRoom;
import com.ovo.little.project.rocketmq.admin.dto.AdminHotelRoomMessage;
import com.ovo.little.project.rocketmq.admin.dto.AdminRoomDescription;
import com.ovo.little.project.rocketmq.admin.dto.AdminRoomPicture;
import com.ovo.little.project.rocketmq.admin.service.AdminRoomService;
import com.ovo.little.project.rocketmq.common.constant.RedisKeyConstant;
import com.ruyuan.little.project.common.dto.CommonResponse;
import com.ruyuan.little.project.common.enums.ErrorCodeEnum;
import com.ruyuan.little.project.common.enums.LittleProjectTypeEnum;
import com.ruyuan.little.project.mysql.api.MysqlApi;
import com.ruyuan.little.project.mysql.dto.MysqlRequestDTO;
import com.ruyuan.little.project.redis.api.RedisApi;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Service
public class AdminRoomServiceImpl implements AdminRoomService {

    private Logger LOGGER = LoggerFactory.getLogger(AdminRoomServiceImpl.class);

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

    /**
     * 房间管理mq producer
     */
    @Autowired
    @Qualifier(value = "hotelRoomMqProducer")
    private DefaultMQProducer hotelRoomMqProducer;

    /**
     * 酒店房间topic
     */
    @Value("${rocketmq.hotelRoom.topic}")
    private String hotelRoomTopic;

    @Override
    public CommonResponse add(AdminHotelRoom adminHotelRoom) {
        // TODO 房间数据mysql已经存储 这里只是请求转发过来写redis
        redisApi.set(RedisKeyConstant.HOTEL_ROOM_KEY_PREFIX + adminHotelRoom.getId(),
                JSON.toJSONString(adminHotelRoom),
                adminHotelRoom.getPhoneNumber(),
                LittleProjectTypeEnum.ROCKETMQ);
        LOGGER.info("add hotel room to redis cache roomId:{}", adminHotelRoom.getId());
        return CommonResponse.success();
    }

    @Override
    public CommonResponse update(AdminHotelRoom hotelRoom) {
        String phoneNumber = hotelRoom.getPhoneNumber();
        MysqlRequestDTO mysqlRequestDTO = new MysqlRequestDTO();
        mysqlRequestDTO.setProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);
        mysqlRequestDTO.setPhoneNumber(phoneNumber);
        mysqlRequestDTO.setSql("UPDATE t_shop_goods SET pcate = ?, title = ?, thumb_url = ?, productprice = ?, total = ?, totalcnf = ? WHERE id = ?");
        this.builderSqlParams(hotelRoom, mysqlRequestDTO);
        LOGGER.info("start update hotel room param:{}", JSON.toJSONString(mysqlRequestDTO));
        // 写mysql
        CommonResponse<Integer> response = mysqlApi.update(mysqlRequestDTO);
        LOGGER.info("end update hotel room param:{}, response:{}", JSON.toJSONString(mysqlRequestDTO), JSON.toJSONString(response));

        // db更新成功后，更新redis，发客房数据更新的消息
        if (Objects.equals(response.getCode(), ErrorCodeEnum.SUCCESS.getCode())) {
            Long roomId = hotelRoom.getId();

            // 查询更新后的数据
            AdminHotelRoom adminHotelRoom = this.getHotelRoomById(roomId, phoneNumber);

            LOGGER.info("update hotel room data success update redis cache key:{}", RedisKeyConstant.HOTEL_ROOM_KEY_PREFIX + roomId);
            // 写redis
            redisApi.set(RedisKeyConstant.HOTEL_ROOM_KEY_PREFIX + roomId, JSON.toJSONString(adminHotelRoom), phoneNumber, LittleProjectTypeEnum.ROCKETMQ);

            // 发客房数据更新的消息
            this.sendRoomUpdateMessage(phoneNumber, roomId);

        }
        return response;
    }

    /**
     * 发客房数据更新的消息到mq中
     *
     * @param phoneNumber 手机号
     * @param roomId      房间id
     */
    private void sendRoomUpdateMessage(String phoneNumber, Long roomId) {
        // 发送广播消息到mq中
        // 提供给小程序的api模块来消费消息后从redis中获取消息更新本地jvm内存
        Message message = new Message();
        message.setTopic(hotelRoomTopic);
        AdminHotelRoomMessage adminHotelRoomMessage = new AdminHotelRoomMessage();
        adminHotelRoomMessage.setRoomId(roomId);
        adminHotelRoomMessage.setPhoneNumber(phoneNumber);

        message.setBody(JSON.toJSONString(adminHotelRoomMessage).getBytes(StandardCharsets.UTF_8));
        try {
            LOGGER.info("start send hotel room update  message, param:{}", roomId);
            SendResult sendResult = hotelRoomMqProducer.send(message);
            LOGGER.info("end send hotel room update  message, param:{}, sendResult:{}", roomId, JSON.toJSONString(sendResult));
        } catch (Exception e) {
            LOGGER.error("send login success notify message fail, error message:{}", e);
        }
    }

    /**
     * 根据房间id查询房间内容
     *
     * @param id          房间id
     * @param phoneNumber 用户手机号
     * @return 房间信息
     */
    private AdminHotelRoom getHotelRoomById(Long id, String phoneNumber) {
        MysqlRequestDTO mysqlRequestDTO = new MysqlRequestDTO();
        mysqlRequestDTO.setSql("SELECT "
                + "id,"
                + "title, "
                + "pcate, "
                + "thumb_url, "
                + "description, "
                + "goods_banner, "
                + "marketprice, "
                + "productprice, "
                + "total,"
                + "createtime "
                + "FROM "
                + "t_shop_goods "
                + "WHERE "
                + "id = ?");
        mysqlRequestDTO.setPhoneNumber(phoneNumber);
        mysqlRequestDTO.setProjectTypeEnum(LittleProjectTypeEnum.ROCKETMQ);
        mysqlRequestDTO.setParams(Collections.singletonList(id));
        LOGGER.info("start query room detail param:{}", JSON.toJSONString(mysqlRequestDTO));
        CommonResponse<List<Map<String, Object>>> commonResponse = mysqlApi.query(mysqlRequestDTO);
        LOGGER.info("end query room detail param:{}, response:{}", JSON.toJSONString(mysqlRequestDTO), JSON.toJSONString(commonResponse));
        if (Objects.equals(commonResponse.getCode(), ErrorCodeEnum.SUCCESS.getCode())) {
            List<Map<String, Object>> mapList = commonResponse.getData();
            List<AdminHotelRoom> hotelRoomDetailList = mapList.stream().map(map -> {
                AdminHotelRoom detailDTO = new AdminHotelRoom();
                detailDTO.setId((Long) map.get("id"));
                detailDTO.setTitle(String.valueOf(map.get("title")));
                detailDTO.setPcate((Long) map.get("pcate"));
                detailDTO.setThumbUrl(String.valueOf(map.get("thumb_url")));
                detailDTO.setRoomDescription(JSON.parseObject(String.valueOf(map.get("description")), AdminRoomDescription.class));
                String goods_banner = String.valueOf(map.get("goods_banner"));
                List<AdminRoomPicture> adminRoomPictures = JSON.parseArray(goods_banner, AdminRoomPicture.class);
                detailDTO.setGoods_banner(adminRoomPictures);
                detailDTO.setMarketprice((BigDecimal) map.get("marketprice"));
                detailDTO.setProductprice((BigDecimal) map.get("productprice"));
                detailDTO.setTotal((Integer) map.get("total"));
                detailDTO.setCreatetime((Long) map.get("createtime"));
                return detailDTO;
            }).collect(Collectors.toList());
            return hotelRoomDetailList.get(0);
        }
        return null;
    }

    /**
     * 构建sql查询条件
     *
     * @param adminHotelRoom  房间数据
     * @param queryRequestDTO mysql查询请求体
     */
    private void builderSqlParams(AdminHotelRoom adminHotelRoom, MysqlRequestDTO queryRequestDTO) {
        List<Object> params = new ArrayList<>();
        params.add(adminHotelRoom.getPcate());
        params.add(adminHotelRoom.getTitle());
        params.add(adminHotelRoom.getThumbUrl());
        params.add(adminHotelRoom.getProductprice());
        params.add(adminHotelRoom.getTotal());
        params.add(adminHotelRoom.getTotalcnf());
        params.add(adminHotelRoom.getId());
        queryRequestDTO.setParams(params);
    }
}
