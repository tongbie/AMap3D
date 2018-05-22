package com.example.amap3d.Services;

/**
 * Created by BieTong on 2018/5/19.
 */

public interface DownloadListener {

    void onProgress(int progress);

    void onSuccess();

    void onFailed();

    void onPaused();

    void onCanceled();
}
