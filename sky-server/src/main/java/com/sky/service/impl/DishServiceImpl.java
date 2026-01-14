package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public PageResult pageSelect(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(),dishPageQueryDTO.getPageSize());
        Page<Dish> page =dishMapper.pageQuery(dishPageQueryDTO);

        Long total = page.getTotal();

        List<Dish> result = page.getResult();

        return new PageResult(total,result);
    }

    /**
     * 增加菜品
     * @param dishdto
     */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(DishDTO dishdto) {

        // 向菜品表添加数据
        Dish dish = new Dish();

        BeanUtils.copyProperties(dishdto, dish);

        dishMapper.save(dish);

        // 向口味表添加数据
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishdto.getFlavors();

        if(flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dishId);
            });

            dishFlavorMapper.saveBatch(flavors);

        }

       // dishMapper.save();
    }

    @Override
    public void deleteByIds(List<Integer> ids) {

        // 判断菜品是否可以进行删除-是否存在起售中的菜品
        for (Integer id : ids) {
            Dish dish = dishMapper.selectByIds(id);
            if(dish.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        // 判断菜品是否可以删除 -是否被套餐关联了
        List<Integer> setmealIds = setmealMapper.getSetmealByDishIds(ids);
        if(setmealIds != null && setmealIds.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 删除菜品表菜品数据
        dishMapper.deleteByIds(ids);

        // 删除关联的口味数据
        dishFlavorMapper.deleteByIds(ids);
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @Override
    public DishDTO selectById(Integer id) {
        Dish dish = dishMapper.selectById(id);
        DishFlavor dishFlavor = dishFlavorMapper.selectById(id);
        DishDTO dishDTO = new DishDTO();
        BeanUtils.copyProperties(dish, dishDTO);
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> {
                flavor.setDishId(dishFlavor.getDishId());
                flavor.setId(dishFlavor.getId());
                flavor.setName(dishFlavor.getName());
                flavor.setValue(dishFlavor.getValue());
            });
        }
        return dishDTO;
    }

    /**
     * 修改菜品
     * @param dishdto
     */
    @Override
    public void updateDish(DishDTO dishdto) {
        Long id = dishdto.getId();

        // 修改菜品表
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishdto, dish);
        dishMapper.updateDish(dish);

        // 修改口味表
        // 口味表是否修改了要判断
        if(dishdto.getFlavors() == null || dishdto.getFlavors().size() == 0) {
            return;
        }

        DishFlavor dishFlavor = new DishFlavor();


        dishdto.getFlavors().forEach(flavor -> {
            dishFlavor.setDishId(flavor.getDishId());
            dishFlavor.setId(flavor.getId());
            dishFlavor.setName(flavor.getName());
            dishFlavor.setValue(flavor.getValue());
        });
        dishFlavorMapper.updateDishFlavor(dishFlavor);
    }


    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    @Override
    public List<DishVO> listWithFlavor(Dish dish) {

        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {

            DishVO dishVO = new DishVO();

            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }

    /**
     *菜品起售停售
     * @param status
     * @param id
     */
    @Override
    public void startOrstop(Integer status, Long id) {
        Dish dish = dishMapper.startOrstop(id);
        dish.setStatus(status);
        dishMapper.updateDish(dish);
    }


}
