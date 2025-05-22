package com.yjfcasting.app.sandcoreworkreporting.model;

import android.view.PixelCopy;

import com.yjfcasting.app.sandcoreworkreporting.Utility;

import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.FormBody;
import okhttp3.Request;

public class LoginModel {
    public String workerNumber;
    public Request Login(String workerNumber){
        FormBody body = new FormBody.Builder()
                .add("employeecode", workerNumber)
                .build();
        Request request = Utility.composeRequest("/api/GetEmployeeDept")
                .post(body)
                .build();
        return request;
    }


}
