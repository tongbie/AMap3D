package com.example.amap3d.Services;

import android.os.AsyncTask;
import android.os.Environment;

import com.example.amap3d.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by BieTong on 2018/5/19.
 */

public class DownloadTask extends AsyncTask<String, Integer, Integer> {
    public String pathName="SchoolBusQuery";

    public static final int SUCCESS = 0;
    public static final int FAILED = 1;
    public static final int PAUSED = 2;
    public static final int CANCELED = 3;

    private DownloadListener downloadListener;

    private boolean isCanceled = false;
    private boolean isPaused = false;

    private int lastProgress;

    public DownloadTask(DownloadListener Listener) {
        this.downloadListener = Listener;
    }

    //后台执行具体的下载逻辑
    @Override
    protected Integer doInBackground(String... strings) {
        InputStream inputStream = null;
        RandomAccessFile randomAccessFile = null;
        File file = null;
        try {
            long downloadLength = 0;//记录下载文件的长度
            String downloadUrl = strings[0];
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory + pathName);
            if (file.exists()) {
                downloadLength = file.length();
            }
            long contentLength = getContentLength(downloadUrl);
            if (contentLength == 0) {
                return FAILED;
            } else if (contentLength == downloadLength) {              //已下载字节和总文件字节长度相等，则下载成功
                return SUCCESS;
            }
            //断点下载，指定从哪个字节开始上一次的下载
//            Request request = new Request.Builder().addHeader("RANGE", "bytes = " + downloadLength + "-").url(downloadUrl).build();
//            Response response = Utils.client.newCall(request).execute();
//            if (response != null) {
//                inputStream = response.body().byteStream();
//                randomAccessFile = new RandomAccessFile(file, "rw");
//                randomAccessFile.seek(downloadLength);//跳过已下载字节
//                byte[] b = new byte[1024];
//                int total = 0;
//                int len;
//                while ((len = inputStream.read(b)) != -1) {
//                    if (isCanceled) {
//                        return CANCELED;
//                    } else if (isPaused) {
//                        return PAUSED;
//                    } else {
//                        total += len;
//                        randomAccessFile.write(b, 0, len);
//                        //计算已下载的百分比
//                        int progress = (int) ((total + downloadLength) * 100 / contentLength);
//                        publishProgress(progress);
//                    }
//                }
//                response.body().close();
//                return SUCCESS;
//            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
                if (isCanceled && file != null) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return FAILED;
    }

    //在界面上更新当前的下载进度
    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if (progress > lastProgress) {
            //回调方法中的onProgress
            downloadListener.onProgress(progress);
            lastProgress = progress;
        }
    }

    //通知最终的下载结果
    //用的listener来回调方法。
    @Override
    protected void onPostExecute(Integer status) {
        switch (status) {
            case SUCCESS:
                downloadListener.onSuccess();
                break;
            case FAILED:
                downloadListener.onFailed();
                break;
            case PAUSED:
                downloadListener.onPaused();
                break;
            case CANCELED:
                downloadListener.onCanceled();
                break;
            default:
                break;
        }
    }

    public void pauseDownload(){
        isPaused = true;
    }

    public void cancelDownload(){
        isCanceled = true;
    }

    private long getContentLength(String downloadUrl) throws IOException {
        Request request = new Request.Builder().url(downloadUrl).build();
        Response response = Utils.client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }
        return 0;
    }
}
