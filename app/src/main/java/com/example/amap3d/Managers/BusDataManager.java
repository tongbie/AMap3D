package com.example.amap3d.Managers;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.amap3d.Datas;
import com.example.amap3d.Gsons.BusDataGson;
import com.example.amap3d.Gsons.BusPositionGson;
import com.example.amap3d.Utils;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by BieTong on 2018/5/10.
 */

public class BusDataManager {
    private Context context;
    private Activity activity;

    public BusDataManager(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
    }

    /* 获取校车信息 */
    public synchronized void setBusInformationToMap() {
        try {
            Request request = new Request.Builder()
                    .url(Utils.busDataURL)
                    .build();
            Response response = Utils.client.newCall(request).execute();
            String responseData = response.body().string();
            String responseCode = String.valueOf(response.code());
            if (responseData != null && responseCode.charAt(0) == '2') {
                try {
                    List<BusDataGson> busDatas = Utils.gson.fromJson(responseData, new TypeToken<List<BusDataGson>>() {
                    }.getType());
                    for (BusDataGson busData : busDatas) {
                        String key = busData.getGPSDeviceIMEI();
                        String title = busData.getBus_lineName() + "\n" + busData.getBus_departureSite();
                        String snippet = Pattern.compile("[^0-9]").matcher(busData.getBus_arriveSite()).replaceAll("");
                        Datas.busInformationMap.put(key, new String[]{title, snippet});
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    uiToast("校车信息异常");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            uiToast("数据获取失败，请检查网络设置");
        }
    }

    /* 获取校车位置 */
    public synchronized List<BusPositionGson> getBusPosition() {
        List<BusPositionGson> busPositionList = null;
        try {
            Request request = new Request.Builder()
                    .url(Utils.busPositionURL)
                    .build();
            Response response = Utils.client.newCall(request).execute();
            String responseData = response.body().string();
            String responseCode = String.valueOf(response.code());
            if (responseData != null && responseCode.charAt(0) == '2') {
                try {
                    busPositionList = Utils.gson.fromJson(responseData, new TypeToken<List<BusPositionGson>>() {
                    }.getType());
                } catch (Exception e) {
                    e.printStackTrace();
                    uiToast("位置数据异常");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            uiToast("数据获取失败");
        }
        return busPositionList == null ? new ArrayList<BusPositionGson>() : busPositionList;
    }


    private void uiToast(final String text) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
