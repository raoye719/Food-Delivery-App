package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedisTemplate<String, Long> longRedisTemplate;

    private static final String ORDER_PAYMENT_TIMEOUT_ZSET = "order:payment:timeout";

    private static final String ORDER_STATUS = "order:status";

 //   @Value("${sky.shop.address}")
    private String shopAddress;

  //  @Value("${sky.baidu.ak}")
    private String ak;


    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WeChatPayUtil weChatPayUtil;


    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public OrderSubmitVO sumbitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        // 处理各种业务异常（地址薄为空，购物车数据为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);}

            Long userId = BaseContext.getCurrentId();
            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setUserId(userId);
            List<ShoppingCart> shoppingCartList = shoppingCartMapper.selectCart(shoppingCart);
            if (shoppingCartList.size() == 0) {
                throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
            }

            // 获取用户地址
            String address = addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName();
            // 检测是否超出范围
        //    checkOutOfRange(address);

            // 向订单表插入一条数据
            Orders order = new Orders();
            BeanUtils.copyProperties(ordersSubmitDTO, order);
            order.setOrderTime(LocalDateTime.now());
            order.setPayStatus(Orders.UN_PAID);
            order.setStatus(Orders.PENDING_PAYMENT);
            order.setNumber(String.valueOf(System.currentTimeMillis()));
            order.setPhone(addressBook.getPhone());
            order.setUserId(userId);
            orderMapper.insert(order);
            // 同步添加到redis
            monitorPaymentTimeout(order.getId());
            // 向订单明细表插入n条数据
            List<OrderDetail> orderDetailArrayList
                    = new ArrayList<>();
            for (ShoppingCart cart : shoppingCartList) {
                OrderDetail orderDetail = new OrderDetail();
                BeanUtils.copyProperties(cart, orderDetail);
                orderDetail.setOrderId(order.getId());
                orderDetailArrayList.add(orderDetail);
            }
            orderDetailMapper.insertBatch(orderDetailArrayList);
            // 清空当前购物车
            shoppingCartMapper.deleteAll(shoppingCart);
            // 封装VO返回结果
            OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder().id(order.getId())
                    .orderNumber(order.getNumber())
                    .orderAmount(order.getAmount())
                    .orderTime(order.getOrderTime())
                    .build();

            return orderSubmitVO;


        }




    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
      //  User user = userMapper.getById(userId);
//
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));
        Long userId = BaseContext.getCurrentId();
        String orderNumber = ordersPaymentDTO.getOrderNumber();
        Orders orders = orderMapper.getByOrderNum(orderNumber);
        orders.setStatus(Orders.TO_BE_CONFIRMED);
        orders.setPayMethod(1);
        orders.setPayStatus(Orders.PAID);
        // 2. 更新 Redis 状态
        longRedisTemplate.opsForHash().put(ORDER_STATUS, orders.getId(), Orders.PAID);
