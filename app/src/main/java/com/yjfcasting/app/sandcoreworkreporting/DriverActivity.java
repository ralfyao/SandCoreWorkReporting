package com.yjfcasting.app.sandcoreworkreporting;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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
import com.yjfcasting.app.sandcoreworkreporting.vo.SandCoreMoldDriveRes;
import com.yjfcasting.app.sandcoreworkreporting.vo.SandcoreWorkOrderRes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

public class DriverActivity extends AppCompatActivity {

    private ArrayList<ArrayList<String>> gradingData = new ArrayList<ArrayList<String>>();
    private AppBarConfiguration appBarConfiguration;
    private static String departmentNumber = "";// 部門代號
    private static String departmentType = "";// 部門別：造模、合模、砂心
    private static String departmentName = "";// 部門名稱
    private static String reportWorkingNumber = "";// 工號
    private static boolean isManager = false;// 是否為系統管理者
    private Timer mTimer = null;
    private SnadCoreModel model = null;
    private static ArrayList<String> dataList = new ArrayList<>();// 製令列表
    private final OkHttpClient okHttpClient = new OkHttpClient().newBuilder().addInterceptor(
                    new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)
            ).connectTimeout(150, TimeUnit.SECONDS) // 連線超時
            .writeTimeout(150, TimeUnit.SECONDS)   // 傳送資料超時
            .readTimeout(300, TimeUnit.SECONDS) .build();
    private SwipeRefreshLayout swipeRefreshLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        this.setContentView(R.layout.activity_driver);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.driver), (v, insets) -> {
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
                        Toast.makeText(DriverActivity.this, "資料已重新整理", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false); // 停止動畫
                    }
                }, 1500); // 模擬 1.5 秒
            }
        });
        // 每隔5秒鐘抓取資料
