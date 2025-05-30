package com.yjfcasting.app.sandcoreworkreporting;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
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

import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.yjfcasting.app.sandcoreworkreporting.model.SnadCoreModel;
import com.yjfcasting.app.sandcoreworkreporting.ui.login.LoginActivity;
import com.yjfcasting.app.sandcoreworkreporting.vo.PROD_WorkOrderDispatch;
import com.yjfcasting.app.sandcoreworkreporting.vo.SandcoreWorkOrderRes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

public class MainActivity extends AppCompatActivity {
    private ArrayList<ArrayList<String>> gradingData = new ArrayList<ArrayList<String>>();
    private OkHttpClient okHttpClient = new OkHttpClient().newBuilder().addInterceptor(
            new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)
    ).connectTimeout(150, TimeUnit.SECONDS) // 連線超時
    .writeTimeout(150, TimeUnit.SECONDS)   // 傳送資料超時
    .readTimeout(300, TimeUnit.SECONDS) .build();
    private ArrayList<PROD_WorkOrderDispatch> data = new ArrayList<PROD_WorkOrderDispatch>();
    private static ArrayList<String> workOrderList = new ArrayList<>();;// 製令列表
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        hm.clear();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // set Drawer and Custom Action Bar
        androidx.appcompat.widget.Toolbar customToolbar = findViewById(R.id.custom_toolbar);
        setSupportActionBar(customToolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        appBarConfiguration = new AppBarConfiguration.Builder(
            R.id.nav_home,
            R.id.nav_settings
        ).setOpenableLayout(drawerLayout).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

//        drawerLayout.closeDrawer(GravityCompat.START);
        //设置左侧菜单
        NavigationView navigationView = findViewById(R.id.navigation_view);
//        navigationView.setNavigationItemSelectedListener(item -> {
//            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
//            if (handled) {
//                drawerLayout.closeDrawer(GravityCompat.START);  // <- 手動關閉 Drawer
//            }
//            return handled;
//        });
        NavigationUI.setupWithNavController(navigationView, navController);

        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener()
        {
            @Override
            public void onDestinationChanged( @NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments)
            {
                // 不使用預設的箭頭
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);

                // 不使用預設的漢堡圖
                getSupportActionBar().setHomeButtonEnabled(false);

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
            }
        });

        ImageView customHamburgerIcon = customToolbar.findViewById(R.id.custom_hamburger_icon);
        customHamburgerIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

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
//        GetData();
        // 每隔5秒鐘抓取資料
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                GetData();
            }
        }, 0, 5000);
    }
    private void GetData(){
        Request request = model.GetSandCoreList(departmentType, departmentName, isManager);
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SandCoreWorkingReport", e.toString()+e.getStackTrace());
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
                        Log.d("error", e.toString()+e.getStackTrace());
                    }
                    gradingData.clear();
                    String sftStation = "";
