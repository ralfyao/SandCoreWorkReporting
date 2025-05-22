package com.yjfcasting.app.sandcoreworkreporting;

import androidx.annotation.NonNull;

import okhttp3.Request;

public class Utility {
     // 組成HTTP request
    @NonNull
    public static Request.Builder composeRequest(String url){
        Request.Builder request = new Request.Builder()
                .url(Constant.url + url);
        return request;
    }
}
