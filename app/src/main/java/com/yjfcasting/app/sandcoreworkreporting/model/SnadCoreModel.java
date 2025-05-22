package com.yjfcasting.app.sandcoreworkreporting.model;

import android.util.Log;

import androidx.annotation.NonNull;

import com.yjfcasting.app.sandcoreworkreporting.Constant;
import com.yjfcasting.app.sandcoreworkreporting.Utility;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.Request;

public class SnadCoreModel {
    // 報工
    public Request UploadSfcData(@NonNull String[] workOrderArr, @NonNull ArrayList<ArrayList<String>> gradingData, int index, String userCode, String flaskId){
        FormBody body = new FormBody.Builder()
                .add("ProductionOrderHead", workOrderArr[0])
                .add("ProductionOrder",  workOrderArr[1])
                .add("Type", gradingData.get(index).get(0).indexOf("未進站") != -1 ? "in" : "out")
                .add("UserCode", userCode)
                .add("Sequence", "0")
                .add("TransferDate", new SimpleDateFormat("yyyyMMdd").format(new Date())).build();
        if (flaskId != null && !flaskId.equals("")){
            body = new FormBody.Builder()
                    .add("ProductionOrderHead", workOrderArr[0])
                    .add("ProductionOrder",  workOrderArr[1])
                    .add("Type", gradingData.get(index).get(0).indexOf("未進站") != -1 ? "in" : "out")
                    .add("UserCode", userCode)
                    .add("Sequence", "0")
                    .add("FlaskID", flaskId)
                    .add("BottomFlaskID", flaskId)
                    .add("TransferDate", new SimpleDateFormat("yyyyMMdd").format(new Date())).build();
        }


        Request request = Utility.composeRequest("/api/SandCoreSFTUpdate")
                .post(body)
                .build();
        return request;
    }
    // 取得砂心列表
    public Request GetSandCoreList(String workGroup, String deptName, Boolean isManager){
        Log.d("debug", "workGroup:"+workGroup);
        Log.d("debug", "deptName:"+deptName);
        Request request = null;
        if (!isManager) {
            if (workGroup == null || workGroup.indexOf("砂心") != -1) {
                request = Utility.composeRequest("/api/LoadSandCoreData")
                        .get()
                        .build();
            } else {
                FormBody body = new FormBody.Builder()
                        .add("workGroup", workGroup)
                        .add("deptName", deptName)
                        .build();
                request = Utility.composeRequest("/api/LoadSfteData")
                        .post(body)
                        .build();
            }
        } else {
            request = Utility.composeRequest("/api/LoadTotalSftData")
                    .get()   // 讀取資料超時
                    .build();
        }
        return request;
    }

}
