package com.example.amap3d.managers;

import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.example.amap3d.R;
import com.example.amap3d.datas.Datas;
import com.example.amap3d.gsons.BusDataGson;
import com.example.amap3d.gsons.BusMoveGson;
import com.example.amap3d.gsons.BusPositionGson;
import com.example.amap3d.utils.Utils;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by BieTong on 2018/5/10.
 */

public class BusManager {
    private static BusManager busManager;
    public static final String busDataURL = "http://bus.mysdnu.cn/android/bus";
    public static final String busPositionURL = "http://bus.mysdnu.cn/android/bus/location";

    private BusManager() {
    }

    public static BusManager getInstance() {
        if (busManager == null) {
            busManager = new BusManager();
        }
        return busManager;
    }

    /* 获取校车信息 */
    public synchronized void setBusInformationToMap() {
        try {
            Request request = new Request.Builder()
                    .url(busDataURL)
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
//                    Log.e("busInformationMap", Datas.busInformationMap.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    Utils.uiToast("校车信息异常");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.uiToast("数据获取失败，请检查网络设置");
        }
    }

    public void moveBus(MqttMessage message) {
        try {
            List<BusMoveGson> busMoveGsons = Utils.gson.fromJson(message.toString(), new TypeToken<List<BusMoveGson>>() {
            }.getType());
            for (BusMoveGson busMoveGson : busMoveGsons) {
                String key = busMoveGson.getGPSDeviceIMEI();
                LatLng latLng = Datas.busMarkerMap.get(key).getPosition();
                double lat = latLng.latitude;
                double lng = latLng.longitude;
                Datas.busMarkerMap.get(key).setPosition(new LatLng(busMoveGson.getLat(), busMoveGson.getLng()));
                Datas.busMarkerMap.get(key).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bus2));
                AMapManager.getInstance().moveMarker(new LatLng[]{new LatLng(lat, lng), Datas.busMarkerMap.get(key).getPosition()}, key);
            }
//                    Log.e("messageArrived", Datas.busMarkerMap.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* 获取校车位置 */
    public synchronized List<BusPositionGson> requireBusPosition() {
        List<BusPositionGson> busPositionList = null;
        try {
            Request request = new Request.Builder()
                    .url(busPositionURL)
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
                    Utils.uiToast("位置数据异常");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.uiToast("数据获取失败");
        }
        return busPositionList == null ? new ArrayList<BusPositionGson>() : busPositionList;
    }
}
