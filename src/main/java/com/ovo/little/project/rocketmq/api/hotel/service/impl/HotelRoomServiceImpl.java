package com.ovo.little.project.rocketmq.api.hotel.service.impl;

import com.alibaba.fastjson.JSON;
import com.ovo.little.project.rocketmq.api.hotel.dto.HotelRoom;
import com.ovo.little.project.rocketmq.api.hotel.dto.RoomDescription;
import com.ovo.little.project.rocketmq.api.hotel.dto.RoomPicture;
import com.ovo.little.project.rocketmq.api.hotel.enums.HotelBusinessErrorCodeEnum;
import com.ovo.little.project.rocketmq.api.hotel.service.HotelRoomService;
import com.ovo.little.project.rocketmq.common.constant.RedisKeyConstant;
import com.ovo.little.project.rocketmq.common.exception.BusinessException;
import com.ruyuan.little.project.common.dto.CommonResponse;
import com.ruyuan.little.project.common.enums.ErrorCodeEnum;
import com.ruyuan.little.project.common.enums.LittleProjectTypeEnum;
import com.ruyuan.little.project.mysql.api.MysqlApi;
import com.ruyuan.little.project.mysql.dto.MysqlRequestDTO;
import com.ruyuan.little.project.redis.api.RedisApi;
import org.apache.dubbo.config.annotation.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author QAQ
 * @date 2021/8/6
 */

@Service
public class HotelRoomServiceImpl implements HotelRoomService {

    private Logger LOGGER = LoggerFactory.getLogger(HotelRoomServiceImpl.class);

    /**
     * mysql dubbo接口api
     */
    @Reference(version = "1.0.0",
            interfaceClass = MysqlApi.class,
            cluster = "failfast")
    private MysqlApi mysqlApi;

    /**
     * redsi dubbo接口api
     */
    @Reference(version = "1.0.0",
            interfaceClass = RedisApi.class,
            cluster = "failfast")
    private RedisApi redisApi;

    /**
     * 房间本地缓存
     */
    @Autowired
    private HotelRoomCacheManager hotelRoomCacheManager;

    @Override
    public CommonResponse<HotelRoom> getRoomById(Long id, String phoneNumber) {
        HotelRoom hotelRoom = hotelRoomCacheManager.getHotelRoomFromLocalCache(id);
        if (!Objects.isNull(hotelRoom)) {
            // jvm缓存不为空
            LOGGER.info("hotelId:{} data hit local cache", id);
            return CommonResponse.success(hotelRoom);
        }
        LOGGER.info("hotelId:{} data local cache miss try from redis query", id);
        // jvm缓存不存在 查询redis缓存
        LOGGER.info("start query room data from redis cache param:{}", id);
        CommonResponse<String> commonResponse = redisApi.get(RedisKeyConstant.HOTEL_ROOM_KEY_PREFIX + id,
                phoneNumber,
                LittleProjectTypeEnum.ROCKETMQ);
        LOGGER.info("end query room data from redis cache param:{}, commonResponse:{}", id, JSON.toJSONString(commonResponse));

        if (Objects.equals(commonResponse.getCode(), ErrorCodeEnum.SUCCESS.getCode())) {
            String data = commonResponse.getData();
            if (StringUtils.hasLength(data)) {
                // redis缓存不为空
                LOGGER.info("hotelId:{} data hit redis cache ", id);
                return CommonResponse.success(JSON.parseObject(data, HotelRoom.class));
            }
        }
        // redis宕机或查询jvm内存为空 查询db
        LOGGER.info("hotelId:{} data local cache and redis cache miss try from db query", id);
        return CommonResponse.success(this.getHotelRoomById(id, phoneNumber));
    }

    /**
     * 根据房间id查询房间内容
     *
     * @param id          房间id
     * @param phoneNumber 手机号
     * @return 房间信息
     */
    private HotelRoom getHotelRoomById(Long id, String phoneNumber) {
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
            if (!CollectionUtils.isEmpty(mapList)) {
                List<HotelRoom> hotelRoomDetailList = mapList.stream().map(map -> {
                    HotelRoom detailDTO = new HotelRoom();
                    detailDTO.setId((Long) map.get("id"));
                    detailDTO.setTitle(String.valueOf(map.get("title")));
                    detailDTO.setPcate((Long) map.get("pcate"));
                    detailDTO.setThumbUrl(String.valueOf(map.get("thumb_url")));
                    detailDTO.setRoomDescription(JSON.parseObject(String.valueOf(map.get("description")), RoomDescription.class));
                    String goods_banner = String.valueOf(map.get("goods_banner"));
                    List<RoomPicture> roomPictures = JSON.parseArray(goods_banner, RoomPicture.class);
                    detailDTO.setGoods_banner(roomPictures);
                    detailDTO.setMarketprice((BigDecimal) map.get("marketprice"));
                    detailDTO.setProductprice((BigDecimal) map.get("productprice"));
                    detailDTO.setTotal((Integer) map.get("total"));
                    detailDTO.setCreatetime((Long) map.get("createtime"));
                    return detailDTO;
                }).collect(Collectors.toList());

                // 存入redis缓存
                HotelRoom hotelRoom = hotelRoomDetailList.get(0);
                redisApi.set(RedisKeyConstant.HOTEL_ROOM_KEY_PREFIX + id,
                        JSON.toJSONString(hotelRoom),
                        phoneNumber,
                        LittleProjectTypeEnum.ROCKETMQ);

                // 本地缓存
                hotelRoomCacheManager.updateLocalCache(hotelRoom);

                return hotelRoom;
            }
        }
        LOGGER.info("hotelId:{} data db not exist", id);
        // 房间不存在
        throw new BusinessException(HotelBusinessErrorCodeEnum.HOTEL_ROOM_NOT_EXIST.getMsg());
    }
}
