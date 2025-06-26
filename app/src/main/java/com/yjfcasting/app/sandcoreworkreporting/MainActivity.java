package com.yjfcasting.app.sandcoreworkreporting;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.yjfcasting.app.sandcoreworkreporting.model.SnadCoreModel;
import com.yjfcasting.app.sandcoreworkreporting.ui.login.LoginActivity;
import com.yjfcasting.app.sandcoreworkreporting.vo.SandcoreWorkOrderRes;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

public class MainActivity extends AppCompatActivity {
    private ArrayList<ArrayList<String>> gradingData = new ArrayList<ArrayList<String>>();
    private final OkHttpClient okHttpClient = new OkHttpClient().newBuilder().addInterceptor(
            new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)
    ).connectTimeout(150, TimeUnit.SECONDS) // 連線超時
    .writeTimeout(150, TimeUnit.SECONDS)   // 傳送資料超時
    .readTimeout(300, TimeUnit.SECONDS) .build();
//    private ArrayList<PROD_WorkOrderDispatch> data = new ArrayList<PROD_WorkOrderDispatch>();
    private static ArrayList<String> workOrderList = new ArrayList<>();// 製令列表
    private Timer mTimer = null;
    private SnadCoreModel model = null;
    private static String reportWorkingNumber = "";// 工號
    private static String flaskId = "";// 鐵斗編號
    private static String departmentNumber = "";// 部門代號
    private static String departmentType = "";// 部門別：造模、合模、砂心
    private static String departmentName = "";// 部門名稱
    private static HashMap hm = new HashMap();// 製令-鐵斗的對應物件
    private static boolean isManager = false;// 是否為系統管理者
    private AppBarConfiguration appBarConfiguration;
    private SwipeRefreshLayout swipeRefreshLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        this.setContentView(R.layout.activity_main);
        hm.clear();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setDrawerAndCustomActionBar();
        Intent intent = getIntent();
        if (intent != null) {
            reportWorkingNumber = intent.getStringExtra("employeecode");
            departmentNumber = intent.getStringExtra("deptcode");
            departmentType = intent.getStringExtra("depttype");
            departmentName = intent.getStringExtra("deptname");
            isManager = intent.getBooleanExtra("ismanager", false);
            Log.d("debug", "reportWorkingNumber:"+reportWorkingNumber);
            Log.d("debug", "departmentNumber:"+departmentNumber);
            Log.d("debug", "departmentType:"+departmentType);
            Log.d("debug", "departmentName:"+departmentName);
            Log.d("debug", "IsManager:"+isManager);
        }
        model = new SnadCoreModel();
        // 每隔5秒鐘抓取資料
