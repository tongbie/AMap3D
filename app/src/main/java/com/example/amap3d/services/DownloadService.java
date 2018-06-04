package com.example.amap3d.services;

import android.app.Notification;
import android.app.NotificationChannel;
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

import com.example.amap3d.BuildConfig;
import com.example.amap3d.MainActivity;
import com.example.amap3d.managers.UpdateManager;
import com.example.amap3d.R;
import com.example.amap3d.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadService extends Service {
    private static NotificationManager notificationManager;
    private static boolean isFirstCommand = true;
    private PowerManager.WakeLock wakeLock = null;
    private String channelId = "1";
    private int downloadNotificationId = 10;
    private String downloadNotificationTag = "tag";
    private NotificationCompat.Builder builder = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isFirstCommand) {
            initNotificationManager();
            requireWakeLock();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    downloadApk();
                }
            }).start();
        }
        if (intent == null) {
            notificationManager.cancelAll();
        }
        return START_NOT_STICKY;
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
        if (builder == null) {
            builder = new NotificationCompat.Builder(this, channelId);
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);//避免重复打开activity
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.sign));
            builder.setContentIntent(pendingIntent);
            builder.setContentTitle("校车查询（更新下载中）");
            builder.setOngoing(true);
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }
        builder.setContentText((progress > 0 ? progress : 0) + "%");
        builder.setProgress(100, progress, false);
        return builder.build();
    }

    private boolean isApkExist(long fileSize) {
        boolean isExist = false;
        File file = new File(Environment.getExternalStorageDirectory(), UpdateManager.downloadFileName);
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            long localFileSize = fileInputStream.available();
            if (localFileSize == fileSize) {
                installApk(file);
                isExist = true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return isExist;
    }

    private void initNotificationManager() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = "channelName";
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showFailNotifacation(String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.sign));
        builder.setContentTitle("下载失败");
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setContentText(text);
        notificationManager.notify(20, builder.build());
    }

    public void downloadApk() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            showFailNotifacation("SD卡不可用，请检查权限设置");
            destroyService();
            return;
        }
        Request request = new Request.Builder()
                .url(UpdateManager.downloadApkURL)
                .build();
        Utils.client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
                showFailNotifacation("网络异常，请检查网络设置");
                destroyService();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream inputStream = null;
                FileOutputStream fileOutputStream = null;
                try {
                    inputStream = response.body().byteStream();
                    long fileSize = response.body().contentLength();
                    if (!isApkExist(fileSize) && inputStream != null) {
                        File file = new File(Environment.getExternalStorageDirectory(), UpdateManager.downloadFileName);
                        fileOutputStream = new FileOutputStream(file);
                        byte[] bytes = new byte[1024];
                        int bytesNum;
                        int allBytes = 0;
                        int progress ;
                        int progressListener = 0;
                        while ((bytesNum = inputStream.read(bytes)) != -1) {
                            fileOutputStream.write(bytes, 0, bytesNum);
                            allBytes += bytesNum;
                            progress = (int) (allBytes * 100 / fileSize);
                            if (progress > progressListener) {
                                notificationManager.notify(downloadNotificationTag, downloadNotificationId, setDownloadNotification(progress));
                                progressListener = progress;
                            }
                        }
                        notificationManager.notify(downloadNotificationTag, downloadNotificationId, setDownloadNotification(100));
                        if (file.exists()) {
                            installApk(file);
                        } else {
                            showFailNotifacation("升级包获取失败");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showFailNotifacation("连接超时，请检查网络设置");
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (fileOutputStream != null) {
                            fileOutputStream.flush();
                            fileOutputStream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    destroyService();
                }
            }
        });
    }

    private void destroyService() {
        notificationManager.cancel(downloadNotificationTag, downloadNotificationId);
        cancleWakeLock();
        this.stopSelf();
    }

    private void installApk(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //android N的权限问题
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileProvider", file);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
