package com.example.amap3d.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;

import com.example.amap3d.MainActivity;
import com.example.amap3d.Managers.UpdateManager;
import com.example.amap3d.R;
import com.example.amap3d.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

//TODO: 8.0Notification适配，Toast去除，下载异常通知
public class DownloadService extends Service {
    private static NotificationManager notificationManager;
    private boolean isFirstCommand = true;
    private PowerManager.WakeLock wakeLock = null;
    private static Thread thread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isFirstCommand) {
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            requireWakeLock();
        }
        if (intent == null) {
            notificationManager.cancelAll();
        }
        if (!isApkExist() || thread == null) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    downloadApk();
                }
            });
            thread.start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void requireWakeLock() {
        if (null == wakeLock) {
            PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "PostLocationService");
            if (null != wakeLock) {
                wakeLock.acquire();
            }
        }
    }

    private void cancleWakeLock() {
        if (null != wakeLock) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    private Notification setDownloadNotification(int progress) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);//避免重复打开activity
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.sign));
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle("校车查询（更新下载中）");
        builder.setOngoing(true);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setContentText((progress > 0 ? progress : 0) + "%");
        builder.setProgress(100, progress, false);
        return builder.build();
    }

    private boolean isApkExist() {
        File file = new File(Environment.getExternalStorageDirectory(), UpdateManager.downloadPathName + ".apk");
        if (file.exists()) {
            installApk(file);
            return true;
        }
        return false;
    }

    public void downloadApk() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            Utils.uiToast("SD卡不可用，请检查权限设置");
            return;
        }
        Request request = new Request.Builder()
                .url(UpdateManager.downloadApkURL)
                .build();
        Utils.client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
//                Utils.uiToast("安装包下载失败");
                call.cancel();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream inputStream = null;
                FileOutputStream fileOutputStream = null;
                try {
                    inputStream = response.body().byteStream();//获取输入流
                    long fileSize = response.body().contentLength();//获取文件大小
                    if (inputStream != null) {
                        File file = new File(Environment.getExternalStorageDirectory(), UpdateManager.downloadPathName);
                        fileOutputStream = new FileOutputStream(file);
                        byte[] bytes = new byte[1024];
                        int bytesNum = -1;
                        int progress = 0;
                        while ((bytesNum = inputStream.read(bytes)) != -1) {
                            fileOutputStream.write(bytes, 0, bytesNum);
                            progress += bytesNum;
                            notificationManager.notify(10, setDownloadNotification((int) (progress * 100 / fileSize)));
                        }
                        if (file.exists()) {
                            File fullFile = new File(UpdateManager.downloadPathName + ".apk");
                            file.renameTo(fullFile);
                            installApk(file);
                        } else {
//                            Utils.uiToast("升级包获取失败");
                        }
                    }
                    fileOutputStream.flush();
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
//                    Utils.uiToast("文件写入失败");
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
                    destroyService();
                }
            }
        });
    }

    private void destroyService() {
        notificationManager.cancelAll();
        cancleWakeLock();
        this.onDestroy();
    }

    private void installApk(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //android N的权限问题
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(this, "com.example.amap3d.fileprovider", file);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        destroyService();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
