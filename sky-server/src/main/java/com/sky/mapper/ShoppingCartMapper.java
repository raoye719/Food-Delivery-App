package com.sky.mapper;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 查询购物车是否有对应商品
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 根据id增加商品数量
     * @param shoppingCart
     */
    @Update("update shopping_cart set number = #{number} where id = #{id}")
    void updateNum(ShoppingCart shoppingCart);

    @Insert("insert into shopping_cart(name, image, user_id, dish_id, setmeal_id, dish_flavor, amount, create_time) " +
            "VALUES " +
            "(#{name}, #{image},#{userId}, #{dishId}, #{setmealId}, #{dishFlavor} ,#{amount}, #{createTime})")
    void insert(ShoppingCart shoppingCart);


    @Select("select * from shopping_cart where user_id = #{userId}")
    List<ShoppingCart> selectCart(ShoppingCart shoppingCart);

    /**
     * 清空购物车·
     * @param shoppingCart
     */
    @Delete("delete from shopping_cart where user_id = #{userId}")
    void deleteAll(ShoppingCart shoppingCart);

    @Delete("delete from shopping_cart where number = #{number}")
    void deleteOne(ShoppingCart cart);


    void insetBatch(List<ShoppingCart> shoppingCartList);
}
