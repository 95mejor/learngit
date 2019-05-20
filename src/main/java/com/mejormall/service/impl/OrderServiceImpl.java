package com.mejormall.service.impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mejormall.common.Const;
import com.mejormall.common.ServiceResponse;
import com.mejormall.dao.*;
import com.mejormall.pojo.*;
import com.mejormall.service.IOrderService;
import com.mejormall.util.BigDecimalUtil;
import com.mejormall.util.DateTimeUtil;
import com.mejormall.util.FTPUtil;
import com.mejormall.util.PropertiesUtil;
import com.mejormall.vo.OrderItemVo;
import com.mejormall.vo.OrderProductVo;
import com.mejormall.vo.OrderVo;
import com.mejormall.vo.ShippingVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service("iOrderService")
public class OrderServiceImpl implements IOrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private static AlipayTradeService tradeService;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private PayInfoMapper payInfoMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ShippingMapper shippingMapper;

    @Override
    public ServiceResponse creatOrder(Integer userId, Integer shippingId) {
        //获取当前用户的购物车中选中的产品
        List<Cart> cartList = cartMapper.selectCheckedByUserId(userId);
        //计算单个商品总价
        ServiceResponse serviceResponse= this.getCartOrderItem(userId,cartList);
        if(!serviceResponse.isSuccess()){
            return serviceResponse;
        }
        //计算购物车所有商品总价
        List<OrderItem> orderItems = (List<OrderItem>)serviceResponse.getData();
        BigDecimal payment = this.getOrderTotalPrice(orderItems);
        //生成订单
        Order order = this.assembleOrder(userId,shippingId,payment);
        if(order == null){
            return ServiceResponse.creatByErrorMessage("生成订单错误");
        }
        if(CollectionUtils.isEmpty(orderItems)){
            return ServiceResponse.creatByErrorMessage("未选中商品");
        }
        for(OrderItem orderItem : orderItems){
            orderItem.setOrderNo(order.getOrderNo());
        }
        //mybatis批量插入
        orderItemMapper.batchInsert(orderItems);
        //生成成功，要减少库存和清空购物车
        this.cleanCart(cartList);
        this.reduceProductStock(orderItems);
        //返回给前端数据
        OrderVo orderVo = assembleOrderVo(order,orderItems);
        return ServiceResponse.creatBySuccess(orderVo);
    }

    @Override
    public ServiceResponse cancel(Integer userId, Long orderNo) {
         Order order = orderMapper.selectByUserIdOrderNo(userId, orderNo);
         if(order == null){
             return ServiceResponse.creatByErrorMessage("订单不存在");
         }
         if(order.getStatus() >= Const.OrderStatusEnum.NO_PAY.getCode()){
             return ServiceResponse.creatByErrorMessage("用户已付款，暂时无法取消订单");
         }
         Order updateOrder = new Order();
         updateOrder.setOrderNo(order.getOrderNo());
         updateOrder.setStatus(Const.OrderStatusEnum.CANCLED.getCode());
         int rowCount = orderMapper.updateByPrimaryKeySelective(updateOrder);
         if(rowCount > 0){
             return ServiceResponse.creatBySuccess();
         }
         return ServiceResponse.creatByError();
    }

    @Override
    public ServiceResponse getOrderCartProduct(Integer userId) {
        OrderProductVo orderProductVo = new OrderProductVo();
        List<Cart> carts = cartMapper.selectCheckedByUserId(userId);
        ServiceResponse serviceResponse = getCartOrderItem(userId,carts);
        if(!serviceResponse.isSuccess()){
            return serviceResponse;
        }
        List<OrderItem> orderItems = (List<OrderItem>)serviceResponse.getData();
        List<OrderItemVo> orderItemVos = Lists.newArrayList();
        BigDecimal totalPay = new BigDecimal("0");
        for(OrderItem orderItem : orderItems){
            totalPay = BigDecimalUtil.add(orderItem.getTotalPrice().doubleValue(),totalPay.doubleValue());
            orderItemVos.add(assembleOrderItemVo(orderItem));
        }
        orderProductVo.setOrderItemVoList(orderItemVos);
        orderProductVo.setTotalPay(totalPay);
        orderProductVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        return ServiceResponse.creatBySuccess(orderProductVo);
    }

    @Override
    public ServiceResponse getOrderDetail(Integer userId, Long orderNo) {
        Order order = orderMapper.selectByUserIdOrderNo(userId,orderNo);
        if(order != null){
            List<OrderItem> orderItems = orderItemMapper.selectByUserIdOrderNo(userId, orderNo);
            OrderVo orderVo = assembleOrderVo(order,orderItems);
            return ServiceResponse.creatBySuccess(orderVo);
        }
        return ServiceResponse.creatBySuccessMessage("没有该订单!");
    }

    @Override
    public ServiceResponse<PageInfo> orderList(Integer userId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Order> orders = orderMapper.selectByUserId(userId);
        List<OrderVo> orderVos = assembleOrderVoList(orders,userId);
        PageInfo pageInfo = new PageInfo(orderVos);
        return ServiceResponse.creatBySuccess(pageInfo);
    }

    private List<OrderVo> assembleOrderVoList(List<Order> orders,Integer userId){
        List<OrderVo> orderVos = Lists.newArrayList();
        for(Order order : orders){
            List<OrderItem> orderItems = Lists.newArrayList();
            if(userId == null){
                //管理员身份登录时，可以看到任何订单，方便方法复用
                orderItems = orderItemMapper.selectByOrderNo(order.getOrderNo());
            }else {
                orderItems = orderItemMapper.selectByUserIdOrderNo(userId,order.getOrderNo());
            }
            orderVos.add(assembleOrderVo(order,orderItems));
        }
        return orderVos;
    }

    private OrderVo assembleOrderVo(Order order, List<OrderItem> orderItems){
        OrderVo orderVo = new OrderVo();
        orderVo.setOrderNo(order.getOrderNo());
        orderVo.setPayment(order.getPayment());
        orderVo.setPaymentType(order.getPaymentType());
        orderVo.setPaymentTypeDesc(Const.PaymentTypeEnum.codeOf(order.getPaymentType()).getValue());
        orderVo.setPostage(order.getPostage());
        orderVo.setStatus(order.getStatus());
        orderVo.setStatusDesc(Const.OrderStatusEnum.codeOf(order.getStatus()).getValue());
        orderVo.setShippingId(order.getShippingId());
        Shipping shipping = shippingMapper.selectByPrimaryKey(order.getShippingId());
        if(shipping != null){
            orderVo.setReceiverName(shipping.getReceiverName());
            orderVo.setShippingVo(assembleShippingVo(shipping));
        }
        orderVo.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
        orderVo.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
        orderVo.setClosedTime(DateTimeUtil.dateToStr(order.getCloseTime()));
        orderVo.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));
        orderVo.setCreateTime(DateTimeUtil.dateToStr(order.getCreateTime()));

        orderVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        List<OrderItemVo> orderItemVos = Lists.newArrayList();
        for(OrderItem orderItem : orderItems){
            OrderItemVo orderItemVo = assembleOrderItemVo(orderItem);
            orderItemVos.add(orderItemVo);
        }
        orderVo.setOrderItemVoList(orderItemVos);
        return orderVo;
    }

    private OrderItemVo assembleOrderItemVo(OrderItem orderItem){
        OrderItemVo orderItemVo = new OrderItemVo();
        orderItemVo.setOrderNo(orderItem.getOrderNo());
        orderItemVo.setProductId(orderItem.getProductId());
        orderItemVo.setProductImage(orderItem.getProductImage());
        orderItemVo.setProductName(orderItem.getProductName());
        orderItemVo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
        orderItemVo.setQuantity(orderItem.getQuantity());
        orderItemVo.setTotalPrice(orderItem.getTotalPrice());
        orderItemVo.setCreateTime(DateTimeUtil.dateToStr(orderItem.getCreateTime()));
        return orderItemVo;
    }

    private ShippingVo assembleShippingVo(Shipping shipping){
        ShippingVo shippingVo = new ShippingVo();
        shippingVo.setReceiverName(shipping.getReceiverName());
        shippingVo.setReceiverMobile(shipping.getReceiverMobile());
        shippingVo.setReceiverPhone(shipping.getReceiverPhone());
        shippingVo.setReceiverProvince(shipping.getReceiverProvince());
        shippingVo.setReceiverAddress(shipping.getReceiverAddress());
        shippingVo.setReceiverCity(shipping.getReceiverCity());
        shippingVo.setReceiverDistrict(shipping.getReceiverDistrict());
        shippingVo.setReceiverZip(shipping.getReceiverZip());
        return shippingVo;
    }

    private void cleanCart(List<Cart> carts){
        for(Cart cart : carts){
            cartMapper.deleteByPrimaryKey(cart.getId());
        }
    }

    private void reduceProductStock(List<OrderItem> orderItems){
        for(OrderItem orderItem : orderItems){
            Product product = productMapper.selectByPrimaryKey(orderItem.getProductId());
            product.setStock(product.getStock()-orderItem.getQuantity());
            productMapper.updateByPrimaryKeySelective(product);
        }
    }

    private Order assembleOrder(Integer userId,Integer shippingId,BigDecimal payment){
        Order order = new Order();
        long orderNo = generateOrderNo();
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setStatus(Const.OrderStatusEnum.NO_PAY.getCode());
        order.setPayment(payment);
        order.setShippingId(shippingId);
        order.setPostage(0);
        order.setPaymentType(Const.PaymentTypeEnum.ONLINE.getCode());
        //发货时间、付款时间等等
        int rowCount = orderMapper.insert(order);
        if(rowCount > 0){
            return order;
        }
        return null;
    }

    private Long generateOrderNo(){
        long currentTime = System.currentTimeMillis();
        return currentTime+new Random().nextInt(100);
    }

    private BigDecimal getOrderTotalPrice(List<OrderItem> orderItems){
        BigDecimal payment = new BigDecimal("0");
        for(OrderItem orderItem : orderItems){
            payment = BigDecimalUtil.add(orderItem.getTotalPrice().doubleValue(),payment.doubleValue());
        }
        return payment;
    }

    private ServiceResponse getCartOrderItem(Integer userId,List<Cart> cartList){
        if(CollectionUtils.isEmpty(cartList)){
            return ServiceResponse.creatByErrorMessage("购物车为空");
        }
        List<OrderItem> orderItems = Lists.newArrayList();
        for(Cart cart : cartList){
            OrderItem orderItem = new OrderItem();
            Product product = productMapper.selectByPrimaryKey(cart.getProductId());
            if(Const.ProductStatusEnum.ON_SALE.getCode() != product.getStatus()){
                return ServiceResponse.creatByErrorMessage("商品不是在售状态");
            }
            if(cart.getQuantity() >= product.getStock()){
                return ServiceResponse.creatByErrorMessage("商品库存不足");
            }
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setCurrentUnitPrice(product.getPrice());
            orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cart.getQuantity().doubleValue()));
            orderItem.setUserId(userId);
            orderItems.add(orderItem);
        }
        return ServiceResponse.creatBySuccess(orderItems);
    }


    @Override
    public ServiceResponse pay(Long orderNo, Integer userId, String path) {
        //获取用户的订单号
        Map<String,String> resultMap = Maps.newHashMap();
        Order order = orderMapper.selectByUserIdOrderNo(userId,orderNo);
        if(order == null){
            return ServiceResponse.creatByErrorMessage("用户没有该订单");
        }
        resultMap.put("orderNo",order.getOrderNo().toString());

        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = order.getOrderNo().toString();

        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = new StringBuilder().append("mejormall扫码支付，订单号：").append(outTradeNo).toString();

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = order.getPayment().toString();

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";

        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = new StringBuilder().append("订单为").append(outTradeNo).append("购买商品共").append(totalAmount).append("元").toString();

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");

        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();
        List<OrderItem> orderItems = orderItemMapper.selectByUserIdOrderNo(userId, orderNo);
        for(OrderItem orderItem : orderItems){
            GoodsDetail goodsDetail = GoodsDetail.newInstance(orderItem.getProductId().toString(),orderItem.getProductName(),
                    BigDecimalUtil.mul(orderItem.getCurrentUnitPrice().doubleValue(),new Double(100).doubleValue()).longValue(),
                    orderItem.getQuantity());
            goodsDetailList.add(goodsDetail);
        }
        /*// 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
        GoodsDetail goods1 = GoodsDetail.newInstance("goods_id001", "xxx小面包", 1000, 1);
        // 创建好一个商品后添加至商品明细列表
        goodsDetailList.add(goods1);

        // 继续创建并添加第一条商品信息，用户购买的产品为“黑人牙刷”，单价为5.00元，购买了两件
        GoodsDetail goods2 = GoodsDetail.newInstance("goods_id002", "xxx牙刷", 500, 2);
        goodsDetailList.add(goods2);*/

        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
                .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
                .setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
                .setGoodsDetailList(goodsDetailList);

        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
        tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();
        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                logger.info("支付宝预下单成功: )");
                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);
                File folder = new File(path);
                if(!folder.exists()){
                    folder.setWritable(true);
                    folder.mkdirs();
                }

                // 需要修改为运行机器上的路径
                String qrPath = String.format(path + "/qr-%s.png",response.getOutTradeNo());
                String qrName = String.format("qr-%s.png",response.getOutTradeNo());
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);
                //上传二维码图片到ftp服务器
                File targetFile = new File(path,qrName);
                try {
                    FTPUtil.uploadFile(Lists.newArrayList(targetFile));
                } catch (IOException e) {
                    logger.error("上传二维码图片异常",e);
                }
                logger.info("qrPath:" + qrPath);
                String qrUrl = PropertiesUtil.getProperty("ftp.server.http.prefix") + qrName;
                resultMap.put("qrUrl",qrUrl);
                return ServiceResponse.creatBySuccess(resultMap);
            case FAILED:
                logger.error("支付宝预下单失败!!!");
                return ServiceResponse.creatByErrorMessage("支付宝预下单失败!!!");
            case UNKNOWN:
                logger.error("系统异常，预下单状态未知!!!");
                return ServiceResponse.creatByErrorMessage("系统异常，预下单状态未知!!!");
            default:
                logger.error("不支持的交易状态，交易返回异常!!!");
                return ServiceResponse.creatByErrorMessage("不支持的交易状态，交易返回异常!!!");
        }
    }

    // 简单打印应答
    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            logger.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                logger.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                        response.getSubMsg()));
            }
            logger.info("body:" + response.getBody());
        }
    }

    @Override
    public ServiceResponse aliCallBack(Map<String, String> params) {
        Long orderNo = Long.parseLong(params.get("out_trade_no"));
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order == null){
            return  ServiceResponse.creatByErrorMessage("非本商城订单，回调忽略");
        }
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()){
            return ServiceResponse.creatBySuccess("支付宝回调重复通知");
        }
        if(Const.AlipayCallBack.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)){
            order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment")));
            order.setStatus(Const.OrderStatusEnum.PAID.getCode());
            orderMapper.updateByPrimaryKeySelective(order);
        }
        PayInfo payInfo = new PayInfo();
        payInfo.setUserId(order.getUserId());
        payInfo.setOrderNo(orderNo);
        payInfo.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode());
        payInfo.setPlatformNumber(tradeNo);
        payInfo.setPlatformStatus(tradeStatus);
        payInfoMapper.insert(payInfo);
        return ServiceResponse.creatBySuccess();
    }

    @Override
    public ServiceResponse queryOrderPayStatus(Integer userId, Long orderNo) {
        Order order = orderMapper.selectByUserIdOrderNo(userId, orderNo);
        if(order == null){
            return ServiceResponse.creatByErrorMessage("用户没有该订单");
        }
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()){
            return ServiceResponse.creatBySuccess();
        }
        return ServiceResponse.creatByError();
    }

    //backend


    @Override
    public ServiceResponse<PageInfo> manageList(Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Order> orders = orderMapper.selectAllOrder();
        List<OrderVo> orderVos = assembleOrderVoList(orders,null);
        PageInfo pageInfo = new PageInfo(orders);
        pageInfo.setList(orderVos);
        return ServiceResponse.creatBySuccess(pageInfo);
    }

    @Override
    public ServiceResponse<OrderVo> manageDetail(Long orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            List<OrderItem> orderItems = orderItemMapper.selectByOrderNo(orderNo);
            OrderVo orderVo = assembleOrderVo(order,orderItems);
            return ServiceResponse.creatBySuccess(orderVo);
        }
        return ServiceResponse.creatByErrorMessage("订单不存在");
    }

    @Override
    public ServiceResponse<PageInfo> manageSearch(Long orderNo,Integer pageNum,Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            List<OrderItem> orderItems = orderItemMapper.selectByOrderNo(orderNo);
            OrderVo orderVo = assembleOrderVo(order,orderItems);
            PageInfo pageInfo = new PageInfo(Lists.newArrayList(orderVo));
            return ServiceResponse.creatBySuccess(pageInfo);
        }
        return ServiceResponse.creatByErrorMessage("订单不存在");
    }

    @Override
    public ServiceResponse<String> manageSendGoods(Long orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            if(order.getStatus() == Const.OrderStatusEnum.PAID.getCode()){
                order.setStatus(Const.OrderStatusEnum.SHIPPED.getCode());
                order.setSendTime(new Date());
                return ServiceResponse.creatBySuccessMessage("已发货");
            }
        }
        return ServiceResponse.creatByErrorMessage("订单不存在");
    }
}
