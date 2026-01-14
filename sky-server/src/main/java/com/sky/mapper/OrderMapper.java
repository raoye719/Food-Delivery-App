package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * @param order
     */
    void insert(Orders order);


    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 分页查询
     * @param ordersPageQueryDTO
     * @return
     */

    Page<Orders> orderQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    @Select("select * from orders where id = #{id}")
    Orders getById(Integer id);

    @Select("select * from orders where id = #{id}")
    Orders getByIdL(Long id);


    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);


    @Select("select * from orders where number = #{number}")
    Orders getByOrderNum(String number);

    @Select("select * from orders where status >= 2 and status = #{status}")
    Page<Orders> queryOrders(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单状态和下单时间查询订单
     * @param status
     * @param orderTime
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusOrderTimeLT(Integer status, LocalDateTime orderTime);


    @Select("select * from orders where id = #{id} and status = #{status}")
    List<Orders> getByIdsTimeLT( List<Long> id,Integer status);



    Double getSumBymap(Map map);

    Integer CountByMap(Map map);


    /**
     * 找出一定时间内销量最高的几个菜品
     * @param beginTime
     * @param endTime
     * @return
     */
    List<GoodsSalesDTO> getByNameNum(LocalDateTime beginTime, LocalDateTime endTime);
}
