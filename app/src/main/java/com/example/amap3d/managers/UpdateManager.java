package com.example.amap3d.managers;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;

import com.example.amap3d.MainActivity;
import com.example.amap3d.gsons.ApkVersionGson;
import com.example.amap3d.services.DownloadService;
import com.example.amap3d.utils.Utils;

import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by BieTong on 2018/5/7.
 */

public class UpdateManager {
    private static UpdateManager updateManager;
    private static String updateDescription;
    private static final String versionCodeURL = "http://bus.mysdnu.cn/android/update/alpha";

    public static String downloadApkURL = "http://bus.mysdnu.cn/android/latest/alpha";
    public static final String downloadFileName = "SchoolBusQuery.apk";

    public static final int UPDATE_NOT_NEED = 0;
    private static final int UPDATA_CLIENT = 1;
    private static final int UPDATE_FORCE = 2;
    private static final int UPDATE_LOCAL_VERSION_ERROR = 3;
    private static final int UPDATE_SERVICE_VERSION_ERROR = 4;

    private UpdateManager() {

    }

    public static UpdateManager getInstance() {
        if (updateManager == null) {
            updateManager = new UpdateManager();
        }
        return updateManager;
    }

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
            int versionCode;
            if (responseCode.charAt(0) == '2') {
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
//                String updateType = apkVersionGson.getType();
//                updateState = updateType.equals("alpha") ? "alpha" : "stable";
                //TODO:updateType
                if (versionCode < minVersionCode /*|| updateType.equals("updateType")*/) {
                    versionState[0] = UPDATE_FORCE;
                    updateDescription = ("更新时间:" + apkVersionGson.getUpdateTime()).equals("") ? "暂无" : (apkVersionGson.getUpdateTime()
                            + "\n更新日志：\n" + apkVersionGson.getDescription()).equals("") ? "暂无" : apkVersionGson.getDescription();
                } else if (versionCode < packageVersionCode) {
                    versionState[0] = UPDATA_CLIENT;
                    updateDescription = ("更新时间:" + apkVersionGson.getUpdateTime()).equals("") ? "暂无" : (apkVersionGson.getUpdateTime()
                            + "\n更新日志：\n" + apkVersionGson.getDescription()).equals("") ? "暂无" : apkVersionGson.getDescription();
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
        if (isServiceWorking(MainActivity.getInstance().getApplicationContext())) {
            Utils.uiToast("更新下载中...");
            return;
        }
        final AlertDialog.Builder builer = new AlertDialog.Builder(MainActivity.getInstance());
        builer.setTitle(isForceUpdate ? "有必须的更新" : "有可用的更新");
        builer.setCancelable(!isForceUpdate);
        builer.setMessage(updateDescription);
        builer.setPositiveButton(isForceUpdate ? "退出" : "忽略", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (isForceUpdate) {
                    MainActivity.getInstance().finish();
                }
            }
        });
        builer.setNegativeButton("升级", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                downloadApkWithService();
            }
        });
        MainActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = builer.create();
                dialog.show();
            }
        });
    }

    private boolean isServiceWorking(Context context) {
        boolean isWork = false;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        assert activityManager != null;
        List<ActivityManager.RunningServiceInfo> runningServiceInfoList = activityManager.getRunningServices(Integer.MAX_VALUE);
        if (runningServiceInfoList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < runningServiceInfoList.size(); i++) {
            String mName = runningServiceInfoList.get(i).service.getClassName();
            if (mName.equals("com.example.amap3d.Services.DownloadService")) {
                isWork = true;
                break;
            }
        }
        return isWork;
    }

    private void downloadApkWithService() {
        Utils.uiToast("开始下载");
        MainActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.getInstance(), DownloadService.class);
                MainActivity.getInstance().startService(intent);
            }
        });
    }
}






