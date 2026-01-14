package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishFlavorMapper {

    void saveBatch(List<DishFlavor> flavors);

    void deleteByIds(@Param("dishIds") List<Integer> ids);

    @Select("select * from dish_flavor where dish_id = #{dishId}")
    DishFlavor selectById(@Param("dishId") Integer id);


    void updateDishFlavor(DishFlavor dishFlavor);

    List<DishFlavor> getByDishId( Long id);
}
