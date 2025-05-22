package com.yjfcasting.app.sandcoreworkreporting.ui.login;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import okhttp3.Callback;
import okhttp3.OkHttpClient;

import com.google.gson.Gson;
import com.yjfcasting.app.sandcoreworkreporting.MainActivity;
import com.yjfcasting.app.sandcoreworkreporting.databinding.ActivityLoginBinding;
import com.yjfcasting.app.sandcoreworkreporting.model.LoginModel;
import com.yjfcasting.app.sandcoreworkreporting.vo.LoginRes;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginModel model;
    private OkHttpClient okHttpClient = new OkHttpClient().newBuilder().addInterceptor(
            new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)
    ).build();
    private ProgressBar progressBar = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final EditText usernameEditText = binding.username;
        progressBar = binding.loadingSpinner;
        final Button loginButton = binding.login;
        model = new LoginModel();
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (usernameEditText.getText().toString().trim().equals("")){
                    Toast.makeText(LoginActivity.this, "請輸入工號/Please Input Wokrer Number", Toast.LENGTH_LONG).show();
                    return;
                }
                showLoading(true);
                try {
                    Request request = model.Login(usernameEditText.getText().toString().trim());
                    Call call = okHttpClient.newCall(request);
                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {

                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            LoginRes responseObj = new Gson().fromJson(response.body().string(), LoginRes.class);
                            Log.d("LoginActivity", responseObj.WorkStatus);
                            if (responseObj.WorkStatus.equals("OK")) {
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.putExtra("deptcode", responseObj.result.DeptCode);
                                intent.putExtra("deptname", responseObj.result.DeptName);
                                intent.putExtra("employeecode", responseObj.result.EmployeeCode);
                                intent.putExtra("empname", responseObj.result.EmployeeName);
                                intent.putExtra("depttype", responseObj.result.DeptType);
                                intent.putExtra("ismanager", responseObj.result.IsManager);
                                startActivity(intent);
                            } else {
                                if (responseObj.WorkStatus.equals("Fail")){
                                    runOnUiThread(() -> {
                                                    Toast.makeText(LoginActivity.this, "系統發生問題，請洽資訊人員", Toast.LENGTH_LONG).show();
                                    });
                                }
                                if (responseObj.WorkStatus.equals("NG")){
                                    runOnUiThread(() -> {
                                                    Toast.makeText(LoginActivity.this, responseObj.ErrorMsg, Toast.LENGTH_LONG).show();
                                                });
                                }
                            }
                        }
                    });
                    showLoading(false);
                }
                catch(Exception ex)
                {
                    ((LoginActivity)v.getContext()).runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, ex.toString()+ex.getStackTrace(), Toast.LENGTH_LONG).show();
                                }
                            }
                    );
                    showLoading(false);
                    return;
                }
            }
        });
    }
    private void showLoading(boolean show){
        if (progressBar != null)  {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}