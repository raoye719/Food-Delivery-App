package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface DishService {
    /**
     * 分页查询菜品
     * @param dishPageQueryDTO
     * @return
     */
    PageResult pageSelect(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 新增菜品
     * @param dishdto
     */
    void save(DishDTO dishdto);

    /**
     * 批量删除菜品
     * @param ids
     */
    void deleteByIds(List<Integer> ids);

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    DishDTO selectById(Integer id);

    /**
     * 修改菜品
     * @param dishdto
     */
    void updateDish(DishDTO dishdto);


    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    List<Dish> list(Long categoryId);

    /**
     * 起售停售菜品
     * @param status
     * @param id
     */
    void startOrstop(Integer status, Long id);
}
