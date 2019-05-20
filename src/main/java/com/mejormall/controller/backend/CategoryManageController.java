package com.mejormall.controller.backend;

import com.mejormall.common.Const;
import com.mejormall.common.ResponseCode;
import com.mejormall.common.ServiceResponse;
import com.mejormall.pojo.User;
import com.mejormall.service.ICategoryService;
import com.mejormall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/manage/category")
public class CategoryManageController {
    @Autowired
    private IUserService iUserService;

    @Autowired
    private ICategoryService iCategoryService;

    @RequestMapping(value = "/add_category.do")
    public ServiceResponse addCategory(HttpSession session, String categoryName, @RequestParam(value = "parentId", defaultValue = "0")int parentId){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登陆！");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            //是管理员
            return iCategoryService.addCategory(categoryName,parentId);
        }else {
            return ServiceResponse.creatByErrorMessage("用户无权限，需要管理员权限！");
        }
    }

    @RequestMapping(value = "/set_category.do")
    public ServiceResponse setCategory(HttpSession session,String categoryName,Integer categoryId) {
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登陆！");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            //是管理员
            return iCategoryService.setCategory(categoryName,categoryId);
        }else {
            return ServiceResponse.creatByErrorMessage("用户无权限，需要管理员权限！");
        }
    }

    @RequestMapping(value = "/get_category.do")
    public ServiceResponse findChildrenParallelCategory(HttpSession session, @RequestParam(value = "parentId", defaultValue = "0")Integer parentId){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登陆！");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            //是管理员
            return iCategoryService.findParallelCategory(parentId);
        }else {
            return ServiceResponse.creatByErrorMessage("用户无权限，需要管理员权限！");
        }
    }

    @RequestMapping(value = "/get_deep_category.do")
    public ServiceResponse findChildrenAndDeepCategory(HttpSession session, @RequestParam(value = "parentId", defaultValue = "0")Integer parentId){
        User user = (User) session.getAttribute(Const.CURRENT_USER);
        if (user == null) {
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(), "用户未登陆！");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            //是管理员
            return iCategoryService.findAllCategory(parentId);
        }else {
            return ServiceResponse.creatByErrorMessage("用户无权限，需要管理员权限！");
        }
    }
}
