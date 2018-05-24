package com.example.amap3d.Managers;

import android.app.Activity;
import android.app.ActivityManager;
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
import android.util.Log;
import android.widget.Toast;

import com.example.amap3d.Datas;
import com.example.amap3d.Gsons.ApkVersionGson;
import com.example.amap3d.MainActivity;
import com.example.amap3d.R;
import com.example.amap3d.Services.DownloadService;
import com.example.amap3d.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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

    public static String downloadApkURL = /*"http://bus.mysdnu.cn/android/latest/:type"*/
//            "http://p.gdown.baidu.com/62e0fca9e294a883e407f5483c61ca13732e8699c5dea2c4e2d9e8497de01da00ca0c898c5d67e27bacc59ad5fb28ea0c3e065f4be8f6d6d2fb710c111adc8eee16bee2b2d73abc4fde9a2fea3e6231f370c30354e3a6920c5d22207ac464e467b3797ce9e675dd05ec8662f2a267c9883082f66d29b393dce7dbd91232de5019be714d931998d7a46d67da9039bc96544e5e4848967f44841fc8fcf999236e3aa77d9b6f6a2a7fdd4662739dc90fa8b4ed99482a115fa494ce03ebf9de70d81db9b394df69e73e830f33b62b98548f4d67001496904b5f0217146fbed429cb2a52a3cb7fb3d36a3561e04afd206e36d07045414a6b886368bfff40f271403ee4dd4e14dc8d7931044f3395cc765fadea08994ecbaa2b0b6008c955b632dda84307732ca25a576d7c10efec0a1b549b9bf5c43d2dba8f59d59f989eda0e7966727e60d21204626dde77255c83bbc41b378e092d76b554d1b";
            "http://gdown.baidu.com/data/wisegame/06ed75c9055ec156/baidushoujizhushou_16795029.apk";
    public static String downloadPathName = "SchoolBusQuery";

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
        if(isServiceWorking(Utils.getApplicationContext(),"com.example.amap3d.Services.DownloadService")){
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
            Log.e("Service",mName);
            if (mName.equals(serviceName)) {
                isWork = true;
                break;
            }
        }
        return isWork;
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
//                intent.setAction(ACTION_START);
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Utils.getMainActivity().startService(intent);
            }
        });
    }
}






