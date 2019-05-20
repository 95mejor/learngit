package com.mejormall.service;

import com.mejormall.common.ServiceResponse;
import com.mejormall.pojo.User;

public interface IUserService {
    ServiceResponse<User> login(String username,String password);

    ServiceResponse<String> register(User user);

    ServiceResponse<String> checkValid(String str,String type);

    ServiceResponse<String> selectQuestion(String username);

    ServiceResponse<String> checkAnswer(String username, String question,String answer);

    ServiceResponse<String> forgetResetPassword(String username,String password,String forgetToken);

    ServiceResponse<String> resetPassword(String passwordOld,String passwordNew,User user);

    ServiceResponse<User> updateInformation(User user);

    ServiceResponse<User> getInformation(int userId);

    ServiceResponse checkAdminRole(User user);
}
