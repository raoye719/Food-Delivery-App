package com.sky.controller.user;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.Orders;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderService")
@Slf4j
@RequestMapping("/user/order")
@Api("订单相关接口")
public class OrderController {

    @Autowired
    private OrderService orderService;
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")

    public Result<OrderSubmitVO> submitVOResult(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("下单具体信息{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.sumbitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 历史订单
     * @param page, pageSize, status
     * @return
     */
    @ApiOperation("查询历史订单信息")
    @GetMapping("/historyOrders")
    public Result<PageResult> page(Integer page, Integer pageSize, Integer status) {
        log.info("具体分页信息{},{},{}", page, pageSize, status);
        PageResult pageResult = orderService.page(page, pageSize, status);
        return Result.success(pageResult);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @ApiOperation("查询订单详情")
    @GetMapping("/orderDetail/{id}")
    public Result<OrderVO> getByNumber(@PathVariable Integer id){
        log.info("查询订单id:{}", id);
        OrderVO orderVO = orderService.getById(id);
        return Result.success(orderVO);
    }


    @PutMapping("/cancel/{id}")
    public Result CancelOrder(@PathVariable Integer id) throws Exception {
        log.info("取消订单id:{}", id);
        orderService.updateStatus(id);
        return Result.success();
    }

    /**
     * 再来一单
     * @param id
     * @return
     */
    @ApiOperation("再来一单")
    @PostMapping("/repetition/{id}")
    public Result repetition(@PathVariable Long id) {
        log.info("订单号{}再来一单",id);
        orderService.paymentAgain(id);
        return Result.success();
    }


    /**
     * 催单
     * @param id
     * @return
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("催单")
    public Result reminder(@PathVariable Long id) {
        orderService.reminder(id);
        return Result.success();
    }




}
