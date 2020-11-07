package com.example.amap3d.managers;

import android.util.Log;

import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.example.amap3d.R;
import com.example.amap3d.datas.Datas;
import com.example.amap3d.datas.Fields;
import com.example.amap3d.gsons.AdressGson;
import com.example.amap3d.gsons.BusDataGson;
import com.example.amap3d.gsons.BusMoveGson;
import com.example.amap3d.gsons.BusPositionGson;
import com.example.amap3d.gsons.BusTimetableGson;
import com.example.amap3d.gsons.TodayTimetableGson;
import com.example.amap3d.utils.Utils;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by BieTong on 2018/5/10.
 */

public class BusManager {

    private BusManager() {
    }

    private static class BusManagerFactory {
        public static BusManager instance = new BusManager();
    }

    public static BusManager getInstance() {
        return BusManagerFactory.instance;
    }

    /* 获取校车信息 */
    public void requireBusInformation() throws Exception {
        Request request = new Request.Builder()
                .url(Fields.URL_BUS_INFORMATION)
                .build();
        Response response = Utils.client.newCall(request).execute();
        String body = response.body().string();
        String code = String.valueOf(response.code());
        if (code.charAt(0) == '2') {
            List<BusDataGson> busDatas = Utils.gson.fromJson(body, new TypeToken<List<BusDataGson>>() {
            }.getType());
            for (BusDataGson busData : busDatas) {
                String key = busData.getGPSDeviceIMEI();
                String title = busData.getBus_lineName() + "\n" + busData.getBus_departureSite();
                String snippet = Pattern.compile("[^0-9]").matcher(busData.getBus_arriveSite()).replaceAll("");
                Datas.getBusInformationMap().put(key, new String[]{title, snippet});
            }
        }
    }

