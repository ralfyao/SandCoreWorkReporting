package com.yjfcasting.app.sandcoreworkreporting.model;

import android.util.Log;

import androidx.annotation.NonNull;

import com.yjfcasting.app.sandcoreworkreporting.Utility;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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
                .add("AppVersion", "1.0")
                .add("TransferDate", new SimpleDateFormat("yyyyMMdd").format(new Date())).build();
        if (flaskId != null && !flaskId.equals("")){
            body = new FormBody.Builder()
                    .add("ProductionOrderHead", workOrderArr[0])
                    .add("ProductionOrder",  workOrderArr[1])
                    .add("Type", gradingData.get(index).get(0).indexOf("未進站") != -1 ? "in" : "out")
                    .add("UserCode", userCode)
                    .add("Sequence", "0")
                    .add("AppVersion", "1.0")
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
    public Request GetSandCoreList(String workGroup, String deptName, String alternateDeptName, Boolean isManager){
        Log.d("debug", "workGroup:"+workGroup);
        Log.d("debug", "deptName:"+deptName);
        Log.d("debug", "alternateDeptName:"+alternateDeptName);
        Request request = null;
        if (!isManager) {
            if (workGroup == null || workGroup.indexOf("砂心") != -1) {
                request = Utility.composeRequest("/api/LoadSandCoreData?sandCoreGroup="+deptName)
                        .get()
                        .build();
            } else {
                FormBody body = new FormBody.Builder()
                        .add("workGroup", workGroup)
                        .add("deptName", deptName)
                        .add("alternateDeptName", alternateDeptName)
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
    public Request GetDriverSandCoreMoldList(String driver){
        FormBody body = new FormBody.Builder()
                .add("driver", driver)
                .build();
        return Utility.composeRequest("/api/GetSandCoreMoldDispatchList?driver="+driver)
                .post(body)
                .build();
    }
    public Request UpdateReceiveCompFlag(String workOrder, String itemNo, boolean actioned, String reportWorkingNumber, String location, String endLocation, String action, int isReceive){
        Integer newReceive = new Integer(isReceive);
        FormBody body = new FormBody.Builder()
                .add("WorkOrder", workOrder)
                .add("Action", action)
                .add("ItemNo", itemNo)
                .add("ReportWorkingNumber", reportWorkingNumber)
                .add("Location", location)
                .add("EndLocation", endLocation)
                .add("IsReceiveComplete", newReceive.toString())
                .build();
        return Utility.composeRequest("/api/UpdateSandCoreMoldDriveRecvComp")
                .post(body)
                .build();
    }

    public Request GetBRParamValueReq(String workOrderHead, String workOrder) {
        FormBody body = new FormBody.Builder()
                .add("workOrderHead", workOrderHead)
                .add("workOrder", workOrder)
                .build();
        return Utility.composeRequest("/api/GetBRParamData")
                .post(body)
                .build();
    }

    public Request GetStockerList() {
        return Utility.composeRequest("/api/GetWHStockList")
                .get()
                .build();
    }

    public Request GetWareHouseList() {
        return Utility.composeRequest("/api/GetWareHouseList")
                .get()
                .build();
    }

    public Request RepairFlask(String[] workOrderArr, ArrayList<ArrayList<String>> gradingData, int index, String userCode, String flaskId) {
        FormBody body = new FormBody.Builder()
                .add("ProductionOrderHead", workOrderArr[0])
                .add("ProductionOrder",  workOrderArr[1])
                .add("Type", gradingData.get(index).get(0).indexOf("未進站") != -1 ? "in" : "out")
                .add("UserCode", userCode)
                .add("Sequence", "0")
                .add("FlaskID", flaskId)
                .add("BottomFlaskID", flaskId)
                .add("TransferDate", new SimpleDateFormat("yyyyMMdd").format(new Date())).build();
        Request request = Utility.composeRequest("/api/SFCRepairFlask")
                .post(body)
                .build();
        return request;
    }
}
