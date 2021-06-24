package com.fudan.intellij.plugin.marco.service.remote;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.ejlchina.okhttps.HttpResult;
import com.fudan.intellij.plugin.marco.constant.KeyConstant;
import com.fudan.intellij.plugin.marco.entity.MarkReview;
import com.fudan.intellij.plugin.marco.entity.User;
import com.intellij.ide.util.PropertiesComponent;

import java.util.ArrayList;
import java.util.List;

public class MarkReviewRemoteService extends RemoteService{

    public boolean updateFilePath(long id,String filePath){
        HttpResult httpResult = http.sync("/updateFilePath")
                .addBodyPara(KeyConstant.FILE,filePath)
                .addBodyPara(KeyConstant.MARKREVIEW_ID,id)
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
    public boolean updateComment(long id,String comment){
        HttpResult httpResult = http.sync("/updateComment")
                .addBodyPara(KeyConstant.COMMENT,comment)
                .addBodyPara(KeyConstant.MARKREVIEW_ID,id)
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



    public List<MarkReview> getAllMarkReview(){
        HttpResult httpResult = http.sync("/getAllMarkReview")
                .addHeader(KeyConstant.TOKEN,PropertiesComponent.getInstance().getValue(KeyConstant.MACRO_TOKEN))
                .post();
        if (httpResult.isSuccessful()) {
            String response = httpResult.getBody().toString();
            CommonResult<List<MarkReview>> result = JSON.parseObject(
                    response, new TypeReference<CommonResult<List<MarkReview>>>() {
                    });
            if(result.getStatus() == KeyConstant.SUCCESS){
                return result.getData();
            }
        }
        return new ArrayList<>();
    }

    public long uploadMarkReview(MarkReview markReview) {
        HttpResult httpResult = http.sync(KeyConstant.URI_UPLOAD_MARKREVIEW)
                .addHeader(KeyConstant.TOKEN,PropertiesComponent.getInstance().getValue(KeyConstant.MACRO_TOKEN))
                .addBodyPara(KeyConstant.REVIEWER,markReview.getReviewer())
                .addBodyPara(KeyConstant.STATUS,markReview.getStatus())
                .addBodyPara(KeyConstant.BRANCH,markReview.getBranch())
                .addBodyPara(KeyConstant.FILE,markReview.getFile())
                .addBodyPara(KeyConstant.PROJECT,markReview.getProject())
                .addBodyPara(KeyConstant.VERSION,markReview.getVersion())
                .addBodyPara(KeyConstant.COMMENT,markReview.getComment())
                .addBodyPara(KeyConstant.TYPE,markReview.getType())
                .addBodyPara(KeyConstant.LINE,markReview.getLine())
                .post();
        if (httpResult.isSuccessful()) {
            String response = httpResult.getBody().toString();
            CommonResult<Long> result = JSON.parseObject(
                    response, new TypeReference<CommonResult<Long>>() {
                    });
            if(result.getStatus() == KeyConstant.SUCCESS){
                return result.getData();
            }
        }
        return -1;
    }

    public boolean updateMarkReviewStatus(long markReviewId, String status) {
        HttpResult httpResult = http.sync(KeyConstant.URI_UPDATE_MARKREVIEW_STATUS)
                .addHeader(KeyConstant.TOKEN,PropertiesComponent.getInstance().getValue(KeyConstant.MACRO_TOKEN))
                .addBodyPara(KeyConstant.MARKREVIEW_ID,markReviewId)
                .addBodyPara(KeyConstant.STATUS,status)
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
        return false;
    }

    public List<MarkReview> getAllMarkReviewAsMarker(){
        HttpResult httpResult = http.sync(KeyConstant.URI_GET_ALL_MARKREVIEW_AS_MARKER)
                .addHeader(KeyConstant.TOKEN,PropertiesComponent.getInstance().getValue(KeyConstant.MACRO_TOKEN))
                .post();
        if (httpResult.isSuccessful()) {
            String response = httpResult.getBody().toString();
            CommonResult<List<MarkReview>> result = JSON.parseObject(
                    response, new TypeReference<CommonResult<List<MarkReview>>>(MarkReview.class) {
                    });
            if(result.getStatus() == KeyConstant.SUCCESS){
                return result.getData();
            }
        }
        return null;
    }

    public List<MarkReview> getAllMarkReviewAsReviewer(){
        HttpResult httpResult = http.sync(KeyConstant.URI_GET_ALL_MARKREVIEW_AS_REVIEWER)
                .addHeader(KeyConstant.TOKEN,PropertiesComponent.getInstance().getValue(KeyConstant.MACRO_TOKEN))
                .post();
        if (httpResult.isSuccessful()) {
            String response = httpResult.getBody().toString();
            CommonResult<List<MarkReview>> result = JSON.parseObject(
                    response, new TypeReference<CommonResult<List<MarkReview>>>(MarkReview.class) {
                    });
            if(result.getStatus() == KeyConstant.SUCCESS){
                return result.getData();
            }
        }
        return null;
    }

    public List<User> getAllReviewerForMarkReview(long markReviewId){
        HttpResult httpResult = http.sync(KeyConstant.URI_GET_ALL_REVIEWER_FOR_MARKREVIEW)
                .addHeader(KeyConstant.TOKEN,PropertiesComponent.getInstance().getValue(KeyConstant.MACRO_TOKEN))
                .addBodyPara(KeyConstant.MARKREVIEW_ID,markReviewId)
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

    public boolean addReviewerForMarkReview(long markReviewId,String reviewer){
        HttpResult httpResult = http.sync(KeyConstant.URI_ADD_REVIEWER_FOR_MARKREVIEW)
                .addHeader(KeyConstant.TOKEN,PropertiesComponent.getInstance().getValue(KeyConstant.MACRO_TOKEN))
                .addBodyPara(KeyConstant.MARKREVIEW_ID,markReviewId)
                .addBodyPara(KeyConstant.REVIEWER,reviewer)
                .post();
        if (httpResult.isSuccessful()) {
            String response = httpResult.getBody().toString();
            CommonResult<Boolean> result = JSON.parseObject(
                    response, new TypeReference<CommonResult<Boolean>>(User.class) {
                    });
            if(result.getStatus() == KeyConstant.SUCCESS){
                return true;
            }
        }
        return false;
    }
}