//        mTimer = new Timer();
        GetData();
        swipeRefreshLayout = findViewById(R.id.swipe_layout);
        View scrollView = findViewById(R.id.scrollView2);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                // 執行刷新邏輯
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        GetData();
                        Toast.makeText(MainActivity.this, "資料已重新整理", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false); // 停止動畫
                    }
                }, 1500); // 模擬 1.5 秒
            }
        });
    }

    private void setDrawerAndCustomActionBar() {
        // set Drawer and Custom Action Bar
        androidx.appcompat.widget.Toolbar customToolbar = findViewById(R.id.custom_toolbar);
        setSupportActionBar(customToolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_settings,
                R.id.nav_profile
        ).setOpenableLayout(drawerLayout).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        try {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        } catch (NullPointerException np){
            Log.e("Error", np + Arrays.toString(np.getStackTrace()));
        }
        //设置左侧菜单
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            Log.d("sandcoreworkreporting", "navigationView.NavigationItemSelected");
            int id = item.getItemId();
            if (id == R.id.nav_home){
                Intent intent = new Intent(MainActivity.this, LoginActivity.class); // 改成你的目標 Activity
                startActivity(intent);
                finish(); // 如果你想結束 MainActivity 可加這行
                return true;
            }
            if (id == R.id.nav_profile){
                Intent intent = new Intent(MainActivity.this, DriverActivity.class);
                intent.putExtra("deptcode", departmentNumber);
                intent.putExtra("deptname", departmentName);
                intent.putExtra("employeecode", reportWorkingNumber);
                intent.putExtra("depttype", departmentType);
                intent.putExtra("ismanager", isManager);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            // 不使用預設的箭頭
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);

            // 使用預設的漢堡圖
            getSupportActionBar().setHomeButtonEnabled(true);

            ImageView customHamburgerIcon = customToolbar.findViewById(R.id.custom_hamburger_icon);
            TextView fragmentTitle = customToolbar.findViewById(R.id.fragment_title);

            if(destination.getId() == R.id.nav_home || destination.getId() == R.id.nav_settings || destination.getId()==R.id.nav_profile) {
                customHamburgerIcon.setImageResource(R.drawable.icon_menu_n);  // 漢堡圖示
            } else {
                customHamburgerIcon.setImageResource(R.drawable.icon_back_n);  // 返回圖示
            }

            // Set the title based on the current fragment
            CharSequence label = destination.getLabel();
            if (label != null) {
                fragmentTitle.setText(label);
            }
        });
        ImageView customHamburgerIcon = customToolbar.findViewById(R.id.custom_hamburger_icon);
        customHamburgerIcon.setOnClickListener(v -> {
            NavDestination currentDestination = navController.getCurrentDestination();
            if (currentDestination != null) {
                if (currentDestination.getId() == R.id.nav_home || currentDestination.getId() == R.id.nav_settings || currentDestination.getId()==R.id.nav_profile) {
                    // 開啟或關閉 drawer
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    } else {
                        drawerLayout.openDrawer(GravityCompat.START);
                    }
                } else {
                    // 返回上一個 Fragment
                    navController.navigateUp();
                }
            }
        });

    }

    private void GetData(){
        Request request = model.GetSandCoreList(departmentType, departmentName, isManager);
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SandCoreWorkingReport", e + Arrays.toString(e.getStackTrace()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    SandcoreWorkOrderRes res = null;
                    try {
                        ResponseBody responseBody = response.body();
                        if (responseBody != null) {
                            String jsonString = responseBody.string();  // 只能讀一次
                            res= new Gson().fromJson(jsonString, SandcoreWorkOrderRes.class);
                            Log.d("debug", "response.body().string():"+jsonString);
//                            Log.d("debug", "res.resultDict:"+res.resultDict);
                        }
//                        res = new Gson().fromJson(response.body().string(), SandcoreWorkOrderRes.class);
                    }
                    catch(Exception e)
                    {
                        Log.d("error", e + Arrays.toString(e.getStackTrace()));
                    }
                    gradingData.clear();
                    String sftStation = "";
//                    if (res.resultList.size() > 0)
                        sftStation = departmentType.length()>2?departmentType.substring(0, 2):departmentName.length()>2?departmentName.substring(0, 2):"";
                    ArrayList<String> columns = initColumns(sftStation);
                    gradingData.add(columns);
                    workOrderList.clear();
                    if (res != null && res.WorkStatus.equals("OK")) {
                        ArrayList<String> dataContainer = new ArrayList<>();
                        for (int i = 0; i < res.resultList.size(); i++) {
                            dataContainer = new ArrayList<>();
                            if (!isManager) { // 非管理者列表
                                switch(res.resultList.get(i).SftStation)
                                {
                                    case "砂心":
                                        dataContainer.add(res.resultList.get(i).SftStatus + "\r\n");                                            // 報工狀態
                                        dataContainer.add((i + 1) + "\r\n");                                                                    // 序號
                                        dataContainer.add(res.resultList.get(i).SandCorePlanStartDate + "\r\n");                                // 砂心預計生產日
                                        dataContainer.add(res.resultList.get(i).MoldingGroup + "\r\n");                                         // 外模組別
                                        dataContainer.add(res.resultList.get(i).MoldingPlanStartDate + "\r\n");                                 // 外模預計生產日
                                        dataContainer.add(res.resultList.get(i).AssemblingGroup + "\r\n");                                      // 合模組別
                                        dataContainer.add(res.resultList.get(i).ItemNo + "\r\n");                                               // 產品代號
                                        dataContainer.add(                                                                                      // 製令品名規格
                                                (departmentType.equals("砂心") ? res.resultList.get(i).SandCoreWorkOrder : res.resultList.get(i).WorkOrder)
                                                        + "\r\n" + (res.resultList.get(i).ItemDesc.length() > 9 ? res.resultList.get(i).ItemDesc.substring(0, 9) : res.resultList.get(i).ItemDesc));
                                        dataContainer.add((res.resultList.get(i).ThisWeekQuantity) + "\r\n");                                   // 本週數量
                                        dataContainer.add((res.resultList.get(i).UnitWeight) + "\r\n");                                         // 單重
                                        dataContainer.add(res.resultList.get(i).SandCoreLocation + "\r\n");                                     // 砂心存放位置
                                        break;
                                    case "造模":
                                        dataContainer.add(res.resultList.get(i).SftStatus + "\r\n");                                    // 報工狀態
                                        dataContainer.add((i + 1) + "\r\n");                                                            // 序號
                                        dataContainer.add(res.resultList.get(i).SandCoreEndDate + "\r\n");                              // 砂心完成日
                                        dataContainer.add(res.resultList.get(i).MoldingGroup + "\r\n");                                 // 外模組別
                                        dataContainer.add(res.resultList.get(i).MoldingPlanStartDate + "\r\n");                         // 外模預計生產日
                                        dataContainer.add(res.resultList.get(i).AssemblingGroup + "\r\n");                              // 合模組別
                                        dataContainer.add(res.resultList.get(i).ItemNo + "\r\n");                                       // 產品代號
                                        dataContainer.add(res.resultList.get(i).Material + "\r\n");                                     // 材質
//                                        dataContainer.add(res.resultList.get(i).FlaskId + "\r\n");                                      // 鐵斗
                                        dataContainer.add(                                                                              // 製令品名規格
                                                (departmentType.equals("砂心") ? res.resultList.get(i).SandCoreWorkOrder : res.resultList.get(i).WorkOrder)
                                                        + "\r\n" + (res.resultList.get(i).ItemDesc.length() > 9 ? res.resultList.get(i).ItemDesc.substring(0, 9) : res.resultList.get(i).ItemDesc));
                                        dataContainer.add((res.resultList.get(i).ThisWeekQuantity) + "\r\n");                           // 本週數量
                                        dataContainer.add((res.resultList.get(i).UnitWeight) + "\r\n");                                 // 單重
                                        dataContainer.add((res.resultList.get(i).DispatchStatus) + "\r\n");                             // 模具到站狀態
                                        break;
                                    case "合模":
                                        dataContainer.add(res.resultList.get(i).SftStatus + "\r\n");                                    // 報工狀態
                                        dataContainer.add((i + 1) + "\r\n");                                                            // 序號
                                        dataContainer.add(res.resultList.get(i).MoldingEndDate + "\r\n");                               // 外模完成日
                                        dataContainer.add(res.resultList.get(i).MoldingGroup + "\r\n");                                 // 外模組別
                                        dataContainer.add(res.resultList.get(i).AssemblingGroup + "\r\n");                              // 合模組別
                                        dataContainer.add(res.resultList.get(i).ItemNo + "\r\n");                                       // 產品代號
                                        dataContainer.add(res.resultList.get(i).Material + "\r\n");                                     // 材質
                                        dataContainer.add(res.resultList.get(i).BottomFlask + "\r\n");                                      // 鐵斗
                                        dataContainer.add(                                                                              // 製令品名規格
                                                (departmentType.equals("砂心") ? res.resultList.get(i).SandCoreWorkOrder : res.resultList.get(i).WorkOrder)
                                                        + "\r\n" + (res.resultList.get(i).ItemDesc.length() > 9 ? res.resultList.get(i).ItemDesc.substring(0, 9) : res.resultList.get(i).ItemDesc));
                                        dataContainer.add((res.resultList.get(i).ThisWeekQuantity) + "\r\n");                           // 本週數量
                                        dataContainer.add((res.resultList.get(i).UnitWeight) + "\r\n");                                 // 單重
                                        dataContainer.add((res.resultList.get(i).DispatchStatus) + "\r\n");                             // 模具到站狀態
                                        break;
                                    case "電爐":
                                        dataContainer.add(res.resultList.get(i).SftStatus + "\r\n");                                    // 報工狀態
                                        dataContainer.add((i + 1) + "\r\n");                                                            // 序號
                                        dataContainer.add(res.resultList.get(i).AssemblingEndDate + "\r\n");                            // 合模完成日
                                        dataContainer.add(res.resultList.get(i).AssemblingGroup + "\r\n");                              // 合模組別
                                        dataContainer.add(res.resultList.get(i).ItemNo + "\r\n");                                       // 產品代號
                                        dataContainer.add(res.resultList.get(i).Material + "\r\n");                                     // 材質
                                        dataContainer.add(                                                                              // 製令品名規格
                                                (departmentType.equals("砂心") ? res.resultList.get(i).SandCoreWorkOrder : res.resultList.get(i).WorkOrder)
                                                        + "\r\n" + (res.resultList.get(i).ItemDesc.length() > 9 ? res.resultList.get(i).ItemDesc.substring(0, 9) : res.resultList.get(i).ItemDesc));
                                        dataContainer.add((res.resultList.get(i).ThisWeekQuantity) + "\r\n");                           // 本週數量
                                        dataContainer.add((res.resultList.get(i).UnitWeight) + "\r\n");                                 // 單重

                                        dataContainer.add(res.resultList.get(i).BottomFlask + "\r\n");                                  // 鐵斗
                                        dataContainer.add((res.resultList.get(i).DispatchStatus) + "\r\n");                             // 模具到站狀態
                                        break;
                                }

                            } else { // 管理者列表
                                dataContainer.add(res.resultList.get(i).SftStation + "\r\n" + res.resultList.get(i).SftStatus);
                                dataContainer.add((i + 1) + "\r\n");
                                dataContainer.add(res.resultList.get(i).SandCorePlanStartDate + "\r\n");
                                dataContainer.add(res.resultList.get(i).MoldingGroup + "\r\n");
                                dataContainer.add(res.resultList.get(i).MoldingPlanStartDate + "\r\n");
                                dataContainer.add(res.resultList.get(i).AssemblingGroup + "\r\n");
                                dataContainer.add(res.resultList.get(i).ItemNo + "\r\n");
                                dataContainer.add(
                                        (departmentType == "砂心" ? res.resultList.get(i).SandCoreWorkOrder : res.resultList.get(i).WorkOrder)
                                                + "\r\n" + (res.resultList.get(i).ItemDesc.length() > 9 ? res.resultList.get(i).ItemDesc.substring(0, 9) : res.resultList.get(i).ItemDesc));
                                dataContainer.add((res.resultList.get(i).ThisWeekQuantity) + "\r\n");
                                dataContainer.add((res.resultList.get(i).UnitWeight) + "\r\n");
                                dataContainer.add(res.resultList.get(i).SandCoreLocation + "\r\n");
                            }
                            workOrderList.add((departmentType.equals("砂心") ? res.resultList.get(i).SandCoreWorkOrder : res.resultList.get(i).WorkOrder));
                            gradingData.add(dataContainer);
                            hm.put(res.resultList.get(i).WorkOrder, res.resultList.get(i).BottomFlask);
                        }
                   }
                    initGridViewWData(gradingData);
                }catch (Exception ex){
                    Log.e("SandCoreWorkingReport", ex + Arrays.toString(ex.getStackTrace()));
                }
            }
        });
    }

    private void initGridViewWData(ArrayList<ArrayList<String>> dataResult){
        runOnUiThread(() -> {
            try {
                LinearLayout parentLayout = findViewById(R.id.parentLayout);
                parentLayout.removeAllViews();
                parentLayout.addView(createTableLayout(gradingData.size(), gradingData.get(0).size()));
            }
            catch (Exception ex){
                Log.e("initGridViewWData error", ex.getMessage());
            }
        });
    }

    private ArrayList<String> initColumns(String SftStation){
        ArrayList<String> columns = new ArrayList<>();
        if (SftStation.isEmpty() || SftStation.equals("砂心")){
            columns.add("報工狀態\r\n");
            columns.add("序號\r\n");
            columns.add("砂心預計\r\n生產日");
            columns.add("外模組別\r\n");
            columns.add("外模預計\r\n生產日");
            columns.add("合模組別\r\n");
            columns.add("產品代號\r\n");
            columns.add("製令品名\r\n規格");
            columns.add("本週數量\r\n");
            columns.add("單重\r\n");
            columns.add("砂心存放\r\n位置");
        } else {
            switch(SftStation){
                case "造模":
                    columns.add("報工狀態\r\n");
                    columns.add("序號\r\n");
                    columns.add("砂心\r\n完成日");
                    columns.add("外模組別\r\n");
                    columns.add("外模預計\r\n生產日");
                    columns.add("合模組別\r\n");
                    columns.add("產品代號\r\n");
                    columns.add("材質\r\n");
//                    columns.add("鐵斗\r\n");
                    columns.add("製令品名\r\n規格");
                    columns.add("本週數量\r\n");
                    columns.add("單重\r\n");
                    columns.add("模具到站\r\n狀態");
                    break;
                case "合模":
                    columns.add("報工狀態\r\n");
                    columns.add("序號\r\n");
                    columns.add("外模\r\n完成日");
                    columns.add("外模組別\r\n");
                    columns.add("合模組別\r\n");
                    columns.add("產品代號\r\n");
                    columns.add("材質\r\n");
                    columns.add("鐵斗\r\n");
                    columns.add("製令品名\r\n規格");
                    columns.add("本週數量\r\n");
                    columns.add("單重\r\n");
                    columns.add("模具到站\r\n狀態");
                    break;
                case "電爐":
                case "熔解":
                    columns.add("報工狀態\r\n");
                    columns.add("序號\r\n");
                    columns.add("合模完成日\r\n");
                    columns.add("合模組別\r\n");
                    columns.add("產品代號\r\n");
                    columns.add("材質\r\n");
                    columns.add("製令品名\r\n規格");
                    columns.add("本週數量\r\n");
                    columns.add("單重\r\n");
                    columns.add("鐵斗\r\n");
                    columns.add("模具到站\r\n狀態");
                    break;
            }
//            columns.add("報工狀態\r\n");
//            columns.add("序號\r\n");
//            columns.add("砂心預計\r\n生產日");
//            if (!SftStation.equals("造模") && !SftStation.equals("合模"))
//                columns.add("外模組別\r\n");
//            columns.add(SftStation + "預計\r\n生產日");
//            if (!SftStation.equals("合模"))
//                columns.add("合模組別\r\n");
//            columns.add("產品代號\r\n");
//            columns.add("製令品名\r\n規格");
//            columns.add("本週數量\r\n");
//            columns.add("單重\r\n");
//            if (!SftStation.equals("合模"))
//                columns.add("砂心存放\r\n位置");
        }
        return columns;
    }

    private TableLayout createTableLayout(int rowCount, int columnCount) {
        // 1) Create a tableLayout and its params
        TableLayout.LayoutParams tableLayoutParams = new TableLayout.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
        );
        TableLayout tableLayout = new TableLayout(this);
        tableLayout.setLayoutParams(tableLayoutParams);

        // 2) create tableRow params
        TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams();
        tableRowParams.weight = 1;
        for (int i = 0; i < gradingData.size(); i++) {
            // 3) create tableRow
            final int index = i ;
            TableRow tableRow = new TableRow(this);
            if (!isManager) { // 管理者不可報工
                tableRow.setOnClickListener(v -> {
                    if (index == 0)
                        return;
                    // 定義Layout
                    // 建立一個垂直方向的 LinearLayout 作為容器
                    LinearLayout layout = new LinearLayout(MainActivity.this);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(50, 40, 50, 10); // 可選的 padding，讓 UI 看起來更舒服

                    // 工號
                    final EditText input = new EditText(MainActivity.this);
                    input.setHint("請輸入工號");
                    input.setText(reportWorkingNumber);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    layout.addView(input);
                    Log.d("DEBUG", "index:"+index);
                    if (hm.containsKey(workOrderList.get(index - 1))) {
                        flaskId = (String) hm.get(workOrderList.get(index - 1));
                    }
                    final EditText flaskInput = new EditText(MainActivity.this);
                    if (departmentType.equals("造模") || departmentType.equals("合模") || departmentName.indexOf("熔解") != -1) {// 造模、合模、澆注均需要可以改鐵斗號碼
                        flaskInput.setHint("請輸入鐵斗");
                        flaskInput.setText(flaskId);
                        flaskInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                        layout.addView(flaskInput);
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("報工")
                            .setMessage(
                                    (departmentType.equals("造模") || departmentType.equals("合模")) ? "製令編號：" + (workOrderList.get(index - 1)) + "，請輸入工號及鐵斗編號報工" : "製令編號：" + (workOrderList.get(index - 1)) + "，請輸入工號報工"
                            )
                            .setView(layout);
                    builder.setPositiveButton("報工", (dialog, which) -> {
                        try {
                            if (gradingData.get(index).get(0).indexOf("已完成") != -1) {
                                Toast.makeText(MainActivity.this, "已完成無法再報工", Toast.LENGTH_LONG).show();
                                return;
                            }
                            if (reportWorkingNumber == null || reportWorkingNumber.isEmpty()) {
                                reportWorkingNumber = input.getText().toString();
                            }
                            if (flaskId == null || flaskId.isEmpty()) {
                                flaskId = flaskInput.getText().toString();
                            }
                            if (!flaskId.equals(flaskInput.getText().toString())) {
                                AlertDialog.Builder flaskBuilder = new AlertDialog.Builder(MainActivity.this);
                                if (departmentType.indexOf("合模") != -1) {
                                    flaskBuilder.setTitle("報工")
                                            .setMessage("合模維護的下模編號與造模維護的編號不同，是否要更新?")
                                            .setPositiveButton("報工", (dialog1, which1) -> ((MainActivity) v.getContext()).runOnUiThread(
                                                    () -> {
                                                        String[] workOrderArr = workOrderList.get(index - 1).split("-");
                                                        Request request = model.UploadSfcData(workOrderArr, gradingData, index, input.getText().toString(), flaskInput.getText().toString());
                                                        Call call = okHttpClient.newCall(request);
                                                        call.enqueue(new Callback() {
                                                            @Override
                                                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                                                ((MainActivity) v.getContext()).runOnUiThread(
                                                                        () -> Toast.makeText(MainActivity.this, e.toString() + e.getStackTrace(), Toast.LENGTH_LONG).show()
                                                                );
                                                            }

                                                            @Override
                                                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                                                try {
                                                                    ((MainActivity) v.getContext()).runOnUiThread(
                                                                            () -> {
                                                                                ResponseBody responseBody = response.body();
                                                                                SandcoreWorkOrderRes res = new SandcoreWorkOrderRes();
                                                                                try {
                                                                                    if (responseBody != null) {
                                                                                        String jsonString = responseBody.string();  // 只能讀一次
                                                                                        res = new Gson().fromJson(jsonString, SandcoreWorkOrderRes.class);
//                                                                        Log.d("debug", "response.body().string():"+jsonString);
                                                                                    }
                                                                                    if (res.WorkStatus.equals("OK"))
                                                                                        Toast.makeText(MainActivity.this, "執行成功", Toast.LENGTH_LONG).show();
                                                                                    else
                                                                                        Toast.makeText(MainActivity.this, res.ErrorMsg, Toast.LENGTH_LONG).show();
                                                                                } catch (
                                                                                        Exception e) {
                                                                                    Toast.makeText(MainActivity.this, e+ Arrays.toString(e.getStackTrace()), Toast.LENGTH_LONG).show();
                                                                                }
                                                                            }
                                                                    );
                                                                    GetData();
                                                                } catch (
                                                                        Exception ex) {
                                                                    Log.e("ReportWorkError", ex + Arrays.toString(ex.getStackTrace()));
                                                                }
                                                            }
                                                        });
                                                    }
                                            ))
                                            .setNegativeButton("取消", (dialog2, which2) -> {

                                            }).show();
                                }
                            } else {
                                String[] workOrderArr = workOrderList.get(index - 1).split("-");
                                Request request = model.UploadSfcData(workOrderArr, gradingData, index, input.getText().toString(), flaskInput.getText().toString());
                                Call call = okHttpClient.newCall(request);
                                call.enqueue(new Callback() {
                                    @Override
                                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                        ((MainActivity) v.getContext()).runOnUiThread(
                                                () -> Toast.makeText(MainActivity.this, e.toString() + e.getStackTrace(), Toast.LENGTH_LONG).show()
                                        );
                                    }

                                    @Override
                                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                        try {
                                            ((MainActivity) v.getContext()).runOnUiThread(
                                                    () -> {
                                                        ResponseBody responseBody = response.body();
                                                        SandcoreWorkOrderRes res = new SandcoreWorkOrderRes();
                                                        try {
                                                            if (responseBody != null) {
                                                                String jsonString = responseBody.string();  // 只能讀一次
                                                                res = new Gson().fromJson(jsonString, SandcoreWorkOrderRes.class);
//                                                                        Log.d("debug", "response.body().string():"+jsonString);
                                                            }
                                                            if (res.WorkStatus.equals("OK"))
                                                                Toast.makeText(MainActivity.this, "執行成功", Toast.LENGTH_LONG).show();
                                                            else
                                                                Toast.makeText(MainActivity.this, res.ErrorMsg, Toast.LENGTH_LONG).show();
                                                        } catch (Exception e) {
                                                            Toast.makeText(MainActivity.this, e + Arrays.toString(e.getStackTrace()), Toast.LENGTH_LONG).show();
                                                        }
                                                    }
                                            );
                                            GetData();
                                        } catch (Exception ex) {
                                            Log.e("ReportWorkError", ex + Arrays.toString(ex.getStackTrace()));
                                        }
                                    }
                                });
                            }
                        } catch (Exception ex) {
                            ((MainActivity) v.getContext()).runOnUiThread(
                                    () -> Toast.makeText(MainActivity.this, ex + Arrays.toString(ex.getStackTrace()), Toast.LENGTH_LONG).show()
                            );
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
                });
            }
            // 填充表格
            for (int j = 0; j < gradingData.get(0).size(); j++) {
                // 4) create textView

                TextView textView = new TextView(this);
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(9);
                Typeface typeface = ResourcesCompat.getFont(this, R.font.my_font_bold);
                textView.setTypeface(typeface);
                textView.setBackgroundResource(R.drawable.cell_border);
                textView.setPadding(0, 20, 0, 10);
                textView.setText(gradingData.get(i).get(j));
                textView.setTextColor(Color.BLACK);
                // 合模如果造模完成後超過2天，show紅色字體
                Log.d("DEBUG:",gradingData.get(0).get(2));
                if (gradingData.get(0).get(2).indexOf("外模") != -1) {// 合模會show外模完成日，以此作為條件判斷
                    Date toDay = new Date();
                    if (i != 0) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                        Date moldCompDate = null;
                        try {
                            moldCompDate = sdf.parse(gradingData.get(i).get(2).toString());

                            Log.d("DEBUG:", toDay.toString());
                            Log.d("DEBUG:", moldCompDate.toString());
                            if (((toDay.getTime() - moldCompDate.getTime()) / (1000 * 60 * 60 * 24)) > 2){
                                if (gradingData.get(i).get(0).toString().indexOf("未進站") != -1) {
                                    textView.setTextColor(Color.RED);
                                }
                            }
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                if (i == 0) {
                    textView.setBackgroundColor(Color.parseColor( "#aeaeae"));
                } else {
                    if (j == 0) {
                        if (gradingData.get(i).get(j).indexOf("未進站") != -1) {
                            textView.setTextColor(Color.BLACK);
                            textView.setBackgroundColor(Color.parseColor("#00ff00"));
                        } else if (gradingData.get(i).get(j).indexOf("已完成") != -1) {
                            textView.setTextColor(Color.BLACK);
                            textView.setBackgroundColor(Color.YELLOW);
                        }  else {
                            textView.setTextColor(Color.WHITE);
                            textView.setBackgroundColor(Color.RED);
                        }
                    }
                }
                CardView cardView = new CardView(this);
                cardView.setPadding(10, 10, 10, 10);
                cardView.setRadius(15);

                cardView.setMinimumHeight(30);
                cardView.setMinimumWidth(30);
                cardView.addView(textView);
                tableRow.addView(cardView, tableRowParams);
            }
            tableLayout.addView(tableRow, tableLayoutParams);
        }
        return tableLayout;
    }

    /**
     * 左上角的菜单被点击时调用到
     */
    @Override
    public boolean onSupportNavigateUp()
    {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}