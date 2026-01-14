package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 添加菜品表数据
     * @param dish
     */
    @AutoFill(value = OperationType.INSERT)
    void save(Dish dish);

    Page<Dish> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteByIds(List<Integer> ids);

    @Select("select * from dish where id = #{id}")
    Dish selectByIds(Integer id);

    @Select("select * from dish where id = #{id}")
    Dish GetById(Long id);

    @Select("select * from dish where id = #{id}")
    Dish selectById(Integer id);

    void updateDish(Dish dish);


    List<Dish> list(Dish dish);

    /**
     * 根据套餐id查询菜品
     * @param setmealId
     * @return
     */
    @Select("select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = #{setmealId}")
    List<Dish> getBySetmealId(Long setmealId);

    @Select("select * from dish where id = #{id}")
    Dish startOrstop(Long id);

    /**
     * 根据条件统计菜品数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);

}