    public void requireAddressAndTodayTimetable() {
        try {
            Request request = new Request.Builder()
                    .url(Fields.URL_ADRESS)
                    .build();
            Response response = Utils.client.newCall(request).execute();
            String body = response.body().string();
            String code = String.valueOf(response.code());
            if (code.charAt(0) == '2') {
                List<AdressGson> adressGsonList = Utils.gson.fromJson(body, new TypeToken<List<AdressGson>>() {
                }.getType());
                Datas.setAdressList(adressGsonList);
                requireTodayBusTimetable();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ERROR: ", "BusManager.requireAddressAndTodayTimetable: " + e.getMessage());
        }
    }

    public void requireAllBusTimetable() {
        try {
            Request request = new Request.Builder()
                    .url(Fields.URL_ALL_BUS_TIMETABLE)
                    .build();
            Response response = Utils.client.newCall(request).execute();
            String body = response.body().string();
            String code = String.valueOf(response.code());
            List<Map<String, String>> timetableList = new ArrayList<>();
            String[] campusName = new String[]{"", "千佛山校区", "长清湖校区"};
            if (code.charAt(0) == '2') {
                List<BusTimetableGson> busTimetableList = Utils.gson.fromJson(body, new TypeToken<List<BusTimetableGson>>() {
                }.getType());
                for (BusTimetableGson data : busTimetableList) {
                    Map<String, String> timetable = new HashMap<>();
                    timetable.put("routeTitle", campusName[data.getFrom()] + " → " + campusName[data.getTo()]);
                    timetable.put("timeTitle", data.getStart_date() + "~" + data.getEnd_date());
                    timetable.put("timeList", data.getDeparture_time());
                    timetableList.add(timetable);
                }
            } else {
                Map<String, String> timetable = new HashMap<>();
                timetable.put("routeTitle", "数据获取失败");
                timetableList.add(timetable);
            }
            ViewManager.getInstance().setBusTimetableInUiThread(timetableList);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ERROR: ", "BusManager.requireAllBusTimetable: " + e.getMessage());
        }
    }

    public void requireTodayBusTimetable() {
        try {
            Request request = new Request.Builder()
                    .url(Fields.URL_TODAY_BUS_TIMETABLE)
                    .build();
            Response response = Utils.client.newCall(request).execute();
            String body = response.body().string();
            String code = String.valueOf(response.code());
            if (code.charAt(0) == '2') {
                List<TodayTimetableGson> todayTimetableGsonList = Utils.gson.fromJson(/*hardCodeData*/body, new TypeToken<List<TodayTimetableGson>>() {
                }.getType());
                ViewManager.getInstance().nextBusView.setData(todayTimetableGsonList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ERROR: ", "BusManager.requireTodayBusTimetable: " + e.getMessage());
        }
    }

    public void moveBus(MqttMessage message) {
        try {
            List<BusMoveGson> busMoveGsons =
                    Utils.gson.fromJson(message.toString(),
                            new TypeToken<List<BusMoveGson>>() {
            }.getType());
            for (BusMoveGson busMoveGson : busMoveGsons) {
                final Marker movingMarker =
                        Datas.getBusMarkerMap().get(busMoveGson.getGPSDeviceIMEI());
                final LatLng newLatLng =
                        new LatLng(busMoveGson.getLat(), busMoveGson.getLng());

                final SmoothMoveMarker smoothMarker =
                        new SmoothMoveMarker(AMapManager.aMap);
                smoothMarker.setDescriptor(
                        BitmapDescriptorFactory.fromResource(R.drawable.ic_bus_moving));
                smoothMarker.setPoints(
                        Arrays.asList(movingMarker.getPosition(), newLatLng));
                smoothMarker.setTotalDuration(3);
                movingMarker.setVisible(false);
                smoothMarker.startSmoothMove();
                smoothMarker.setMoveListener(new SmoothMoveMarker.MoveListener() {
                    @Override
                    public void move(double distance) {
                        if (distance == 0) {
                            smoothMarker.stopMove();
                            smoothMarker.removeMarker();
                            movingMarker.setPosition(newLatLng);
                            movingMarker.setIcon(
                                    BitmapDescriptorFactory.fromResource(R.drawable.ic_bus_red));
                            movingMarker.setVisible(true);
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ERROR: ", "BusManager.moveBus: " + e.getMessage());
        }
    }

    void moveTestBus(MqttMessage message) {
        try {
            List<BusMoveGson> busMoveGsons = Utils.gson.fromJson(message.toString(), new TypeToken<List<BusMoveGson>>() {
            }.getType());
            for (BusMoveGson busMoveGson : busMoveGsons) {
                final String key = busMoveGson.getGPSDeviceIMEI();
                final Marker movingMarker = Datas.getBusMarkerMap().get(key);
                LatLng oldLatLng = movingMarker.getPosition();
                final LatLng newLatLng = new LatLng(busMoveGson.getLat(), busMoveGson.getLng());
                LatLng[] movePath = new LatLng[]{oldLatLng, newLatLng};

                final SmoothMoveMarker smoothMarker = new SmoothMoveMarker(AMapManager.aMap);
                smoothMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.drawable.ic_bus_moving));
                smoothMarker.setPoints(Arrays.asList(movePath));
                smoothMarker.setTotalDuration(1);
                movingMarker.setVisible(false);
                smoothMarker.startSmoothMove();

                smoothMarker.setMoveListener(new SmoothMoveMarker.MoveListener() {
                    @Override
                    public void move(double distance) {
                        if (distance == 0) {
                            smoothMarker.stopMove();
                            smoothMarker.removeMarker();
                            movingMarker.setPosition(newLatLng);
                            movingMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bus_red));
                            movingMarker.setVisible(true);
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ERROR: ", "BusManager.moveTestBus: " + e.getMessage());
        }
    }

    /* 获取校车位置 */
    public List<BusPositionGson> requireBusPosition() throws Exception {
        List<BusPositionGson> busPositionList = null;
        Request request = new Request.Builder()
                .url(Fields.URL_BUS_POSITION)
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