//                    if (res.resultList.size() > 0)
                        sftStation = departmentType.length()>2?departmentType.substring(0, 2):departmentType;
                    ArrayList<String> columns = initColumns(sftStation);
                    gradingData.add(columns);
                    workOrderList.clear();
                    if (res != null && res.WorkStatus.equals("OK")) {
                        ArrayList<String> dataContainer = new ArrayList<>();
                        for (int i = 0; i < res.resultList.size(); i++) {
                            dataContainer = new ArrayList<>();
                            if (!isManager) { // 非管理者列表
                                dataContainer.add(res.resultList.get(i).SftStatus + "\r\n");
                                dataContainer.add(String.valueOf(i + 1) + "\r\n");
                                dataContainer.add(res.resultList.get(i).SandCorePlanStartDate + "\r\n");
                                if (!res.resultList.get(i).SftStation.equals("造模") && !res.resultList.get(i).SftStation.equals("合模"))
                                    dataContainer.add(res.resultList.get(i).MoldingGroup + "\r\n");
                                dataContainer.add(res.resultList.get(i).MoldingPlanStartDate + "\r\n");
                                if (!res.resultList.get(i).SftStation.equals("合模"))
                                    dataContainer.add(res.resultList.get(i).AssemblingGroup + "\r\n");
                                dataContainer.add(res.resultList.get(i).ItemNo + "\r\n");
                                dataContainer.add(
                                        (departmentType == "砂心" ? res.resultList.get(i).SandCoreWorkOrder : res.resultList.get(i).WorkOrder)
                                                + "\r\n" + (res.resultList.get(i).ItemDesc.length() > 9 ? res.resultList.get(i).ItemDesc.substring(0, 9) : res.resultList.get(i).ItemDesc));
                                dataContainer.add(String.valueOf(res.resultList.get(i).ThisWeekQuantity) + "\r\n");
                                dataContainer.add(String.valueOf(res.resultList.get(i).UnitWeight) + "\r\n");
                                if (!res.resultList.get(i).SftStation.equals("合模"))
                                    dataContainer.add(res.resultList.get(i).SandCoreLocation + "\r\n");
                            } else { // 管理者列表
                                dataContainer.add(res.resultList.get(i).SftStation + "\r\n" + res.resultList.get(i).SftStatus);
                                dataContainer.add(String.valueOf(i + 1) + "\r\n");
                                dataContainer.add(res.resultList.get(i).SandCorePlanStartDate + "\r\n");
                                dataContainer.add(res.resultList.get(i).MoldingGroup + "\r\n");
                                dataContainer.add(res.resultList.get(i).MoldingPlanStartDate + "\r\n");
                                dataContainer.add(res.resultList.get(i).AssemblingGroup + "\r\n");
                                dataContainer.add(res.resultList.get(i).ItemNo + "\r\n");
                                dataContainer.add(
                                        (departmentType == "砂心" ? res.resultList.get(i).SandCoreWorkOrder : res.resultList.get(i).WorkOrder)
                                                + "\r\n" + (res.resultList.get(i).ItemDesc.length() > 9 ? res.resultList.get(i).ItemDesc.substring(0, 9) : res.resultList.get(i).ItemDesc));
                                dataContainer.add(String.valueOf(res.resultList.get(i).ThisWeekQuantity) + "\r\n");
                                dataContainer.add(String.valueOf(res.resultList.get(i).UnitWeight) + "\r\n");
                                dataContainer.add(res.resultList.get(i).SandCoreLocation + "\r\n");
                            }
                            workOrderList.add((departmentType.equals("砂心") ? res.resultList.get(i).SandCoreWorkOrder : res.resultList.get(i).WorkOrder));
                            gradingData.add(dataContainer);
                            hm.put(res.resultList.get(i).WorkOrder, res.resultList.get(i).BottomFlask);
                        }
                   }
                    initGridViewWData(gradingData);
                }catch (Exception ex){
                    Log.e("SandCoreWorkingReport", ex.toString()+ex.getStackTrace());
                }
            }
        });
    }

    private void initGridViewWData(ArrayList<ArrayList<String>> dataResult){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    LinearLayout parentLayout = findViewById(R.id.parentLayout);
                    parentLayout.removeAllViews();
                    parentLayout.addView(createTableLayout(gradingData.size(), gradingData.get(0).size()));
                }
                catch (Exception ex){
                    Log.e("initGridViewWData error", ex.getMessage());
                }
            }
        });
    }

    private ArrayList<String> initColumns(String SftStation){
        ArrayList<String> columns = new ArrayList<>();
        if (SftStation.equals("") || SftStation.equals("砂心")){
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
            columns.add("報工狀態\r\n");
            columns.add("序號\r\n");
            columns.add("砂心預計\r\n生產日");
            if (!SftStation.equals("造模") && !SftStation.equals("合模"))
                columns.add("外模組別\r\n");
            columns.add(SftStation + "預計\r\n生產日");
            if (!SftStation.equals("合模"))
                columns.add("合模組別\r\n");
            columns.add("產品代號\r\n");
            columns.add("製令品名\r\n規格");
            columns.add("本週數量\r\n");
            columns.add("單重\r\n");
            if (!SftStation.equals("合模"))
                columns.add("砂心存放\r\n位置");
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
            tableRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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
                    if (hm.containsKey( workOrderList.get(index - 1))){
                        flaskId = (String) hm.get( workOrderList.get(index - 1));
                    }
                    final EditText flaskInput = new EditText(MainActivity.this);
                    if (departmentType.equals("造模") || departmentType.equals("合模"))
                    {
                        flaskInput.setHint("請輸入鐵斗");
                        flaskInput.setText(flaskId);
                        flaskInput.setInputType(InputType.TYPE_CLASS_NUMBER);
                        layout.addView(flaskInput);
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("報工")
                            .setMessage(
                                    (departmentType.equals("造模") || departmentType.equals("合模") )?   "製令編號："+( workOrderList.get(index - 1))+"，請輸入工號及鐵斗編號報工":   "製令編號："+( workOrderList.get(index - 1))+"，請輸入工號報工"
                            )
                            .setView(layout);
                    builder.setPositiveButton("報工", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                     try {
                                        if (gradingData.get(index).get(0).indexOf("已完成") != -1) {
                                            Toast.makeText(MainActivity.this, "已完成無法再報工", Toast.LENGTH_LONG).show();
                                            return;
                                        }
                                        if (reportWorkingNumber == null || reportWorkingNumber.equals("")){
                                            reportWorkingNumber = input.getText().toString();
                                        }
                                        if (flaskId == null || flaskId.equals("")){
                                            flaskId = flaskInput.getText().toString();
                                        }
                                        AtomicBoolean reportWorking = new AtomicBoolean(true);
                                        if (!flaskId.equals(flaskInput.getText().toString())){
                                            AlertDialog.Builder flaskBuilder = new AlertDialog.Builder(MainActivity.this);
                                            if (departmentType.indexOf("合模") != -1) {
                                                flaskBuilder.setTitle("報工")
                                                        .setMessage("合模維護的下模編號與造模維護的編號不同，是否要更新?")
                                                        .setPositiveButton("報工", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                ((MainActivity)v.getContext()).runOnUiThread(
                                                                        new Runnable() {
                                                                            @Override
                                                                            public void run() {
                                                                                String[] workOrderArr = workOrderList.get(index - 1).split("-");
                                                                                Request request = model.UploadSfcData(workOrderArr, gradingData, index, input.getText().toString(), flaskInput.getText().toString());
                                                                                Call call = okHttpClient.newCall(request);
                                                                                call.enqueue(new Callback() {
                                                                                    @Override
                                                                                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                                                                        ((MainActivity) v.getContext()).runOnUiThread(
                                                                                                new Runnable() {
                                                                                                    @Override
                                                                                                    public void run() {
                                                                                                        Toast.makeText(MainActivity.this, e.toString() + e.getStackTrace(), Toast.LENGTH_LONG).show();
                                                                                                    }
                                                                                                }
                                                                                        );
                                                                                    }

                                                                                    @Override
                                                                                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                                                                        try {
                                                                                            ((MainActivity) v.getContext()).runOnUiThread(
                                                                                                    new Runnable() {
                                                                                                        @Override
                                                                                                        public void run() {
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
                                                                                                                Toast.makeText(MainActivity.this, e.toString() + e.getStackTrace(), Toast.LENGTH_LONG).show();
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                            );
                                                                                            GetData();
                                                                                        } catch (Exception ex) {
                                                                                            Log.e("ReportWorkError", ex.toString() + ex.getStackTrace());
                                                                                        }
                                                                                    }
                                                                                });
                                                                            }
                                                                        }
                                                                );
                                                            }
                                                        })
                                                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {

                                                            }
                                                        }).show();;//.setTitle("合模維護的下模編號與造模維護的編號不同，是否要更新?");
                                            }
                                        } else {
                                            String[] workOrderArr = workOrderList.get(index - 1).split("-");
                                            Request request = model.UploadSfcData(workOrderArr, gradingData, index, input.getText().toString(), flaskInput.getText().toString());
                                            Call call = okHttpClient.newCall(request);
                                            call.enqueue(new Callback() {
                                                @Override
                                                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                                    ((MainActivity) v.getContext()).runOnUiThread(
                                                            new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    Toast.makeText(MainActivity.this, e.toString() + e.getStackTrace(), Toast.LENGTH_LONG).show();
                                                                }
                                                            }
                                                    );
                                                }

                                                @Override
                                                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                                    try {
                                                        ((MainActivity) v.getContext()).runOnUiThread(
                                                                new Runnable() {
                                                                    @Override
                                                                    public void run() {
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
                                                                            Toast.makeText(MainActivity.this, e.toString() + e.getStackTrace(), Toast.LENGTH_LONG).show();
                                                                        }
                                                                    }
                                                                }
                                                        );
                                                        GetData();
                                                    } catch (Exception ex) {
                                                        Log.e("ReportWorkError", ex.toString() + ex.getStackTrace());
                                                    }
                                                }
                                            });
                                        }
                                    } catch (Exception ex){
                                        ((MainActivity)v.getContext()).runOnUiThread(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(MainActivity.this, ex.toString()+ex.getStackTrace(), Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                        );
                                    }
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();;
                }
            });
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

    public void logout(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
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