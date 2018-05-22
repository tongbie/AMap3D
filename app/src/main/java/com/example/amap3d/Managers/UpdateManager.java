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
    private Activity activity;
    private ProgressDialog dialog;
    private ServiceConnection conn;
    private DownloadService.DownloadBinder iBinder;

    private String externalStorageDirectoryChild = "SchoolBusQuery";
    private static String updateDescription;
    private static String downloadApkURL = "http://bus.mysdnu.cn/android/latest/:type";
    private static String versionCodeURL = "http://bus.mysdnu.cn/android/update/:type";

    public static final int UPDATE_NOT_NEED = 0;
    public static final int UPDATA_CLIENT = 1;
    public static final int UPDATE_FORCE = 2;
    public static final int UPDATE_LOCAL_VERSION_ERROR = 3;
    public static final int UPDATE_SERVICE_VERSION_ERROR = 4;


    public UpdateManager(final Activity activity) {
        this.activity = activity;
    }

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

    private Notification getNotification(String title, int progress) {
        Intent intent = new Intent(activity, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(activity);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(activity.getResources(), R.drawable.sign));
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(title);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);//设置通知重要程度，min,low,默认,high，max
        if (progress > 0) {
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false);//三个参数，最大进度，当前进度，是否使用模糊进度条
        }
        return builder.build();
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
                uiToast("应用版本信息获取失败");
                break;
            case UPDATE_SERVICE_VERSION_ERROR:
                uiToast("服务器版本信息获取失败");
                break;
            default:
        }
    }

    public void downloadApk() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(activity, "SD卡不可用，请检查权限设置", Toast.LENGTH_SHORT).show();
            return;
        }
        initDialog();
        Request request = new Request.Builder()
                .url(downloadApkURL)
                .build();
        Utils.client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                uiToast("安装包下载失败");
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream inputStream = null;
                FileOutputStream fileOutputStream = null;
                try {
                    inputStream = response.body().byteStream();//获取输入流
                    long fileSize = response.body().contentLength() / 1024;//获取文件大小
                    dialog.setMax((int) fileSize);
                    if (inputStream != null) {
                        File file = new File(Environment.getExternalStorageDirectory(), externalStorageDirectoryChild);
                        fileOutputStream = new FileOutputStream(file);
                        byte[] bytes = new byte[1024];
                        int bytesNum = -1;
                        int process = 0;
                        while ((bytesNum = inputStream.read(bytes)) != -1) {
                            fileOutputStream.write(bytes, 0, bytesNum);
                            process += bytesNum / 1024;
                            setDialogProgress(process);
                            //TODO:
                            NotificationManager notificationManager = (NotificationManager) activity.getSystemService(NOTIFICATION_SERVICE);
                            notificationManager.notify(1, getNotification("下载中：", (int) (process/fileSize)));

                        }
                        if (file.exists()) {
                            installApk();
                        } else {
                            uiToast("升级包获取失败");
                            dialog.cancel();
                        }
                    }
                    fileOutputStream.flush();
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    uiToast("文件写入失败");
                    return;
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (fileOutputStream != null) {
                            fileOutputStream.close();
                        }
                    } catch (IOException e) {

                    }
                }
            }
        });
    }

    private void installApk() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //android N的权限问题
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(activity, "com.example.amap3d.fileprovider", new File(Environment.getExternalStorageDirectory(), externalStorageDirectoryChild));
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory(), externalStorageDirectoryChild)), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        activity.startActivity(intent);
        dialog.cancel();
    }

    private void initDialog() {
        dialog = new ProgressDialog(activity);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.setTitle("正在下载...");
        dialog.setMessage("请稍候...");
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.show();
                dialog.setProgress(0);
            }
        });
    }

    private void uiToast(final String text) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUpdataDialog(final boolean isForceUpdate) {
        final AlertDialog.Builder builer = new AlertDialog.Builder(activity);
        builer.setTitle(isForceUpdate ? "有必须的更新" : "有可用的更新");
        builer.setCancelable(!isForceUpdate);
        builer.setMessage(updateDescription);
        builer.setPositiveButton(isForceUpdate ? "退出" : "忽略", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (isForceUpdate) {
                    activity.finish();
                    return;
                }
            }
        });
        builer.setNegativeButton("升级", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(isForceUpdate) {
                    downloadApk();
                }else {
                    downloadApkWithService();
                }
            }
        });
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog dialog = builer.create();
                dialog.show();
            }
        });
    }

    private void downloadApkWithService(){
        conn = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                iBinder = (DownloadService.DownloadBinder) service;
                iBinder.startDownload(downloadApkURL);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        Intent intent = new Intent(activity, DownloadService.class);
        activity.startService(intent);
        activity.bindService(intent, conn, BIND_AUTO_CREATE);
    }

    private void setDialogProgress(final int progress) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.setProgress(progress);
            }
        });
    }
}






