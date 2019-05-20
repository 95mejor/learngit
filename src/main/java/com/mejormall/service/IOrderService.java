package com.mejormall.service;

import com.github.pagehelper.PageInfo;
import com.mejormall.common.ServiceResponse;
import com.mejormall.vo.OrderVo;

import java.util.Map;

public interface IOrderService {
    ServiceResponse pay(Long orderNo,Integer userId,String path);

    ServiceResponse aliCallBack(Map<String,String> params);

    ServiceResponse queryOrderPayStatus(Integer userId,Long orderNo);

    ServiceResponse creatOrder(Integer userId,Integer shippingId);

    ServiceResponse cancel(Integer userId,Long orderNo);

    ServiceResponse getOrderCartProduct(Integer userId);

    ServiceResponse getOrderDetail(Integer userId,Long orderNo);

    ServiceResponse<PageInfo> orderList(Integer userId, Integer pageNum, Integer pageSize);

    ServiceResponse<PageInfo> manageList(Integer pageNum,Integer pageSize);

    ServiceResponse<OrderVo> manageDetail(Long orderNo);

    ServiceResponse<PageInfo> manageSearch(Long orderNo,Integer pageNum,Integer pageSize);

    ServiceResponse<String> manageSendGoods(Long orderNo);
}
