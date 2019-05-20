package com.mejormall.controller.portal;

import com.mejormall.common.Const;
import com.mejormall.common.ResponseCode;
import com.mejormall.common.ServiceResponse;
import com.mejormall.pojo.User;
import com.mejormall.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController   //Controller和responseBody的结合，将返回值类型序列化为json格式，restFul技术
@RequestMapping("/user")
public class UserController {
    @Autowired
    private IUserService iUserService;
    /**
     * 用户登录
     * @param username
     * @param password
     * @return
     */
    @RequestMapping(value = "login.do" , method = RequestMethod.POST)
    public ServiceResponse<User> login(String username, String password, HttpSession session){
        ServiceResponse response = iUserService.login(username, password);
        if(response.isSuccess()){
            session.setAttribute(Const.CURRENT_USER,response.getData());
        }
        return response;
    }

    @RequestMapping(value = "logout.do" , method = RequestMethod.POST)
    public ServiceResponse logout(HttpSession session){
        session.removeAttribute(Const.CURRENT_USER);
        return ServiceResponse.creatBySuccess();
    }

    @RequestMapping(value = "register.do" , method = RequestMethod.POST)
    public ServiceResponse<String> register(User user){
        return iUserService.register(user);
    }

    @RequestMapping(value = "check_valid.do" , method = RequestMethod.POST)
    public ServiceResponse<String> checkValid(String str, String type){
        return iUserService.checkValid(str,type);
    }

    @RequestMapping(value = "get_user_info.do" , method = RequestMethod.POST)
    public ServiceResponse<User> getUserInfo(HttpSession session){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user != null){
            return ServiceResponse.creatBySuccess(user);
        }
        return ServiceResponse.creatByErrorMessage("用户当前未登录，无法获取用户名！");
    }

    @RequestMapping(value = "forget_get_question.do",method = RequestMethod.POST)
    public ServiceResponse<String> forgetGetQuestion(String username){
        return iUserService.selectQuestion(username);
    }

    @RequestMapping(value = "forget_check_answer.do",method = RequestMethod.POST)
    public ServiceResponse<String> forgetCheckAnswer(String username,String question,String answer){
        return iUserService.checkAnswer(username,question,answer);
    }

    @RequestMapping(value = "forget_reset_password.do",method = RequestMethod.POST)
    public ServiceResponse<String> forgetResetPassword(String username,String passwordNew,String forgetToken){
        return iUserService.forgetResetPassword(username,passwordNew,forgetToken);
    }

    @RequestMapping(value = "reset_password.do",method = RequestMethod.POST)
    public ServiceResponse<String> resetPassword(HttpSession session,String passwordOld,String passwordNew){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServiceResponse.creatByErrorMessage("请用户先登陆！");
        }
        return iUserService.resetPassword(passwordOld,passwordNew,user);
    }

    @RequestMapping(value = "update_information.do",method = RequestMethod.POST)
    public ServiceResponse<User> updateInformation(HttpSession session,User user){
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if(currentUser == null){
            return ServiceResponse.creatByErrorMessage("请用户登陆！");
        }
        user.setId(currentUser.getId());
        user.setUsername(currentUser.getUsername());//保证更新的用户是当前用户，用户的ID和username从当前登陆用户获取
        user.setUpdateTime(currentUser.getUpdateTime());
        ServiceResponse<User> response = iUserService.updateInformation(user);
        if(response.isSuccess()){
            session.setAttribute(Const.CURRENT_USER,response.getData());
        }
        return response;
    }

    @RequestMapping(value = "get_information.do",method = RequestMethod.POST)
    public ServiceResponse<User> getInformation(HttpSession session){
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if(currentUser == null){
            return ServiceResponse.creatByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"用户未登录，需强制登陆");
        }
        return iUserService.getInformation(currentUser.getId());
    }
}
