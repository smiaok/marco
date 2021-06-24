package com.fudan.intellij.plugin.marco.service.remote;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ejlchina.okhttps.HTTP;
import com.ejlchina.okhttps.HttpResult;
import com.fudan.intellij.plugin.marco.constant.KeyConstant;
import com.fudan.intellij.plugin.marco.entity.User;
import com.intellij.ide.util.PropertiesComponent;
import java.util.List;

public class UserRemoteService {

    private HTTP http = HTTP.builder()
            .baseUrl(PropertiesComponent.getInstance().getValue(KeyConstant.URI_POLO))
            .build();

    public boolean ping(){
        try{
            HttpResult httpResult = http.sync(KeyConstant.URI_PING)
                    .post();
            if (httpResult.isSuccessful()) {
                String response = httpResult.getBody().toString();
                CommonResult<Object> result = JSON.parseObject(
                        response, new TypeReference<CommonResult<Object>>() {
                        });
                if(result.getStatus() == KeyConstant.SUCCESS){
                    return true;
                }
            }
        }catch (Exception e){
            return false;
        }
        return false;
    }

    public boolean login(String username,String password) {
        HttpResult httpResult = http.sync(KeyConstant.URI_LOGIN)
                .addBodyPara(KeyConstant.USERNAME, username)
                .addBodyPara(KeyConstant.PASSWORD, password)
                .post();
        if (httpResult.isSuccessful()) {
            String response = httpResult.getBody().toString();
            CommonResult<Boolean> result = JSON.parseObject(
                    response, new TypeReference<CommonResult<Boolean>>() {
            });
            if(result.getStatus() == KeyConstant.SUCCESS){
                String cookie = httpResult.getHeader(KeyConstant.COOKIE);
                String satoken = cookie.substring(cookie.indexOf('=')+1,cookie.indexOf(';'));

                PropertiesComponent.getInstance().setValue(KeyConstant.MACRO_TOKEN, satoken);

                return true;
            }
        }
        return false;
    }

    public boolean logout() {
        HttpResult httpResult = http.sync(KeyConstant.URI_LOGOUT)
                .addHeader(KeyConstant.TOKEN,PropertiesComponent.getInstance().getValue(KeyConstant.MACRO_TOKEN))
                .post();
        if (httpResult.isSuccessful()) {
            String response = httpResult.getBody().toString();
            CommonResult<Boolean> result = JSON.parseObject(
                    response, new TypeReference<CommonResult<Boolean>>() {
                    });
            if(result.getStatus() == KeyConstant.SUCCESS){
                PropertiesComponent.getInstance().unsetValue(KeyConstant.MACRO_TOKEN);
                return result.getData();
            }
        }
        return false;
    }

    public User userInfo(String username) {
        HttpResult httpResult = http.sync(KeyConstant.URI_USER_INFO)
                .addHeader(KeyConstant.TOKEN,PropertiesComponent.getInstance().getValue(KeyConstant.MACRO_TOKEN))
                .addBodyPara(KeyConstant.USERNAME,username)
                .post();
        if (httpResult.isSuccessful()) {
            String response = httpResult.getBody().toString();
            CommonResult<User> result = JSON.parseObject(
                    response, new TypeReference<CommonResult<User>>() {
                    });
            if(result.getStatus() == KeyConstant.SUCCESS){
                return result.getData();
            }
        }
        return null;
    }

    public boolean isLogin(){
        HttpResult httpResult = http.sync(KeyConstant.URI_IS_LOGIN)
                .addHeader(KeyConstant.TOKEN,PropertiesComponent.getInstance().getValue(KeyConstant.MACRO_TOKEN))
                .post();
        if (httpResult.isSuccessful()) {
            String response = httpResult.getBody().toString();
            CommonResult<Boolean> result = JSON.parseObject(
                    response, new TypeReference<CommonResult<Boolean>>() {
                    });
            if(result.getStatus() == KeyConstant.SUCCESS){
                return result.getData();
            }
        }
        return false;
    }

    public boolean register(String username, String password, String email,String phone) {
        HttpResult httpResult = http.sync(KeyConstant.URI_REGISTER)
                .addBodyPara(KeyConstant.USERNAME,username)
                .addBodyPara(KeyConstant.PASSWORD,password)
                .addBodyPara(KeyConstant.EMAIL,email)
                .addBodyPara(KeyConstant.PHONE,phone)
                .post();
        if (httpResult.isSuccessful()) {
            String response = httpResult.getBody().toString();
            CommonResult<Boolean> result = JSON.parseObject(
                   response, new TypeReference<CommonResult<Boolean>>() {
                    });
            if(result.getStatus() == KeyConstant.SUCCESS){
                return result.getData();
            }
        }
        return false;
    }

    public List<User> getAllUser(){
        HttpResult httpResult = http.sync(KeyConstant.URI_GET_ALL_USER)
                .addHeader(KeyConstant.TOKEN,PropertiesComponent.getInstance().getValue(KeyConstant.MACRO_TOKEN))
                .post();
        if (httpResult.isSuccessful()) {
            String response = httpResult.getBody().toString();
            CommonResult<List<User>> result = JSON.parseObject(
                    response, new TypeReference<CommonResult<List<User>>>(User.class) {
                    });
            if(result.getStatus() == KeyConstant.SUCCESS){
                return result.getData();
            }
        }
        return null;
    }

    public void refreshHttp(){
        this.http = HTTP.builder()
                .baseUrl(PropertiesComponent.getInstance().getValue(KeyConstant.URI_POLO))
                .build();
    }

}
