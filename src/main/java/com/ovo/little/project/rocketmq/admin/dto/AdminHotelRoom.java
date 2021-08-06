package com.ovo.little.project.rocketmq.admin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author QAQ
 * @date 2021/8/7
 */

@Data
public class AdminHotelRoom {
    /**
     * 用户手机号
     */
    private String phoneNumber;

    /**
     * 房间id
     */
    private Long id;

    /**
     * "id":"4009",
     * "title":"豪华客房",
     * "pcate":"1981",
     * "thumb_url":"https://weapp-1303909892.file.myqcloud.com//image/20201221/6e222a7cc34f48db.jpg",
     * <p>
     * 房间名称
     */
    private String title;

    /**
     * 店铺id
     */
    private Long pcate;

    /**
     * 商品图片
     */
    private String thumbUrl;

    /**
     * 房间详细信息
     */
    private AdminRoomDescription roomDescription;

    /**
     * 房间图片信息
     */
    private List<AdminRoomPicture> goods_banner;

    /**
     * 参考价格
     */
    private BigDecimal marketprice;

    /**
     * 实际价格
     */
    private BigDecimal productprice;

    /**
     * 商品的数量
     */
    private Integer total;

    private Integer totalcnf;

    /**
     * 创建时间 unix时间
     */
    private Long createtime;

}