// 3. 从 ZSET 移除（避免被超时任务处理）
        longRedisTemplate.opsForZSet().remove(ORDER_PAYMENT_TIMEOUT_ZSET, orders.getId());
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());

        orderMapper.update(orders);
        return null;
    }


    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    @Override
    public void paySuccess(String outTradeNo){

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        // 通过wenSocket向客户端浏览器推送消息
        Map map = new HashMap();
        map.put("type", 1);
        map.put("orderId", ordersDB.getId());
        map.put("content", "订单号："+outTradeNo);

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 历史订单
     * @param page pageSize status
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult page(Integer page, Integer pageSize, Integer status) {
        PageHelper.startPage(page, pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setPageSize(pageSize);
        ordersPageQueryDTO.setPage(page);
        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(userId);

        Page<Orders> pages = orderMapper.orderQuery(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();

        if(pages.getTotal() > 0 && pages != null){
            for(Orders orders : pages){
                Long orderId = orders.getId();
                List<OrderDetail> orderDetailList = orderDetailMapper.getOrderId(orderId);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                list.add(orderVO);
            }

        }

        return new PageResult(pages.getTotal(), list);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO getById(Integer id) {

        OrderVO orderVO = new OrderVO();
        // 先得到订单号
        Orders orders = orderMapper.getById(id);
        BeanUtils.copyProperties(orders, orderVO);
        // 得到订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getOrderId(orders.getId());
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 取消订单
     * @param id
     */
    @Override
    public void updateStatus(Integer id) throws Exception{
        // 根据id查询订单
        Orders orderDB = orderMapper.getById(id);
        // 校验订单是否存在
        if(orderDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 订单状态
        if(orderDB.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(orderDB.getId());
//        if(orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
//            // 调用微信支付退款接口
//            weChatPayUtil.refund(
//                    orderDB.getNumber(),
//                    orderDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01)
//            );
//            // 支付状态改为退款
//            orders.setPayStatus(Orders.REFUND);
//        }
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 再来一单
     * @param id
     */
    @Transactional
    @Override
    public void paymentAgain(Long id) {

//        我原以为再来一单是重复插入，后面看着原来不是
//        Orders orderDB = orderMapper.getByIdL(id);
//        // 判断是否为空
//        if (orderDB == null){
//            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
//        }
//        // 再下一单
//        Orders orders = new Orders();
//        BeanUtils.copyProperties(orderDB, orders);
//        orderMapper.insert(orders);
//
//        List<OrderDetail> orderDetailList = orderDetailMapper.getOrderId(orderDB.getId());
//
//        orderDetailMapper.insertBatch(orderDetailList);
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getOrderId(id);

        // 将订单详情对象转换成购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream()
                .map(x->{
                    ShoppingCart shoppingCart = new ShoppingCart();

                    // 将原订单详情里面的菜品信息重新复制到购物车对象中
                    BeanUtils.copyProperties(x, shoppingCart,"id");
                    shoppingCart.setUserId(userId);
                    shoppingCart.setCreateTime(LocalDateTime.now());
                    return shoppingCart;
                }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库中
        shoppingCartMapper.insetBatch(shoppingCartList);

    }

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult adminPage(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Long userId = BaseContext.getCurrentId();
        ordersPageQueryDTO.setUserId(userId);

        Page<Orders> page = orderMapper.queryOrders(ordersPageQueryDTO);

        List<OrderVO> list = new ArrayList<>();
        if(page.getTotal() > 0){
            for(Orders orders : page){
                Long orderId = orders.getId();
                List<OrderDetail> orderDetailList = orderDetailMapper.getOrderId(orderId);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 统计订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        // 查询到的数据放在vo里响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);

        return orderStatisticsVO;
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = orderMapper.getByIdL(ordersConfirmDTO.getId());
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(orders.getStatus() != Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void reject(OrdersRejectionDTO ordersRejectionDTO) throws Exception{
        Orders orderDB = orderMapper.getByIdL(ordersRejectionDTO.getId());
        if(orderDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(orderDB.getStatus() != Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        // 支付状态
        Integer payStatus = orderDB.getPayStatus();
        if(payStatus != Orders.PAID){
//            weChatPayUtil.refund(
//                    orderDB.getNumber(),
//                    orderDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01)
//            );
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .status(Orders.CANCELLED)
                .cancelTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 取消订单
     * @param ordersCancelDTO
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        Orders ordersDB = orderMapper.getByIdL(ordersCancelDTO.getId());
        if(ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(ordersDB.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Integer payStatus = ordersDB.getPayStatus();
//        if(payStatus == Orders.PAID) {
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01)
//            );
//        }
        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .cancelReason(ordersCancelDTO.getCancelReason())
                .status(Orders.CANCELLED)
                .cancelTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void delivery(Integer id) {
        Orders orders = orderMapper.getById(id);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(orders.getStatus() != Orders.CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     * @param id
     */
    @Override
    public void complete(Long id) {
        Orders orders = orderMapper.getByIdL(id);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if(orders.getStatus() != Orders.DELIVERY_IN_PROGRESS){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 催单
     * @param id
     */
    @Override
    public void reminder(Long id) {
        Orders orders = orderMapper.getByIdL(id);
        if(orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map map = new HashMap();
        map.put("type", 2);
        map.put("orderId", id);
        map.put("content","订单号" + orders.getNumber());

        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 检查客户的收货地址是否超出配送范围
     * @param address
     */
    private void checkOutOfRange(String address){
        Map map = new HashMap();
        map.put("address", shopAddress);
        map.put("output", "json");
        map.put("ak", ak);

        // 获取店铺的经纬度坐标
        String shoppingCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        JSONObject jsonObject = JSON.parseObject(shoppingCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }

        // 数据解析
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        // 店铺经纬度坐标
        String shopLngLat = lat + "," + lng;

        map.put("address",address);
        // 获取用户收获地址的经纬度坐标
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收获地址解析失败");
        }

        // 数据解析
        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        // 店铺经纬度坐标
        String userLngLat = lat + "," + lng;

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info", 0);

        //路线规划
        String json = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        // 数据解析
        // 使用Fastjson解析
        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = result.getJSONArray("routes");
        Integer distance = jsonArray.getJSONObject(0).getInteger("distance");

        if(distance > 5000){
            // 配送超过5000米
            throw new OrderBusinessException("超出配送范围");
        }





    }


    /**
     * 给订单设置时间
     * 监控超时订单
     * @param orderId
     */
    public void monitorPaymentTimeout(Long orderId){
        // 计算下单后15分钟后面的时间戳（score）
        double expireTime = System.currentTimeMillis() + 0.5 * 60 * 1000;
        longRedisTemplate.opsForZSet().add(ORDER_PAYMENT_TIMEOUT_ZSET, (Long)orderId, expireTime);
        longRedisTemplate.opsForHash().put(ORDER_STATUS,orderId,Orders.PENDING_PAYMENT);
    }


    /**
     * 处理超时订单
     * @return
     */
    @Override
    public List<Long> checkExpiredOrders(){
        List<Long> result = new ArrayList<>();
        //计算当前时间
        long now = System.currentTimeMillis();

        // 获取所有下单时间超过15分钟的订单
        Set<Long> expiredOrders = longRedisTemplate.opsForZSet().rangeByScore(ORDER_PAYMENT_TIMEOUT_ZSET, 0, now);

        if (expiredOrders != null && !expiredOrders.isEmpty()) {
            expiredOrders.forEach(orderId -> {
                Integer status = (Integer) longRedisTemplate.opsForHash().get(ORDER_STATUS, orderId);
                if(status == Orders.PENDING_PAYMENT){
                    // Zset移除已移除的订单
                    longRedisTemplate.opsForHash().delete(ORDER_STATUS, orderId);
                    longRedisTemplate.opsForZSet().remove(ORDER_PAYMENT_TIMEOUT_ZSET, expiredOrders);
                    result.add( orderId);
                }
            }
            );
        }
        return result;
    }






}