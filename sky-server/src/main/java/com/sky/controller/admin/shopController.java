package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Slf4j
@Api("店铺相关接口")
public class shopController {

    @Autowired
    private RedisTemplate redisTemplate;

    @PutMapping("/{ststus}")
    @ApiOperation("设置店铺的营业状态")
    public Result setStatus(@PathVariable Integer ststus) {
        log.info("设置营业状态为{}",ststus == 0 ? "打样中":"营业中");
        log.info("当前序列化器: {}", redisTemplate.getValueSerializer().getClass());
        redisTemplate.opsForValue().set("SHOP_STATUS", ststus);
        return Result.success();
    }


    @GetMapping("/status")
    @ApiOperation("查看店铺的营业状态")
    public Result<Integer> getStatus() {

        Integer shopStatus = (Integer) redisTemplate.opsForValue().get("SHOP_STATUS");

        log.info("当前店铺的营业状态为{}",shopStatus == 1 ? "营业中" : "打样中");

        return Result.success(shopStatus);
    }
}
