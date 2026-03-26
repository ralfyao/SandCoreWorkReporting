package com.yjfcasting.app.sandcoreworkreporting;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
import com.yjfcasting.app.sandcoreworkreporting.vo.ParamValue;
import com.yjfcasting.app.sandcoreworkreporting.vo.ParamValueResoponse;
import com.yjfcasting.app.sandcoreworkreporting.vo.SandCoreMoldDriveRes;
import com.yjfcasting.app.sandcoreworkreporting.vo.WareHouseStock;
import com.yjfcasting.app.sandcoreworkreporting.vo.WareHouseStockRes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
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
    private static int TEXT_SIZE = 25;
    private SnadCoreModel model = null;
    private static ArrayList<String> dataList = new ArrayList<>();// 製令列表
    private final OkHttpClient okHttpClient = new OkHttpClient().newBuilder().addInterceptor(
                    new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)
            ).connectTimeout(150, TimeUnit.SECONDS) // 連線超時
            .writeTimeout(150, TimeUnit.SECONDS)   // 傳送資料超時
            .readTimeout(300, TimeUnit.SECONDS)
            .build();
    private SwipeRefreshLayout swipeRefreshLayout;
    private static ArrayList<String> workOrderList = new ArrayList<>();
    private int CHECKBOX_SIZE = 45;
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

            // 1. 建立網址
            String url = "https://mail.yjfcasting.com:8013/driver_activity?WorkerNumber="+reportWorkingNumber.trim();

            // 2. 建立 Intent
            Intent intent2 = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            // 3. 啟動外部瀏覽器
            startActivity(intent2);
        }
