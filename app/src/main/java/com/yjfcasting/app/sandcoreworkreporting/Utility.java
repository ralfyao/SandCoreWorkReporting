package com.yjfcasting.app.sandcoreworkreporting;

import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.yjfcasting.app.sandcoreworkreporting.vo.BaseResponse;
import com.yjfcasting.app.sandcoreworkreporting.vo.SandcoreWorkOrderRes;
import com.yjfcasting.app.sandcoreworkreporting.vo.WareHouseStockRes;

import java.util.Arrays;

import okhttp3.Request;
import okhttp3.ResponseBody;

public class Utility {
     // 組成HTTP request
    @NonNull
    public static Request.Builder composeRequest(String url){
        Request.Builder request = new Request.Builder()
                .url(Constant.url + url);
        return request;
    }
}
