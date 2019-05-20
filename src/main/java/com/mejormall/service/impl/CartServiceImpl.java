package com.mejormall.service.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mejormall.common.Const;
import com.mejormall.common.ResponseCode;
import com.mejormall.common.ServiceResponse;
import com.mejormall.dao.CartMapper;
import com.mejormall.dao.ProductMapper;
import com.mejormall.pojo.Cart;
import com.mejormall.pojo.Product;
import com.mejormall.service.ICartService;
import com.mejormall.util.BigDecimalUtil;
import com.mejormall.util.PropertiesUtil;
import com.mejormall.vo.CartProductVo;
import com.mejormall.vo.CartVo;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service("iCartService")
public class CartServiceImpl implements ICartService {
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;

    @Override
    public ServiceResponse<CartVo> list(Integer userId) {
        CartVo cartVo = this.getCartVoLimit(userId);
        return ServiceResponse.creatBySuccess(cartVo);
    }

    @Override
    public ServiceResponse<CartVo> add(Integer userId, Integer productId, Integer count) {
        if(productId == null && userId == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),"添加购物车商品参数错误");
        }
        Cart cart = cartMapper.selectByUserIdProductId(userId,productId);
        if(cart == null){
            Cart cartItem = new Cart();
            cartItem.setQuantity(count);
            cartItem.setChecked(Const.Cart.ON_CHECKE);
            cartItem.setUserId(userId);
            cartItem.setProductId(productId);
            cartMapper.insert(cartItem);
        }else{
            count = cart.getQuantity() + count;
            cart.setQuantity(count);
            cartMapper.updateByPrimaryKeySelective(cart);
        }
        return this.list(userId);
    }

    @Override
    public ServiceResponse<CartVo> selectOrUnSelect(Integer userId, Integer productId,Integer checked) {
        cartMapper.checkedOrUncheckedProduct(userId,productId,checked);
        return this.list(userId);
    }

    @Override
    public ServiceResponse<CartVo> getCartProductCount(Integer userId) {
        cartMapper.getCartProductCount(userId);
        return this.list(userId);
    }

    private CartVo getCartVoLimit(Integer userId){
        CartVo cartVo = new CartVo();
        List<Cart> carts = cartMapper.selectByUserId(userId);
        List<CartProductVo> cartProductVos = Lists.newArrayList();
        BigDecimal cartProductTotalPrice = new BigDecimal("0");
        if (CollectionUtils.isNotEmpty(carts)){
            for(Cart cart: carts){
                CartProductVo cartProductVo = new CartProductVo();
                cartProductVo.setUserId(cart.getUserId());
                cartProductVo.setProductId(cart.getProductId());
                Product product = productMapper.selectByPrimaryKey(cart.getProductId());
                if(product != null){
                    cartProductVo.setProductName(product.getName());
                    cartProductVo.setProductMainImage(product.getMainImage());
                    cartProductVo.setProductPrice(product.getPrice());
                    cartProductVo.setProductSubTitle(product.getSubtitle());
                    cartProductVo.setProductStatus(product.getStatus());
                    cartProductVo.setProductStock(product.getStock());
                    int buyLimitCount = 0;
                    if(product.getStock() >= cart.getQuantity()){
                        buyLimitCount = cart.getQuantity();
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_CART_SUCCESS);
                    }else{
                        buyLimitCount = product.getStock();
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_CART_FAIL);
                        Cart cartItem = new Cart();
                        cartItem.setId(cart.getId());
                        cartItem.setQuantity(buyLimitCount);
                        cartMapper.updateByPrimaryKeySelective(cartItem);
                    }
                    cartProductVo.setQuantity(buyLimitCount);
                    cartProductVo.setProductTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cartProductVo.getQuantity()));
                    cartProductVo.setProductChecked(cart.getChecked());
                }
                if(cart.getChecked() == Const.Cart.ON_CHECKE){
                    cartProductTotalPrice = BigDecimalUtil.add(cartProductTotalPrice.doubleValue(),cartProductVo.getProductTotalPrice().doubleValue());
                }
                cartProductVos.add(cartProductVo);
            }
        }
        cartVo.setCartTotalPrice(cartProductTotalPrice);
        cartVo.setAllChecked(this.getAllCheckedStatus(userId));
        cartVo.setCartProductVoList(cartProductVos);
        cartVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        return cartVo;
    }
    private boolean getAllCheckedStatus(Integer userId){
        if (userId == null){
            return false;
        }
        return cartMapper.selectCartProductCheckedStatusByUserId(userId) == 0;
    }

    @Override
    public ServiceResponse<CartVo> update(Integer userId, Integer productId, Integer count) {
        if(productId == null && userId == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),"添加购物车商品参数错误");
        }
        Cart cart = cartMapper.selectByUserIdProductId(userId,productId);
        if(cart != null){
            cart.setQuantity(count);
            cartMapper.updateByPrimaryKeySelective(cart);
        }
        return this.list(userId);
    }

    @Override
    public ServiceResponse<CartVo> deleteProduct(Integer userId, String productIds) {
        List<String> productIdList = Splitter.on(",").splitToList(productIds);
        if(CollectionUtils.isNotEmpty(productIdList) && userId == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),"添加购物车商品参数错误");
        }
        cartMapper.deleteByUserIdProductIds(userId,productIdList);
        return this.list(userId);
    }
}