//        model = new SnadCoreModel();
//        GetData();
//        swipeRefreshLayout = findViewById(R.id.swipe_layout);
//        View scrollView = findViewById(R.id.scrollView2);
//        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                swipeRefreshLayout.setRefreshing(true);
//                // 執行刷新邏輯
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        GetData();
//                        Toast.makeText(DriverActivity.this, "資料已重新整理", Toast.LENGTH_SHORT).show();
//                        swipeRefreshLayout.setRefreshing(false); // 停止動畫
//                    }
//                }, 1500); // 模擬 1.5 秒
//            }
//        });
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
                workOrderList.clear();
                if (res != null && res.WorkStatus.equals("OK")) {
                    ArrayList<String> dataContainer = new ArrayList<>();
                    for (int i = 0; i < res.resultList.size(); i++) {
                        dataContainer = new ArrayList<>();
                        dataContainer.add(res.resultList.get(i).WorkOrder + "\r\n");
                        dataContainer.add(res.resultList.get(i).Seqnence + "\r\n");
//                        dataContainer.add(res.resultList.get(i).CustomerName + "\r\n");
                        dataContainer.add(res.resultList.get(i).ItemNo + "\r\n");
                        dataContainer.add(res.resultList.get(i).ItemDesc + "\r\n");
//                        dataContainer.add(res.resultList.get(i).SandCoreLocation + "\r\n");
//                        dataContainer.add(res.resultList.get(i).Destination + "\r\n");
                        dataContainer.add(res.resultList.get(i).CompleteDateTime + "\r\n");
                        dataContainer.add(res.resultList.get(i).ReturnDateTime + "\r\n");
                        workOrderList.add(res.resultList.get(i).WorkOrder);

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
//        columns.add("客戶名稱\r\n");
        columns.add("品號\r\n");
        columns.add("品名\r\n");
//        columns.add("起始區\r\n");
//        columns.add("迄點區\r\n");
        columns.add("到站\r\nComplete");
        columns.add("歸還\r\nReturn");
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
                Toast.makeText(DriverActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    private String mKey = "";
    private String mWorkOrder = "";
    @NonNull
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
            textView.setTextSize(12);
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
                String WorkOrder = gradingData.get(i).get(0).replace("\r\n","");
                String ItemNo = gradingData.get(i).get(3).replace("\r\n","");
                String CompleteDateTime = gradingData.get(i).get(4).replace("\r\n","");
                if (j < gradingData.get(0).size() - 2 ){//一般資料
                    TextView textView = new TextView(this);
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextSize(9);
                    textView.setTypeface(typeface);
                    textView.setBackgroundResource(R.drawable.cell_border);
                    textView.setPadding(0, 20, 0, 10);
                    textView.setText(gradingData.get(i).get(j));
                    textView.setTextColor(Color.BLACK);
                    if (i == 0) {
                        textView.setBackgroundColor(Color.parseColor( "#aeaeae"));
                    }
                    cardView.addView(textView);
                } else {//到站、歸還按鈕
                    // 到站按鈕
                    if (j == gradingData.get(0).size() - 2) {
                        Button btnReceive = new Button(DriverActivity.this);

                        btnReceive.setText("到站");
                        // 按下到站按鈕，跳出到站確認畫面
                        btnReceive.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // 準備要送出的Data
                                String[] workOrderArr = workOrderList.get(index - 1).split("-");
                                Log.d("WorkOrderHead", workOrderArr[0]);
                                Log.d("WorkOrder", workOrderArr[1]);
                                mWorkOrder =  workOrderList.get(index - 1);
                                LinearLayout layOut = new LinearLayout(DriverActivity.this);
                                HashMap<String, ArrayList<ParamValue>> paramList = new HashMap<>();
                                Request req = model.GetBRParamValueReq(workOrderArr[0], workOrderArr[1]);
                                GetWareHouseStockerList();
                                // 呼叫API
                                Call call = okHttpClient.newCall(req);
                                // 處理並顯示到站要確認的模具
                                call.enqueue(new Callback() {
                                    @Override
                                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                        processException(v, e);
                                    }
                                    private ArrayList<ParamValue> lLst = null;
                                    @Override
                                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                       try{
                                            ((DriverActivity) v.getContext()).runOnUiThread(() ->{
                                                ResponseBody responseBody = response.body();
                                                ParamValueResoponse res = new ParamValueResoponse();
                                                if (responseBody != null) {
                                                    String jsonString = null;  // 只能讀一次
                                                    try {
                                                        jsonString = responseBody.string();
                                                    } catch (IOException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                    res = new Gson().fromJson(jsonString, ParamValueResoponse.class);
//                                                    Log.d("keySet",  res.resultDict.keySet());
                                                    TableLayout.LayoutParams tableLayoutParams = new TableLayout.LayoutParams(
                                                            MATCH_PARENT,
                                                            MATCH_PARENT
                                                    );
                                                    TableLayout tableLayout = new TableLayout(DriverActivity.this);
                                                    tableLayout.setLayoutParams(tableLayoutParams);
                                                    TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams();
                                                    tableRowParams.weight = 1;
                                                    int index = 0;
                                                    for(String key : res.resultDict.keySet())
                                                    {
                                                        // 模具ID
                                                        Log.d("ID", key);
                                                        mKey = key;
                                                        String[] lData = key.split(",");
                                                        TextView lDTxt = new TextView(DriverActivity.this);
                                                        lDTxt.setText(lData[1]+"：");
                                                        lDTxt.setTextSize(TEXT_SIZE);
                                                        // 第一列：ID
                                                        TableRow tableRow = new TableRow(DriverActivity.this);
                                                        CardView cardView1 = new CardView(DriverActivity.this);
                                                        cardView1.addView(lDTxt);
                                                        CardView cardView2= new CardView(DriverActivity.this);
                                                        tableRow.addView(cardView1);
                                                        tableRow.addView(cardView2);

                                                        lLst = res.resultDict.get(key);
                                                        // 其他列：模具各配件與參數
                                                        String _kKey = "";
                                                        ParamValue nonFinalpStocker = null;
                                                        for(int i = 0; i < lLst.size(); i++){
                                                            if (lLst.get(i).ParamSubName.equals("儲位")){
                                                                nonFinalpStocker = lLst.get(i);
                                                                break;
                                                            }
                                                        }
                                                        final ParamValue pStocker  = nonFinalpStocker;
                                                        for(int i = 0; i < lLst.size(); i++){
//                                                            TableRow row_2 = new TableRow(DriverActivity.this);
                                                            ParamValue p = lLst.get(i);
                                                            if (p.ParamSubName.equals("位置"))
                                                                continue;
                                                            final TextView tt = new TextView(v.getContext());
                                                            tt.setText(p.ParamSubName);
                                                            tt.setTextSize(TEXT_SIZE);
                                                            CardView cView1 = new CardView(v.getContext());
                                                            cView1.addView(tt);
                                                            tableRow.addView(cView1);
//                                                            row_2.addView(cView1);
                                                            // 倉庫儲位為下拉選單
                                                            if (p.ParamSubName.equals("倉庫") || p.ParamSubName.equals("儲位")){
                                                                if (p.ParamSubName.equals("倉庫")){
                                                                    Spinner spWareHouse = new Spinner(v.getContext());
                                                                    spWareHouse.setTag("row_"+p.ParamSubName+"_" + index);
                                                                    ParamValue finalNonFinalpStocker = nonFinalpStocker;
                                                                    spWareHouse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                                                        @Override
                                                                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                                                                            String selected = parent.getItemAtPosition(position).toString();
                                                                            ArrayList<String> stockerOptions = ((DriverActivity)v.getContext()).wareHouseStocker.get(selected);
                                                                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                                                                    v.getContext(),
                                                                                    android.R.layout.simple_list_item_1,
                                                                                    stockerOptions
                                                                            );
                                                                            adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                                                                            Object tag = parent.getTag();
                                                                            if (tag != null && tag.toString().indexOf("倉庫") != -1) {
                                                                                Spinner spStocker1 = null;
                                                                                String sTag = "";
                                                                                for(int i = 0; i < tableLayout.getChildCount(); i++){
                                                                                    TableRow row = (TableRow)tableLayout.getChildAt(i);
                                                                                    for(int j = 0; j < row.getChildCount(); j++){
                                                                                        CardView stag = (CardView)row.getChildAt(j);
                                                                                        if (stag != null){
                                                                                            if (stag.getChildAt(0) instanceof Spinner){
                                                                                                if (((Spinner)stag.getChildAt(0)).getTag().toString().indexOf("儲位") != -1){
//                                                                                                            ArrayList<String> stockerOptions = ((DriverActivity)parent.getContext()).wareHouseStocker.get()
//                                                                                                    ArrayAdapter<String> adapterLeft = new ArrayAdapter<>(
//                                                                                                            parent.getContext(),
//                                                                                                            android.R.layout.simple_list_item_1,
//                                                                                                            stockerOptions
//                                                                                                    );
                                                                                                    ((Spinner)stag.getChildAt(0)).setAdapter(adapter);
//                                                                                                    if (pStocker.ParamSubName.equals("儲位")) {
                                                                                                        int iPosition = adapter.getPosition(pStocker.ParamValue);
                                                                                                        if (iPosition >= 0) {
                                                                                                            ((Spinner) stag.getChildAt(0)).setSelection(iPosition);
                                                                                                        }
//                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }

                                                                        @Override
                                                                        public void onNothingSelected(AdapterView<?> parent) {

                                                                        }
                                                                    });
                                                                    _kKey = p.ParamValue;
                                                                    GetWareHouseStockerList(spWareHouse, p.ParamSubName, p.ParamValue, "");
                                                                    CardView cView2 = new CardView(DriverActivity.this);
                                                                    cView2.addView(spWareHouse);
                                                                    tableRow.addView(cView2);
                                                                } else if (p.ParamSubName.equals("儲位")){
                                                                    Spinner spStocker = new Spinner(v.getContext());
                                                                    spStocker.setTag("row_"+p.ParamSubName+"_"+ index);
                                                                    GetWareHouseStockerList(spStocker, p.ParamSubName, p.ParamValue, _kKey);
                                                                    CardView cView2 = new CardView(DriverActivity.this);
                                                                    cView2.addView(spStocker);
                                                                    tableRow.addView(cView2);
                                                                }
                                                            } else {
                                                                final EditText te = new EditText(DriverActivity.this);
                                                                te.setText(p.ParamValue);
                                                                te.setTextSize(TEXT_SIZE);
                                                                te.setTag("row_"+p.ParamSubName+"_"+index);
                                                                CardView cView2 = new CardView(DriverActivity.this);
                                                                cView2.addView(te);
                                                                tableRow.addView(cView2);
                                                            }
                                                            index++;
                                                        }
                                                        tableLayout.addView(tableRow);
                                                    }
                                                    layOut.addView(tableLayout);
                                                    AlertDialog.Builder receiveBuilder = new AlertDialog.Builder(DriverActivity.this);
                                                    receiveBuilder
                                                            .setTitle("到站")//.setView(layout)
                                                            .setView(layOut)
                                                            .setPositiveButton("到站",  (dialog1, which1) ->  ((DriverActivity) v.getContext()).runOnUiThread(
                                                                    () -> {
                                                                        String[] idArr = mKey.split(",");
                                                                        String mId = idArr[0];
                                                                        String moldName = "";
                                                                        HashMap<String, ArrayList<ParamValue>> mValue = new HashMap<>();
                                                                        ArrayList<ParamValue> objData = new ArrayList<>();
                                                                        for(int i = 0; i < tableLayout.getChildCount(); i++){
                                                                            View view = tableLayout.getChildAt(i);
                                                                            if (view instanceof TableRow){//每一列
                                                                                TableRow row = (TableRow)view;
                                                                                objData = new ArrayList<>();
                                                                                Log.d("row.getChildCount():",  Integer.toString(row.getChildCount()));
                                                                                for(int j = 0; j < row.getChildCount(); j++){//列裡面的欄位、輸入
                                                                                    View child = row.getChildAt(j);
                                                                                    // 抓模具名稱
                                                                                    if (j == 0){
                                                                                        if (child instanceof CardView){
                                                                                            CardView cView = (CardView) child;
                                                                                            if (cView != null){
                                                                                                TextView tv = (TextView) cView.getChildAt(0);
                                                                                                moldName = tv.getText().toString();
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                    if (child instanceof CardView){
                                                                                        CardView cardView = (CardView) child;
                                                                                        if (cardView != null){
                                                                                            View mView = cardView.getChildAt(0);
                                                                                            if (mView != null && mView.getTag() != null && mView.getTag().toString().indexOf("row_") != -1){
                                                                                                String[] strTagArr = mView.getTag().toString().split("_");
                                                                                                ParamValue pv = new ParamValue();
                                                                                                pv.ParamSubName = strTagArr[strTagArr.length - 2];
                                                                                                Log.d("view.Type", mView.getClass().getName());
                                                                                                Log.d("view.Tag", mView.getTag().toString());
                                                                                                Log.d("view instanceof EditText", Boolean.toString(mView.getClass().getName().equals("android.widget.EditText")));
                                                                                                Log.d("view instanceof Spinner", Boolean.toString(mView.getClass().getName().equals("android.widget.Spinner")));

                                                                                                if (mView instanceof EditText){
                                                                                                    pv.ParamValue = ((EditText)mView).getText().toString();
                                                                                                    Log.d("EditText pv.ParamValue :", pv.ParamValue );
                                                                                                }
                                                                                                if (mView instanceof Spinner){
                                                                                                    try {
                                                                                                        int selectedIndex = ((Spinner) mView).getSelectedItemPosition();
                                                                                                        pv.ParamValue = ((Spinner) mView).getItemAtPosition(selectedIndex).toString();
                                                                                                        Log.d("Spinner pv.ParamValue :", pv.ParamValue);
                                                                                                    } catch (Exception e) {
                                                                                                        Log.e("errerr", e.toString()+e.getStackTrace());
                                                                                                    }
                                                                                                }
                                                                                                objData.add(pv);
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                                mValue.put(mWorkOrder+","+ mId+","+moldName+","+reportWorkingNumber, objData);
                                                                            }
                                                                        }
                                                                        String objDataStr = new Gson().toJson(mValue);
                                                                        submitToStationRecord(objDataStr);
//                                                                        Toast.makeText(DriverActivity.this, "執行成功", Toast.LENGTH_LONG).show();
                                                                    }))
                                                            .setNegativeButton("取消", (dialog2, which2) -> {

                                                            })
                                                            .show();
                                                }
                                            });
                                        } catch (Exception e){
                                            Toast.makeText(DriverActivity.this, e+ Arrays.toString(e.getStackTrace()), Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });

                            }
                        });
                        btnReceive.setGravity(Gravity.CENTER);
                        cardView.addView(btnReceive);
                    }
                    // 歸還按鈕
                    if(!CompleteDateTime.equals("")) {
                        if (j == gradingData.get(0).size() - 1) {
                            Button btnReturn = new Button(DriverActivity.this);
                            btnReturn.setText("歸還");
                            btnReturn.setGravity(Gravity.CENTER);
                            btnReturn.setOnClickListener(
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            // 準備要送出的Data
                                            String[] workOrderArr = workOrderList.get(index - 1).split("-");
                                            Log.d("WorkOrderHead", workOrderArr[0]);
                                            Log.d("WorkOrder", workOrderArr[1]);
                                            mWorkOrder = workOrderList.get(index - 1);
                                            LinearLayout layOut = new LinearLayout(DriverActivity.this);
                                            HashMap<String, ArrayList<ParamValue>> paramList = new HashMap<>();
                                            Request req = model.GetBRParamValueReq(workOrderArr[0], workOrderArr[1]);
                                            GetWareHouseStockerList();
                                            // 呼叫API
                                            Call call = okHttpClient.newCall(req);
                                            // 處理並顯示到站要確認的模具
                                            call.enqueue(new Callback() {
                                                @Override
                                                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                                    processException(v, e);
                                                }

                                                private ArrayList<ParamValue> lLst = null;

                                                @Override
                                                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                                    try {
                                                        ((DriverActivity) v.getContext()).runOnUiThread(() -> {
                                                            ResponseBody responseBody = response.body();
                                                            ParamValueResoponse res = new ParamValueResoponse();
                                                            if (responseBody != null) {
                                                                String jsonString = null;  // 只能讀一次
                                                                try {
                                                                    jsonString = responseBody.string();
                                                                } catch (IOException e) {
                                                                    throw new RuntimeException(e);
                                                                }
                                                                res = new Gson().fromJson(jsonString, ParamValueResoponse.class);
//                                                    Log.d("keySet",  res.resultDict.keySet());
                                                                TableLayout.LayoutParams tableLayoutParams = new TableLayout.LayoutParams(
                                                                        MATCH_PARENT,
                                                                        MATCH_PARENT
                                                                );
                                                                TableLayout tableLayout = new TableLayout(DriverActivity.this);
                                                                tableLayout.setLayoutParams(tableLayoutParams);
                                                                TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams();
                                                                tableRowParams.weight = 1;
                                                                int index = 0;
                                                                for (String key : res.resultDict.keySet()) {
                                                                    // 模具ID
                                                                    Log.d("ID", key);
                                                                    mKey = key;
                                                                    String[] lData = key.split(",");
                                                                    TextView lDTxt = new TextView(DriverActivity.this);
                                                                    lDTxt.setText(lData[1] + "：");
                                                                    lDTxt.setTextSize(TEXT_SIZE);
                                                                    // 第一列：ID
                                                                    TableRow tableRow = new TableRow(DriverActivity.this);
                                                                    CardView cardView1 = new CardView(DriverActivity.this);
                                                                    cardView1.addView(lDTxt);
                                                                    CardView cardView2 = new CardView(DriverActivity.this);
                                                                    tableRow.addView(cardView1);
                                                                    tableRow.addView(cardView2);

                                                                    lLst = res.resultDict.get(key);
                                                                    // 其他列：模具各配件與參數
                                                                    String _kKey = "";
                                                                    ParamValue nonFinalpStocker = null;
                                                                    for (int i = 0; i < lLst.size(); i++) {
                                                                        if (lLst.get(i).ParamSubName.equals("儲位")) {
                                                                            nonFinalpStocker = lLst.get(i);
                                                                            break;
                                                                        }
                                                                    }
                                                                    final ParamValue pStocker = nonFinalpStocker;
                                                                    for (int i = 0; i < lLst.size(); i++) {
//                                                            TableRow row_2 = new TableRow(DriverActivity.this);
                                                                        ParamValue p = lLst.get(i);
                                                                        if (p.ParamSubName.equals("位置"))
                                                                            continue;
                                                                        final TextView tt = new TextView(v.getContext());
                                                                        tt.setText(p.ParamSubName);
                                                                        tt.setTextSize(TEXT_SIZE);
                                                                        CardView cView1 = new CardView(v.getContext());
                                                                        cView1.addView(tt);
                                                                        tableRow.addView(cView1);
//                                                            row_2.addView(cView1);
                                                                        // 倉庫儲位為下拉選單
                                                                        if (p.ParamSubName.equals("倉庫") || p.ParamSubName.equals("儲位")) {
                                                                            if (p.ParamSubName.equals("倉庫")) {
                                                                                Spinner spWareHouse = new Spinner(v.getContext());
                                                                                spWareHouse.setTag("row_" + p.ParamSubName + "_" + index);
                                                                                final boolean[] userTouched = {false};

                                                                                spWareHouse.setOnTouchListener(new View.OnTouchListener() {
                                                                                    @Override
                                                                                    public boolean onTouch(View v, MotionEvent event) {
                                                                                        userTouched[0] = true;
                                                                                        return false;
                                                                                    }
                                                                                });
                                                                                spWareHouse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                                                                    @Override
                                                                                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                                                                                        String selected = parent.getItemAtPosition(position).toString();
                                                                                        ArrayList<String> stockerOptions = ((DriverActivity) v.getContext()).wareHouseStocker.get(selected);
                                                                                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                                                                                v.getContext(),
                                                                                                android.R.layout.simple_list_item_1,
                                                                                                stockerOptions
                                                                                        );
                                                                                        adapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
                                                                                        Object tag = parent.getTag();
                                                                                        if (tag != null && tag.toString().indexOf("倉庫") != -1) {
                                                                                            Spinner spStocker1 = null;
                                                                                            String sTag = "";
                                                                                            for (int i = 0; i < tableLayout.getChildCount(); i++) {
                                                                                                TableRow row = (TableRow) tableLayout.getChildAt(i);
                                                                                                for (int j = 0; j < row.getChildCount(); j++) {
                                                                                                    CardView stag = (CardView) row.getChildAt(j);
                                                                                                    if (stag != null) {
                                                                                                        if (stag.getChildAt(0) instanceof Spinner) {
                                                                                                            if (((Spinner) stag.getChildAt(0)).getTag().toString().indexOf("儲位") != -1) {
//                                                                                                            ArrayList<String> stockerOptions = ((DriverActivity)parent.getContext()).wareHouseStocker.get()
//                                                                                                            ArrayAdapter<String> adapterLeft = new ArrayAdapter<>(
//                                                                                                                    parent.getContext(),
//                                                                                                                    android.R.layout.simple_list_item_1,
//                                                                                                                    stockerOptions
//                                                                                                            );
                                                                                                                ((Spinner) stag.getChildAt(0)).setAdapter(adapter);

//                                                                                                            if (p.ParamSubName.equals("儲位")) {
                                                                                                                int iPosition = adapter.getPosition(pStocker.ParamValue);
                                                                                                                if (iPosition >= 0) {
                                                                                                                    ((Spinner) stag.getChildAt(0)).setSelection(iPosition);
                                                                                                                }
//                                                                                                            }
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }

                                                                                    @Override
                                                                                    public void onNothingSelected(AdapterView<?> parent) {

                                                                                    }
                                                                                });
                                                                                _kKey = p.ParamValue;
                                                                                GetWareHouseStockerList(spWareHouse, p.ParamSubName, p.ParamValue, "");
                                                                                CardView cView2 = new CardView(DriverActivity.this);
                                                                                cView2.addView(spWareHouse);
                                                                                tableRow.addView(cView2);
                                                                            } else if (p.ParamSubName.equals("儲位")) {
                                                                                Spinner spStocker = new Spinner(v.getContext());
                                                                                spStocker.setTag("row_" + p.ParamSubName + "_" + index);
                                                                                GetWareHouseStockerList(spStocker, p.ParamSubName, p.ParamValue, _kKey);
                                                                                CardView cView2 = new CardView(DriverActivity.this);
                                                                                cView2.addView(spStocker);
                                                                                tableRow.addView(cView2);
                                                                            }
                                                                        } else {
                                                                            final EditText te = new EditText(DriverActivity.this);
                                                                            te.setText(p.ParamValue);
                                                                            te.setTextSize(TEXT_SIZE);
                                                                            te.setTag("row_" + p.ParamSubName + "_" + index);
                                                                            CardView cView2 = new CardView(DriverActivity.this);
                                                                            cView2.addView(te);
                                                                            tableRow.addView(cView2);
                                                                        }
                                                                        index++;
                                                                    }
                                                                    tableLayout.addView(tableRow);
                                                                }
                                                                layOut.addView(tableLayout);
                                                                AlertDialog.Builder receiveBuilder = new AlertDialog.Builder(DriverActivity.this);
                                                                receiveBuilder
                                                                        .setTitle("歸還")//.setView(layout)
                                                                        .setView(layOut)
                                                                        .setPositiveButton("歸還", (dialog1, which1) -> ((DriverActivity) v.getContext()).runOnUiThread(
                                                                                () -> {
                                                                                    String[] idArr = mKey.split(",");
                                                                                    String mId = idArr[0];
                                                                                    String moldName = "";
                                                                                    HashMap<String, ArrayList<ParamValue>> mValue = new HashMap<>();
                                                                                    ArrayList<ParamValue> objData = new ArrayList<>();
                                                                                    for (int i = 0; i < tableLayout.getChildCount(); i++) {
                                                                                        View view = tableLayout.getChildAt(i);
                                                                                        if (view instanceof TableRow) {//每一列
                                                                                            TableRow row = (TableRow) view;
                                                                                            objData = new ArrayList<>();
                                                                                            Log.d("row.getChildCount():", Integer.toString(row.getChildCount()));
                                                                                            for (int j = 0; j < row.getChildCount(); j++) {//列裡面的欄位、輸入
                                                                                                View child = row.getChildAt(j);
                                                                                                // 抓模具名稱
                                                                                                if (j == 0) {
                                                                                                    if (child instanceof CardView) {
                                                                                                        CardView cView = (CardView) child;
                                                                                                        if (cView != null) {
                                                                                                            TextView tv = (TextView) cView.getChildAt(0);
                                                                                                            moldName = tv.getText().toString();
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                                if (child instanceof CardView) {
                                                                                                    CardView cardView = (CardView) child;
                                                                                                    if (cardView != null) {
                                                                                                        View mView = cardView.getChildAt(0);
                                                                                                        if (mView != null && mView.getTag() != null && mView.getTag().toString().indexOf("row_") != -1) {
                                                                                                            String[] strTagArr = mView.getTag().toString().split("_");
                                                                                                            ParamValue pv = new ParamValue();
                                                                                                            pv.ParamSubName = strTagArr[strTagArr.length - 2];
                                                                                                            Log.d("view.Type", mView.getClass().getName());
                                                                                                            Log.d("view.Tag", mView.getTag().toString());
                                                                                                            Log.d("view instanceof EditText", Boolean.toString(mView.getClass().getName().equals("android.widget.EditText")));
                                                                                                            Log.d("view instanceof Spinner", Boolean.toString(mView.getClass().getName().equals("android.widget.Spinner")));

                                                                                                            if (mView instanceof EditText) {
                                                                                                                pv.ParamValue = ((EditText) mView).getText().toString();
                                                                                                                Log.d("EditText pv.ParamValue :", pv.ParamValue);
                                                                                                            }
                                                                                                            if (mView instanceof Spinner) {
                                                                                                                try {
                                                                                                                    int selectedIndex = ((Spinner) mView).getSelectedItemPosition();
                                                                                                                    pv.ParamValue = ((Spinner) mView).getItemAtPosition(selectedIndex).toString();
                                                                                                                    Log.d("Spinner pv.ParamValue :", pv.ParamValue);
                                                                                                                } catch (
                                                                                                                        Exception e) {
                                                                                                                    Log.e("errerr", e.toString() + e.getStackTrace());
                                                                                                                }
                                                                                                            }
                                                                                                            objData.add(pv);
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                            mValue.put(mWorkOrder + "," + mId + "," + moldName + "," + reportWorkingNumber, objData);
                                                                                        }
                                                                                    }
                                                                                    String objDataStr = new Gson().toJson(mValue);
                                                                                    submitToStationReturn(objDataStr);
//                                                                        Toast.makeText(DriverActivity.this, "執行成功", Toast.LENGTH_LONG).show();
                                                                                }))
                                                                        .setNegativeButton("取消", (dialog2, which2) -> {

                                                                        })
                                                                        .show();
                                                            }
                                                        });
                                                    } catch (Exception e) {
                                                        Toast.makeText(DriverActivity.this, e + Arrays.toString(e.getStackTrace()), Toast.LENGTH_LONG).show();
                                                    }
                                                }
                                            });

                                        }
                                    }
                            );
                            cardView.addView(btnReturn);
                        }
                    }
                }
                tableRow.addView(cardView, tableRowParams);
            }
            tableLayout.addView(tableRow, tableLayoutParams);
        }
        return tableLayout;
    }

    private void submitToStationReturn(String objDataStr) {
        FormBody body = new FormBody.Builder()
                .add("sendData", objDataStr)
                .build();
        Request request = Utility.composeRequest("/api/SendToStationReturn")
                .post(body)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->{
                    Toast.makeText(getParent(), e.toString()+e.getStackTrace(), Toast.LENGTH_LONG).show();
                });
//                ((DriverActivi7ty)this).runOnUiThread();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(()->{
                    Toast.makeText(DriverActivity.this, "歸還成功", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void submitToStationRecord(String objDataStr) {
        FormBody body = new FormBody.Builder()
                .add("sendData", objDataStr)
                .build();
        Request request = Utility.composeRequest("/api/SendToStationRec")
                .post(body)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->{
                    Toast.makeText(getParent(), e.toString()+e.getStackTrace(), Toast.LENGTH_LONG).show();
                });
//                ((DriverActivity)this).runOnUiThread();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(()->{
                    Toast.makeText(DriverActivity.this, "到站成功", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private HashMap<String, ArrayList<String>> wareHouseStocker;
    private void GetWareHouseStockerList() {
//        if (wareHouseStocker == null)
        wareHouseStocker = new HashMap<>();
        Request request = model.GetStockerList();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Toast.makeText(DriverActivity.this, "ERROR:"+e.toString()+e.getStackTrace(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try{
                    ((DriverActivity)DriverActivity.this).runOnUiThread(() ->{
                        ResponseBody responseBody = response.body();
                        WareHouseStockRes res = new WareHouseStockRes();
                        try {
                            if (responseBody != null) {
                                String jsonString = responseBody.string();  // 只能讀一次
                                res = new Gson().fromJson(jsonString, WareHouseStockRes.class);
                                for(WareHouseStock s : res.resultList){
                                    ArrayList<String> lst = new ArrayList<>();
                                    if (wareHouseStocker.containsKey(s.WareHouse)){
                                        lst = wareHouseStocker.get(s.WareHouse);
                                        lst.add(s.Stock);
                                    } else {
                                        lst.add(s.Stock);
                                        wareHouseStocker.put(s.WareHouse, lst);
                                    }
                                }
//                                Log.d("debug", "response.body().string():"+jsonString);
                            }
                            if (res.WorkStatus.equals("OK"))
                                ;
//                                Toast.makeText(DriverActivity.this, "執行成功", Toast.LENGTH_LONG).show();
                            else
                                Toast.makeText(DriverActivity.this, res.ErrorMsg, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(DriverActivity.this, e + Arrays.toString(e.getStackTrace()), Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e){
                    Toast.makeText(DriverActivity.this, e + Arrays.toString(e.getStackTrace()), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void GetWareHouseStockerList(Spinner spSPinner, String paramSubName, String paramValue, String theKey) {
//        wareHouseStocker = new HashMap<>();
        ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        spSPinner.setAdapter(emptyAdapter);
        if (paramSubName.equals("倉庫")){
            Object[] keys = this.wareHouseStocker.keySet().toArray();
            ArrayList<String> keySet = new ArrayList<>();
            for(Object key : keys){
                keySet.add(key.toString());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(DriverActivity.this, android.R.layout.simple_list_item_1, keySet);
            spSPinner.setAdapter(adapter);
            int position = adapter.getPosition(paramValue);
            if (position >= 0){
                spSPinner.setSelection(position);
            }
        }
        else if (paramSubName.equals("儲位")) {
            ArrayList<String> data = this.wareHouseStocker.get(theKey);
            if (data != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(DriverActivity.this, android.R.layout.simple_list_item_1, data);
                spSPinner.setAdapter(adapter);
                int position = adapter.getPosition(paramValue);
                if (position >= 0) {
                    spSPinner.setSelection(position);
                }
            }
        }
    }

    private void processException(View view, Exception e) {
        ((DriverActivity) view.getContext()).runOnUiThread(() ->{
            Toast.makeText(DriverActivity.this, "錯誤："+e.toString(), Toast.LENGTH_LONG).show();
        });
    }

    private void setReceivedReturnFlag(String workOrder, String itemNo, boolean actioned, String reportWorkingNumber, String location, String endLocation, String action, int recFlag) {
        Request request = model.UpdateReceiveCompFlag(workOrder, itemNo, actioned, reportWorkingNumber, location, endLocation, action, recFlag);
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