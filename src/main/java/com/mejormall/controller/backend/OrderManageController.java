package com.mejormall.controller.backend;

import com.github.pagehelper.PageInfo;
import com.mejormall.common.Const;
import com.mejormall.common.ResponseCode;
import com.mejormall.common.ServiceResponse;
import com.mejormall.pojo.User;
import com.mejormall.service.IOrderService;
import com.mejormall.service.IUserService;
import com.mejormall.vo.OrderVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/manage/order")
public class OrderManageController {
    @Autowired
    private IOrderService iOrderService;
    @Autowired
    private IUserService iUserService;

    @RequestMapping("list.do")
    public ServiceResponse orderList(HttpSession session, @RequestParam(value = "pageNum",defaultValue = "1")Integer pageNum,
                                     @RequestParam(value = "pageSize",defaultValue = "10")Integer pageSize){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，需要强制登录");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            //填充作为管理员的业务逻辑
            return iOrderService.manageList(pageNum, pageSize);
        }else{
            return ServiceResponse.creatByErrorMessage("无权限操作");
        }
    }

    @RequestMapping("detail.do")
    public ServiceResponse<OrderVo> orderDetail(HttpSession session,Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，需要强制登录");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            return iOrderService.manageDetail(orderNo);
        }else{
            return ServiceResponse.creatByErrorMessage("无权限操作");
        }
    }

    @RequestMapping("search.do")
    public ServiceResponse<PageInfo> orderSearch(HttpSession session, Long orderNo,
                                                 @RequestParam(value = "pageNum",defaultValue = "1")Integer pageNum,
                                                 @RequestParam(value = "pageSize",defaultValue = "10")Integer pageSize){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，需要强制登录");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            //填充作为管理员的业务逻辑
            return iOrderService.manageSearch(orderNo,pageNum,pageSize);
        }else{
            return ServiceResponse.creatByErrorMessage("无权限操作");
        }
    }
    @RequestMapping("send_goods.do")
    public ServiceResponse<String> orderSendGoods(HttpSession session,Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，需要强制登录");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            return iOrderService.manageSendGoods(orderNo);
        }else{
            return ServiceResponse.creatByErrorMessage("无权限操作");
        }
    }
}
