package com.sky.controller.admin;


import com.google.common.collect.Sets;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Slf4j
@Api("菜品相关接口")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 新增菜品
     * @param dishdto
     * @return
     */
    @PostMapping
    public Result addDish(@RequestBody DishDTO dishdto){
        log.info("新增菜品{}",dishdto);
        dishService.save(dishdto);
        String key = "dish" + dishdto.getCategoryId();
        redisTemplate.delete(key);
        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @ApiOperation("菜品分页查询")
    @GetMapping("/page")
    public Result<PageResult> pageDish(DishPageQueryDTO dishPageQueryDTO){
        log.info("分页查询{}",dishPageQueryDTO);
        PageResult pageResult = dishService.pageSelect(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除菜品
     * @param ids
     * @return
     */
    @ApiOperation("批量删除菜品")
    @DeleteMapping
    public Result deleteDish(@RequestParam List<Integer> ids){
        log.info("删除菜品{}",ids);
        dishService.deleteByIds(ids);

        // 将所有缓存的菜品清理掉，所有以dish_开头的key
       clearCache("dish*");
        return Result.success();
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @ApiOperation("根据id查询菜品")
    @GetMapping("/{id}")
    public Result<DishDTO> selectById(@PathVariable Integer id){
        log.info("查看id为{}的菜品",id);
        DishDTO dishDTO = dishService.selectById(id);
        return Result.success(dishDTO);
    }

    /**
     * 修改菜品
     * @param dishdto
     * @return
     */
    @ApiOperation("修改菜品")
    @PutMapping
    public Result updateDish(@RequestBody DishDTO dishdto){
        log.info("要修改菜品{}",dishdto);
        dishService.updateDish(dishdto);
        // 为了简化逻辑，将所有缓存内容清理掉
       clearCache("dish*");
        return Result.success();
    }


    /**
     * 菜品起售停售
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result<String> startorStop(@PathVariable Integer status, Long id){
        dishService.startOrstop(status, id);
       clearCache("dish*");
        return Result.success();
    }



    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */

    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId){
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

    private void clearCache(String pattern){
        Set Keys = redisTemplate.keys(pattern);
        redisTemplate.delete(Keys);
    }
}
