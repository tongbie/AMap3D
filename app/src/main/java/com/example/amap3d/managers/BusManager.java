package com.example.amap3d.managers;

import android.util.Log;

import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.example.amap3d.R;
import com.example.amap3d.datas.Datas;
import com.example.amap3d.gsons.BusDataGson;
import com.example.amap3d.gsons.BusMoveGson;
import com.example.amap3d.gsons.BusPositionGson;
import com.example.amap3d.utils.Utils;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by BieTong on 2018/5/10.
 */

public class BusManager {
    private static BusManager busManager;
    private static final String busDataURL = "http://bus.mysdnu.cn/android/bus";
    private static final String busPositionURL = "http://bus.mysdnu.cn/android/bus/location";

    private BusManager() {
    }

    public static BusManager getInstance() {
        if (busManager == null) {
            busManager = new BusManager();
        }
        return busManager;
    }

    /* 获取校车信息 */
    public synchronized void requireBusInformation() throws Exception {
        Request request = new Request.Builder()
                .url(busDataURL)
                .build();
        Response response = Utils.client.newCall(request).execute();
        String responseData = response.body().string();
        String responseCode = String.valueOf(response.code());
        if (responseCode.charAt(0) == '2') {
            List<BusDataGson> busDatas = Utils.gson.fromJson(responseData, new TypeToken<List<BusDataGson>>() {
            }.getType());
            for (BusDataGson busData : busDatas) {
                String key = busData.getGPSDeviceIMEI();
                String title = busData.getBus_lineName() + "\n" + busData.getBus_departureSite();
                String snippet = Pattern.compile("[^0-9]").matcher(busData.getBus_arriveSite()).replaceAll("");
                Datas.busInformationMap.put(key, new String[]{title, snippet});
            }
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean isNeedSetPoints = true;

    public void moveTestBus(MqttMessage message) {
        try {
            List<BusMoveGson> busMoveGsons = Utils.gson.fromJson(message.toString(), new TypeToken<List<BusMoveGson>>() {
            }.getType());
            for (BusMoveGson busMoveGson : busMoveGsons) {
                final String key = busMoveGson.getGPSDeviceIMEI();
                LatLng latLng = Datas.busMarkerMap.get(key).getPosition();
                double lat = latLng.latitude;
                double lng = latLng.longitude;
                Datas.busMarkerMap.get(key).setPosition(new LatLng(busMoveGson.getLat(), busMoveGson.getLng()));
                Datas.busMarkerMap.get(key).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bus2));

                Datas.latLngList.add(new LatLng(lat, lng));
                if(Datas.latLngList.size()>1) {
                    Datas.latLngList.remove(0);
                }

                if (Datas.smoothMarker == null) {
                    Datas.smoothMarker = new SmoothMoveMarker(AMapManager.aMap);
                    Datas.smoothMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.drawable.bus_move));
                    Datas.smoothMarker.setTotalDuration(1);
                    Datas.smoothMarker.setMoveListener(new SmoothMoveMarker.MoveListener() {
                        @Override
                        public void move(double distance) {
                            Log.e("INDEX", Datas.smoothMarker.getIndex() + "");
                            if (Datas.smoothMarker.getIndex() > 0) {
//                                Datas.latLngList.remove(0);
//                                Datas.smoothMarker.stopMove();
                            }
                            if (distance == 0) {
//                                Datas.smoothMarker.setVisible(false);
//                                Datas.busMarkerMap.get(key).setVisible(true);
//                                isNeedSetPoints = true;
                            }
                        }
                    });
                }
                Datas.smoothMarker.setPoints(Datas.latLngList);
                Datas.smoothMarker.startSmoothMove();
                Datas.busMarkerMap.get(key).setVisible(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* 获取校车位置 */
    public synchronized List<BusPositionGson> requireBusPosition() throws Exception {
        List<BusPositionGson> busPositionList = null;
        Request request = new Request.Builder()
                .url(busPositionURL)
                .build();
        Response response = Utils.client.newCall(request).execute();
        String responseData = response.body().string();
        String responseCode = String.valueOf(response.code());
        if (responseCode.charAt(0) == '2') {
            busPositionList = Utils.gson.fromJson(responseData, new TypeToken<List<BusPositionGson>>() {
            }.getType());
        }
        return busPositionList == null ? new ArrayList<BusPositionGson>() : busPositionList;
    }
}
