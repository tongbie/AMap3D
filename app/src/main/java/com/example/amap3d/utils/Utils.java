package com.example.amap3d.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.amap3d.MainActivity;
import com.example.amap3d.datas.Datas;
import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Created by BieTong on 2018/4/7.
 */

public class Utils {
    public static OkHttpClient client;
    public static Gson gson;

    static {
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    public static void uiToast(final String text) {
        MainActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.getInstance(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static float dp(Context context, float px) {
        float scale = context.getResources().getDisplayMetrics().density;
        return px * scale + 0.5f;
    }

    public static boolean checkNetworkState(Context context) {
        boolean flag = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        if (connectivityManager.getActiveNetworkInfo() != null) {
            flag = connectivityManager.getActiveNetworkInfo().isAvailable();
        }
        return flag;
    }

    public static void hideTitleBar(Activity activity) {
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static boolean isLogin() {
        boolean isLogin = false;
        if (Datas.userInfo.getUserName() != null && !Datas.userInfo.getUserName().equals("")) {
            isLogin = true;
        }
        return isLogin;
    }

    public static void clearUserInfo() {
        Datas.userInfo.setUserName(null);
        Datas.userInfo.setDisplayName(null);
        Datas.userInfo.setTime(0);
    }
}