//        mTimer = new Timer();
//        mTimer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                GetData();
//            }
//        }, 0, 5000);
    }
    private void GetData() {
        Request request = model.GetDriverSandCoreMoldList(reportWorkingNumber);
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SandCoreDriver", e + Arrays.toString(e.getStackTrace()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                SandCoreMoldDriveRes res = null;
                try {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        String jsonString = responseBody.string();  // 只能讀一次
                        res= new Gson().fromJson(jsonString, SandCoreMoldDriveRes.class);
                        Log.d("SandCoreDriver", "response.body().string():"+jsonString);
                    }
                }
                catch(Exception e)
                {
                    Log.d("error", e + Arrays.toString(e.getStackTrace()));
                }
                gradingData.clear();
                ArrayList<String> columns = initColumns();
                gradingData.add(columns);
                dataList.clear();
                if (res != null && res.WorkStatus.equals("OK")) {
                    ArrayList<String> dataContainer = new ArrayList<>();
                    for (int i = 0; i < res.resultList.size(); i++) {
                        dataContainer.add(res.resultList.get(i).WorkOrder + "\r\n");
                        dataContainer.add(res.resultList.get(i).Seqnence + "\r\n");
                        dataContainer.add(res.resultList.get(i).CustomerName + "\r\n");
                        dataContainer.add(res.resultList.get(i).ItemNo + "\r\n");
                        dataContainer.add(res.resultList.get(i).ItemDesc + "\r\n");
                        dataContainer.add(res.resultList.get(i).SandCoreLocation + "\r\n");
                        dataContainer.add(res.resultList.get(i).Destination + "\r\n");
                        dataContainer.add(res.resultList.get(i).CompleteDateTime + "\r\n");
                        dataContainer.add(res.resultList.get(i).ReturnDateTime + "\r\n");
                        gradingData.add(dataContainer);
                    }
                }
                initGridViewWData(gradingData);
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
                R.id.nav_settings
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
                Intent intent = new Intent(DriverActivity.this, LoginActivity.class); // 改成你的目標 Activity
                startActivity(intent);
                finish(); // 如果你想結束 MainActivity 可加這行
                return true;
            }
            if (id == R.id.nav_settings){
                Intent intent = new Intent(DriverActivity.this, MainActivity.class);
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
    private ArrayList<String> initColumns(){
        ArrayList<String> columns = new ArrayList<>();
        columns.add("製令號碼\r\n");
        columns.add("優先順序\r\n");
        columns.add("客戶名稱\r\n");
        columns.add("品號\r\n");
        columns.add("品名\r\n");
        columns.add("起始區\r\n");
        columns.add("迄點區\r\n");
        columns.add("到站\r\n");
        columns.add("歸還\r\n");
        return columns;
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
        ArrayList<String> title = gradingData.get(0);
        TableRow tableRow = new TableRow(this);
        for (int i = 0; i < title.size(); i++) {

            CardView cardView = new CardView(this);
            TextView textView = new TextView(this);
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(9);
            Typeface typeface = ResourcesCompat.getFont(this, R.font.my_font_bold);
            textView.setTypeface(typeface);
            textView.setBackgroundResource(R.drawable.cell_border);
            textView.setPadding(0, 20, 0, 10);
            textView.setText(title.get(i));
            textView.setTextColor(Color.BLACK);
            textView.setBackgroundColor(Color.parseColor( "#aeaeae"));

            cardView.setPadding(10, 10, 10, 10);
            cardView.setRadius(15);

            cardView.setMinimumHeight(30);
            cardView.setMinimumWidth(30);
            cardView.addView(textView);
            tableRow.addView(cardView, tableRowParams);
        }
        tableLayout.addView(tableRow, tableLayoutParams);
        for (int i = 1; i < gradingData.size(); i++) {
            // 3) create tableRow
            final int index = i ;
            tableRow = new TableRow(this);
            Typeface typeface = ResourcesCompat.getFont(this, R.font.my_font_bold);
            for (int j = 0; j < gradingData.get(0).size(); j++) {
                // 4) create textView
                CardView cardView = new CardView(this);
                cardView.setPadding(10, 10, 10, 10);
                cardView.setRadius(15);
                cardView.setMinimumHeight(30);
                cardView.setMinimumWidth(30);
                if (j < gradingData.get(0).size() - 2 ){
                    TextView textView = new TextView(this);
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextSize(9);
//                    Typeface typeface = ResourcesCompat.getFont(this, R.font.my_font_bold);
                    textView.setTypeface(typeface);
                    textView.setBackgroundResource(R.drawable.cell_border);
                    textView.setPadding(0, 20, 0, 10);
                    textView.setText(gradingData.get(i).get(j));
                    textView.setTextColor(Color.BLACK);
                    if (i == 0) {
                        textView.setBackgroundColor(Color.parseColor( "#aeaeae"));
                    }
                    cardView.addView(textView);
                } else {
                    if (j == gradingData.get(0).size() - 2) {
                        CheckBox cbReceive = new CheckBox(this);
                        cbReceive.setGravity(Gravity.CENTER);
                        cbReceive.setTextSize(9);
                        cbReceive.setText("到站/Receive");
                        cbReceive.setTypeface(typeface);
                        cbReceive.setBackgroundResource(R.drawable.cell_border);
                        Log.d("Flag 1", gradingData.get(i).get(gradingData.get(0).size() - 2) );
                        if (gradingData.get(i).get(gradingData.get(0).size() - 2) != null &&
                            !gradingData.get(i).get(gradingData.get(0).size() - 2).equals("") &&
                                !gradingData.get(i).get(gradingData.get(0).size() - 2).equals("\r\n") &&
                                gradingData.get(i).get(gradingData.get(0).size() - 2).indexOf("null") == -1){
                            cbReceive.setChecked(true);
                        } else {
                            cbReceive.setChecked(false);
                        }
                        String WorkOrder = gradingData.get(i).get(0).replace("\r\n","");
                        cbReceive.setOnClickListener(v ->{
                            int recFlag = cbReceive.isChecked()?1:0;
                            setReceivedReturnFlag(WorkOrder, "REC", recFlag);
//                            Log.d("WorkOrder", )
                        });
                        cardView.addView(cbReceive);
                    }
                    if (j == gradingData.get(0).size() - 1) {
                        CheckBox cbReturn = new CheckBox(this);
                        cbReturn.setGravity(Gravity.CENTER);
                        cbReturn.setTextSize(9);
                        cbReturn.setTypeface(typeface);
                        cbReturn.setBackgroundResource(R.drawable.cell_border);
                        Log.d("Flag 2", gradingData.get(i).get(gradingData.get(0).size() - 1) );
                        if (gradingData.get(i).get(gradingData.get(0).size() - 1) != null &&
                           !gradingData.get(i).get(gradingData.get(0).size() - 1).equals("")  &&
                                !gradingData.get(i).get(gradingData.get(0).size() - 1).equals("\r\n") &&
                                gradingData.get(i).get(gradingData.get(0).size() - 1).indexOf("null") == -1){
                            cbReturn.setChecked(true);
                        } else {
                            cbReturn.setChecked(false);
                        }
                        cbReturn.setText("歸還/Return");
                        String WorkOrder = gradingData.get(i).get(0).replace("\r\n","");
                        cbReturn.setOnClickListener(v ->{
                            int retFlag = cbReturn.isChecked()?1:0;
                            setReceivedReturnFlag(WorkOrder, "RET", retFlag);
                        });
                        cardView.addView(cbReturn);
                    }
                }
                tableRow.addView(cardView, tableRowParams);
            }
            tableLayout.addView(tableRow, tableLayoutParams);
        }
        return tableLayout;
    }

    private void setReceivedReturnFlag(String workOrder, String action, int recFlag) {
        Request request = model.UpdateReceiveCompFlag(workOrder, action, recFlag);
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        String jsonString = responseBody.string();  // 只能讀一次
//                        res= new Gson().fromJson(jsonString, SandCoreMoldDriveRes.class);
                        Log.d("SandCoreDriver", "response.body().string():"+jsonString);
                    }
                }
                catch(Exception e)
                {
                    Log.d("error", e + Arrays.toString(e.getStackTrace()));
                }
            }
        });
    }

}