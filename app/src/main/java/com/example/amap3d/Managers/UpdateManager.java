package com.example.amap3d.Managers;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.example.amap3d.Gsons.ApkVersionGson;
import com.example.amap3d.MainActivity;
import com.example.amap3d.R;
import com.example.amap3d.Services.DownloadService;
import com.example.amap3d.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.Context.BIND_AUTO_CREATE;
import static android.content.Context.NOTIFICATION_SERVICE;


/**
 * Created by BieTong on 2018/5/7.
 */

public class UpdateManager {
    private ServiceConnection conn;

    private static String updateDescription;
    private static String versionCodeURL = "http://bus.mysdnu.cn/android/update/:type";

    public static String downloadApkURL = "http://bus.mysdnu.cn/android/latest/:type";
    public static String downloadPathName ="SchoolBusQuery";

    public static final int UPDATE_NOT_NEED = 0;
    public static final int UPDATA_CLIENT = 1;
    public static final int UPDATE_FORCE = 2;
    public static final int UPDATE_LOCAL_VERSION_ERROR = 3;
    public static final int UPDATE_SERVICE_VERSION_ERROR = 4;

    public static int isNeedUpdate(final Context context) {
        final int[] versionState = {0};
        Request request = new Request.Builder()
                .url(versionCodeURL)
                .build();
        Response response = null;
        try {
            response = Utils.client.newCall(request).execute();
            String responseData = response.body().string();
            String responseCode = String.valueOf(response.code());
            int versionCode = -1;
            if (responseData != null && responseCode.charAt(0) == '2') {
                try {
                    versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
                } catch (PackageManager.NameNotFoundException e) {
                    return UPDATE_LOCAL_VERSION_ERROR;
                }
                ApkVersionGson apkVersionGson = Utils.gson.fromJson(responseData, ApkVersionGson.class);
                int minVersionCode = apkVersionGson.getMinVersionCode();
                int packageVersionCode = apkVersionGson.getPackageVersionCode();
                String updateType = apkVersionGson.getType() == null ? "" : apkVersionGson.getType();
                if (minVersionCode == -404 || packageVersionCode == -404) {
                    return UPDATE_SERVICE_VERSION_ERROR;
                }
                //TODO:updateType
                if (versionCode < minVersionCode /*|| updateType.equals("updateType")*/) {
                    versionState[0] = UPDATE_FORCE;
                    updateDescription = "更新时间:" + apkVersionGson.getUpdateTime() == null ? "暂无" : apkVersionGson.getUpdateTime()
                            + "\n更新日志：\n" + apkVersionGson.getDescription() == null ? "暂无" : apkVersionGson.getDescription();
                } else if (versionCode < /*packageVersionCode*/9) {
                    versionState[0] = UPDATA_CLIENT;
                    updateDescription = "更新时间:" + apkVersionGson.getUpdateTime() == null ? "暂无" : apkVersionGson.getUpdateTime()
                            + "\n更新日志：\n" + apkVersionGson.getDescription() == null ? "暂无" : apkVersionGson.getDescription();
                } else {
                    versionState[0] = UPDATE_NOT_NEED;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionState[0];
    }

    public void dealWithUpdateState(int versionCode) {
        switch (versionCode) {
            case UPDATE_NOT_NEED:
                break;
            case UPDATA_CLIENT:
                showUpdataDialog(false);
                break;
            case UPDATE_FORCE:
                showUpdataDialog(true);
                break;
            case UPDATE_LOCAL_VERSION_ERROR:
                Utils.uiToast("应用版本信息获取失败");
                break;
            case UPDATE_SERVICE_VERSION_ERROR:
                Utils.uiToast("服务器版本信息获取失败");
                break;
            default:
        }
    }

    private void showUpdataDialog(final boolean isForceUpdate) {
        final AlertDialog.Builder builer = new AlertDialog.Builder(Utils.getMainActivity());
        builer.setTitle(isForceUpdate ? "有必须的更新" : "有可用的更新");
        builer.setCancelable(!isForceUpdate);
        builer.setMessage(updateDescription);
        builer.setPositiveButton(isForceUpdate ? "退出" : "忽略", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (isForceUpdate) {
                    Utils.getMainActivity().finish();
                    return;
                }
            }
        });
        builer.setNegativeButton("升级", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                downloadApkWithService();
            }
        });
        Utils.getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = builer.create();
                dialog.show();
            }
        });
    }

    private void downloadApkWithService() {
        conn = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        Utils.getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Utils.getMainActivity(), DownloadService.class);
                Utils.getMainActivity().startService(intent);
            }
        });
    }
}






