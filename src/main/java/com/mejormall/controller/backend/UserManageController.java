package com.mejormall.controller.backend;

import com.mejormall.common.Const;
import com.mejormall.common.ServiceResponse;
import com.mejormall.pojo.User;
import com.mejormall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/manage/user")
public class UserManageController {
    @Autowired
    private IUserService iUserService;

    @RequestMapping(value = "/login.do",method = RequestMethod.POST)
    @ResponseBody
    public ServiceResponse<User> login(String username, String password, HttpSession session){
        ServiceResponse response = iUserService.login(username,password);
        if(response.isSuccess()){
            User user = (User)response.getData();
            if(user.getRole() == Const.Role.ROLE_ADMIN){
                session.setAttribute(Const.CURRENT_USER,user);
                return response;
            }else{
                return ServiceResponse.creatByErrorMessage("该用户不是管理员");
            }
        }
        return response;
    }
}
