package com.mejormall.service.impl;

import com.mejormall.common.Const;
import com.mejormall.common.ServiceResponse;
import com.mejormall.common.TokenCache;
import com.mejormall.dao.UserMapper;
import com.mejormall.pojo.User;
import com.mejormall.service.IUserService;
import com.mejormall.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("iUserService")
public class UserServiceImpl implements IUserService {
    @Autowired
    private UserMapper userMapper;
    @Override
    public ServiceResponse<User> login(String username, String password) {
        int result = userMapper.checkUsername(username);
        if(result == 0){
            return ServiceResponse.creatByErrorMessage("用户名不存在");
        }
        //   MD5加密
        String md5Password = MD5Util.MD5EncodeUtf8(password);
        User user = userMapper.selectLogin(username, md5Password);
        if(user == null){
            return ServiceResponse.creatByErrorMessage("密码错误");
        }
        user.setPassword(StringUtils.EMPTY);
        return ServiceResponse.creatBySuccess("登陆成功",user);
    }

    @Override
    public ServiceResponse<String> register(User user) {
        ServiceResponse validResponse = checkValid(user.getUsername(),Const.USERNAME);
        if(!validResponse.isSuccess()){
            return validResponse;
        }
        validResponse = checkValid(user.getEmail(),Const.EMAIL);
        if(!validResponse.isSuccess()){
            return validResponse;
        }
        user.setRole(Const.Role.ROLE_CUSTOMER);
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
        int result = userMapper.insert(user);
        if(result == 0){
            return ServiceResponse.creatByErrorMessage("注册失败");
        }
        return ServiceResponse.creatBySuccessMessage("注册成功");
    }

    @Override
    public ServiceResponse<String> checkValid(String str, String type) {
        if (StringUtils.isNotBlank(type)) {
            if(Const.USERNAME.equals(type)){
                int num = userMapper.checkUsername(str);
                if(num > 0){
                    return ServiceResponse.creatByErrorMessage("用户名已存在");
                }
            }else if(Const.EMAIL.equals(type)){
                int num = userMapper.checkEmail(str);
                if(num > 0){
                    return ServiceResponse.creatByErrorMessage("邮箱已存在");
                }
            }
        }else{
            return ServiceResponse.creatByErrorMessage("参数错误");
        }
        return ServiceResponse.creatBySuccessMessage("校验成功");
    }

    @Override
    public ServiceResponse<String> selectQuestion(String username) {
        ServiceResponse validResponse = this.checkValid(username,Const.USERNAME);
        if(validResponse.isSuccess()){
            return ServiceResponse.creatByErrorMessage("用户名不存在");
        }
        String question = userMapper.selectQuestionByUsername(username);
        if(StringUtils.isNotBlank(question)){
            return ServiceResponse.creatBySuccess(question);
        }
        return ServiceResponse.creatByErrorMessage("用户找回密码的问题为空！");
    }

    @Override
    public ServiceResponse<String> checkAnswer(String username, String question, String answer) {
        int resultCount = userMapper.checkAnswer(username,question,answer);
        if(resultCount > 0){
            //说明这个问题和答案是这个用户的，并且正确
            String forgetToken = UUID.randomUUID().toString();
            TokenCache.setKey(TokenCache.TOKEN_PREFIX+username,forgetToken);
            return ServiceResponse.creatBySuccess(forgetToken);
        }
        return ServiceResponse.creatByErrorMessage("问题的答案错误");
    }

    @Override
    public ServiceResponse<String> forgetResetPassword(String username,String passwordNew,String forgetToken){
        if(StringUtils.isBlank(forgetToken)){
            return ServiceResponse.creatByErrorMessage("参数错误，需要传递token");
        }
        ServiceResponse validResponse = checkValid(username,Const.USERNAME);
        if(validResponse.isSuccess()){
            return ServiceResponse.creatByErrorMessage("用户名不存在！");
        }
        String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX+username);
        if(StringUtils.isBlank(token)){
            return ServiceResponse.creatByErrorMessage("token无效或过期");
        }
        if(StringUtils.equals(forgetToken,token)){
            String MD5Password = MD5Util.MD5EncodeUtf8(passwordNew);
            int rowcount = userMapper.updatePasswordByUsername(username,MD5Password);
            if(rowcount > 0){
                return ServiceResponse.creatBySuccessMessage("修改密码成功！");
            }
        }else{
            return ServiceResponse.creatByErrorMessage("token错误，请重新获取重置密码的token！");
        }
        return ServiceResponse.creatByErrorMessage("修改密码失败！");
    }

    @Override
    public ServiceResponse<String> resetPassword(String passwordOld, String passwordNew,User user) {
        //防止横向越权，需要校验原始密码，一定要指定是这个用户，因为我们查询的是count（1）
        int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld),user.getId());
        if(resultCount == 0){
            return ServiceResponse.creatByErrorMessage("原始密码错误");
        }
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        int updatecount = userMapper.updateByPrimaryKeySelective(user);
        if(updatecount > 0){
            return ServiceResponse.creatBySuccessMessage("修改密码成功");
        }
        return ServiceResponse.creatBySuccessMessage("修改密码失败！");
    }

    @Override
    public ServiceResponse<User> updateInformation(User user){
        //username不能被更新
        int resultcount = userMapper.checkEmailByUserId(user.getEmail(),user.getId());
        if(resultcount > 0){
            return ServiceResponse.creatByErrorMessage("当前Email已存在");
        }
        User currentUser = new User();
        currentUser.setId(user.getId());
        currentUser.setEmail(user.getEmail());
        currentUser.setQuestion(user.getQuestion());
        currentUser.setAnswer(user.getAnswer());
        currentUser.setPhone(user.getPhone());
        currentUser.setUpdateTime(user.getUpdateTime());
        int updatecount = userMapper.updateByPrimaryKeySelective(currentUser);
        if(updatecount > 0){
            return ServiceResponse.creatBySuccess("更新个人信息成功",currentUser);
        }
        return ServiceResponse.creatByErrorMessage("更新个人信息失败");
    }

    @Override
    public ServiceResponse<User> getInformation(int userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if(user == null){
            return ServiceResponse.creatByErrorMessage("找不到用户");
        }
        user.setPassword(StringUtils.EMPTY);
        return ServiceResponse.creatBySuccess(user);
    }

    @Override
    public ServiceResponse checkAdminRole(User user){
        if(user != null && (user.getRole() == Const.Role.ROLE_ADMIN)){
            return ServiceResponse.creatBySuccess();
        }
        return ServiceResponse.creatByError();
    }
}
