package com.example.amap3d.Managers;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.example.amap3d.Gsons.ApkVersionGson;
import com.example.amap3d.Services.DownloadService;
import com.example.amap3d.Utils;

import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by BieTong on 2018/5/7.
 */

public class UpdateManager {
    private static String updateDescription;
    private static final String versionCodeURL = "http://bus.mysdnu.cn/android/update/:type";

    public static final String downloadApkURL = "http://bus.mysdnu.cn/android/latest/:type";
    public static final String downloadPathName = "SchoolBusQuery";

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
        Response response;
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
                if (minVersionCode == -404 || packageVersionCode == -404) {
                    return UPDATE_SERVICE_VERSION_ERROR;
                }
                //TODO:updateType
                if (versionCode < minVersionCode /*|| updateType.equals("updateType")*/) {
                    versionState[0] = UPDATE_FORCE;
                    updateDescription = "更新时间:" + apkVersionGson.getUpdateTime() == "" ? "暂无" : apkVersionGson.getUpdateTime()
                            + "\n更新日志：\n" + apkVersionGson.getDescription() == "" ? "暂无" : apkVersionGson.getDescription();
                } else if (versionCode < packageVersionCode) {
                    versionState[0] = UPDATA_CLIENT;
                    updateDescription = "更新时间:" + apkVersionGson.getUpdateTime() == "" ? "暂无" : apkVersionGson.getUpdateTime()
                            + "\n更新日志：\n" + apkVersionGson.getDescription() == "" ? "暂无" : apkVersionGson.getDescription();
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
        if (isServiceWorking(Utils.getApplicationContext(), "com.example.amap3d.Services.DownloadService")) {
            Utils.uiToast("更新下载中...");
            return;
        }
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

    public boolean isServiceWorking(Context context, String serviceName) {
        boolean isWork = false;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServiceInfoList = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (runningServiceInfoList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < runningServiceInfoList.size(); i++) {
            String mName = runningServiceInfoList.get(i).service.getClassName().toString();
            Log.e("Service", mName);
            if (mName.equals(serviceName)) {
                isWork = true;
                break;
            }
        }
        return isWork;
    }

    private void downloadApkWithService() {
        Utils.getMainActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Utils.getMainActivity(), DownloadService.class);
                Utils.getMainActivity().startService(intent);
            }
        });
    }
}






