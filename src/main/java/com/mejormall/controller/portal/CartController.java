package com.mejormall.controller.portal;

import com.mejormall.common.Const;
import com.mejormall.common.ResponseCode;
import com.mejormall.common.ServiceResponse;
import com.mejormall.pojo.User;
import com.mejormall.service.ICartService;
import com.mejormall.vo.CartVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/cart")
public class CartController {
    @Autowired
    private ICartService iCartService;

    @RequestMapping("list.do")
    public ServiceResponse<CartVo> list(HttpSession session){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，需要强制登录");
        }
        return iCartService.list(user.getId());
    }
    @RequestMapping("add.do")
    public ServiceResponse<CartVo> add(HttpSession session, Integer count, Integer productId){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，需要强制登录");
        }
        return iCartService.add(user.getId(),productId,count);
    }

    @RequestMapping("update.do")
    public ServiceResponse<CartVo> update(HttpSession session,Integer productId,Integer count){
        User user= (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，需要强制登录");
        }
        return iCartService.update(user.getId(),productId,count);
    }

    @RequestMapping("delete_product.do")
    public ServiceResponse deleteProduct(HttpSession session,String productIds){
        User user= (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，需要强制登录");
        }
        return iCartService.deleteProduct(user.getId(),productIds);
    }

    @RequestMapping("select_all.do")
    public ServiceResponse selectAll(HttpSession session,Integer productId){
        User user= (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，需要强制登录");
        }
        return iCartService.selectOrUnSelect(user.getId(),productId,Const.Cart.ON_CHECKE);
    }

    @RequestMapping("unselect_all.do")
    public ServiceResponse unSelectAll(HttpSession session,Integer productId){
        User user= (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，需要强制登录");
        }
        return iCartService.selectOrUnSelect(user.getId(),productId,Const.Cart.UN_CHECKE);
    }

    @RequestMapping("select.do")
    public ServiceResponse select(HttpSession session,Integer productId){
        User user= (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，需要强制登录");
        }
        return iCartService.selectOrUnSelect(user.getId(),productId,Const.Cart.ON_CHECKE);
    }

    @RequestMapping("unselect.do")
    public ServiceResponse unSelect(HttpSession session,Integer productId){
        User user= (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，需要强制登录");
        }
        return iCartService.selectOrUnSelect(user.getId(),productId,Const.Cart.UN_CHECKE);
    }

    @RequestMapping("get_cart_product_count.do")
    public ServiceResponse getCartProductCount(HttpSession session){
        User user= (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，需要强制登录");
        }
        return iCartService.getCartProductCount(user.getId());
    }
}
