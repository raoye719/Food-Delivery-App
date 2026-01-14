package com.sky.task;

import com.sky.constant.MessageConstant;
import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderMapper;
import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务类，定时处理订单信息
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    /**
     * 处理超时订单
     */
    @Scheduled(cron = "1/5 * * * * *")
    public void processTimeoutOrder() {
        log.info("定时处理超时订单:{}", LocalDateTime.now());

         List<Long> orderIdLists = orderService.checkExpiredOrders();
    // 出现超时订单
         if(!orderIdLists.isEmpty() && orderIdLists != null){
            for(Long orderId : orderIdLists){
                Orders order = orderMapper.getByIdL(orderId);
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }

         }
    }

    /**
     * 处理配送订单
     */
    @Scheduled(cron = "0 0 1 * * *") // 每天凌晨一点
   // @Scheduled(cron = "0/5 * * * * *")
    public void processDeliveryOrder(){
        log.info("定时处理超时配送订单:{}", LocalDateTime.now());

        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);

        List<Orders> orderList = orderMapper.getByStatusOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);

        if(orderList != null && orderList.size() > 0){
            for(Orders order : orderList){
                order.setStatus(Orders.COMPLETED);
                order.setDeliveryTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }


    }
}
