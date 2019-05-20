package com.mejormall.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.mejormall.common.ResponseCode;
import com.mejormall.common.ServiceResponse;
import com.mejormall.dao.ShippingMapper;
import com.mejormall.pojo.Shipping;
import com.mejormall.service.IShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("iShippingService")
public class ShippingServiceImpl implements IShippingService {
    @Autowired
    private ShippingMapper shippingMapper;

    @Override
    public ServiceResponse add(Integer userId, Shipping shipping) {
        shipping.setUserId(userId);
        int rowCount = shippingMapper.insert(shipping);
        if(rowCount > 0){
            Map result = new HashMap();
            result.put("shippingId",shipping.getId());
            return ServiceResponse.creatBySuccess("新建地址成功",result);
        }
        return ServiceResponse.creatByErrorMessage("新建地址失败");
    }

    @Override
    public ServiceResponse<String> delete(Integer userId, Integer shippingId) {
        int rowCount = shippingMapper.deleteByShippingIdUserId(userId,shippingId);
        if(rowCount > 0){
            return ServiceResponse.creatBySuccessMessage("删除地址成功");
        }
        return ServiceResponse.creatBySuccessMessage("删除地址失败");
    }

    @Override
    public ServiceResponse<String> update(Integer userId, Shipping shipping) {
        if(shipping == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),"修改地址参数错误");
        }
        shipping.setUserId(userId);
        int rowCount = shippingMapper.updateByShippingIdUserId(shipping);
        if(rowCount > 0){
            return ServiceResponse.creatBySuccessMessage("更新地址信息成功");
        }
        return ServiceResponse.creatByErrorMessage("更新地址信息失败");
    }

    @Override
    public ServiceResponse<Shipping> select(Integer userId, Integer shippingId) {
        Shipping shipping = shippingMapper.selectByShippingIdUserId(userId,shippingId);
        if(shipping == null){
            return ServiceResponse.creatByErrorMessage("无法查询到该地址");
        }
        return ServiceResponse.creatBySuccess(shipping);
    }

    @Override
    public ServiceResponse<PageInfo> list(Integer userId, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum,pageSize);
        List<Shipping> shippings = shippingMapper.selectByUserId(userId);
        PageInfo pageInfo = new PageInfo(shippings);
        return ServiceResponse.creatBySuccess(pageInfo);
    }
}
