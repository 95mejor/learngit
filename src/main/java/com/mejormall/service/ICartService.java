package com.mejormall.service;

import com.mejormall.common.ServiceResponse;
import com.mejormall.vo.CartVo;

public interface ICartService {
    ServiceResponse<CartVo> add(Integer userId, Integer productId, Integer count);

    ServiceResponse<CartVo> update(Integer userId,Integer productId,Integer count);

    ServiceResponse<CartVo> deleteProduct(Integer userId, String productIds);

    ServiceResponse<CartVo> list(Integer userId);

    ServiceResponse<CartVo> selectOrUnSelect(Integer userId,Integer productId,Integer checked);

    ServiceResponse<CartVo> getCartProductCount(Integer userId);
}
