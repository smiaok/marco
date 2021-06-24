package com.fudan.intellij.plugin.marco.service.remote;

import com.ejlchina.okhttps.HTTP;
import com.fudan.intellij.plugin.marco.constant.KeyConstant;
import com.intellij.ide.util.PropertiesComponent;

public class RemoteService {

    HTTP http = HTTP.builder()
            .baseUrl(PropertiesComponent.getInstance().getValue(KeyConstant.URI_POLO))
            .build();

    public void refreshHttp(){
        this.http = HTTP.builder()
                .baseUrl(PropertiesComponent.getInstance().getValue(KeyConstant.URI_POLO))
                .build();
    }
}
