package com.mejormall.service;

import com.github.pagehelper.PageInfo;
import com.mejormall.common.ServiceResponse;
import com.mejormall.pojo.Shipping;

public interface IShippingService {
    ServiceResponse add(Integer userId, Shipping shipping);

    ServiceResponse<String> delete(Integer userId,Integer shippingId);

    ServiceResponse<String> update(Integer userId,Shipping shipping);

    ServiceResponse<Shipping> select(Integer userId,Integer shippingId);

    ServiceResponse<PageInfo> list(Integer userId, Integer pageNum, Integer pageSize);
}
