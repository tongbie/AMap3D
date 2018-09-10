package com.example.amap3d.managers;

import android.content.Intent;
import android.util.Log;

import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.example.amap3d.LoginActivity;
import com.example.amap3d.MainActivity;
import com.example.amap3d.R;
import com.example.amap3d.Utils;
import com.example.amap3d.datas.Datas;
import com.example.amap3d.gsons.UploadPositionGson;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PeopleManager {
    private static PeopleManager peopleManager;

    public static PeopleManager getInstance(){
        if(peopleManager==null){
            peopleManager= new PeopleManager();
        }
        return peopleManager;
    }

    public void getPeoplePosition() {
        String url = "http://bus.mysdnu.cn/client";
        Request request = new Request.Builder()
                .url(url)
                .build();
        try {
            Response response = Utils.client.newCall(request).execute();
            String data = response.body().string();
            int code = response.code();
            if (code == 200 && data != null) {
                List<UploadPositionGson> uploadPositionGsonList = Utils.gson.fromJson(data, new TypeToken<List<UploadPositionGson>>() {
                }.getType());
                Datas.peopleMap.clear();
                for (UploadPositionGson peopleGson : uploadPositionGsonList) {
                    LatLng latLng = new LatLng(Double.parseDouble(peopleGson.getLat()), Double.parseDouble(peopleGson.getLng()));
                    Marker marker=AMapManager.aMap.addMarker(new MarkerOptions().position(latLng));
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.people));
                    Datas.peopleMap.put(peopleGson.getDeviceId(), marker);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.uiToast("位置列表获取失败");
        }
    }

    public UploadPositionGson uploadPositionGson;

    public static final String uploadPositionTitle = "clientLocation";

    /*上传位置*/
    public void uploadPosition() {
        if (uploadPositionGson == null) {
            uploadPositionGson = new UploadPositionGson();
            uploadPositionGson.setDeviceId(MQTTManager.clientId);
            AMapManager.getInstance().setOnPositionChangedListener(new AMapManager.OnPositionChangedListener() {
                @Override
                public void onPositionChanged(double longitude, double latitude) {
                    uploadPositionGson.setLat(latitude + "");
                    uploadPositionGson.setLng(longitude + "");
                    String uploadPositionJson = Utils.gson.toJson(uploadPositionGson);
                    MQTTManager.getInstance().publish(uploadPositionTitle, uploadPositionJson, true);
                }
            });
            Utils.uiToast("位置上传中...");
        }
    }

    /*destroy*/
    public void destroy() {
        uploadPositionGson = null;
        AMapManager.getInstance().setOnPositionChangedListener(null);
        try {
            if(MQTTManager.getInstance().mqttClient!=null) {
                MQTTManager.getInstance().mqttClient.unsubscribe(uploadPositionTitle);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ExecutorService uploadPostionService;

    /*上传位置*/
    public void setRemark(String text) {
        if(uploadPostionService==null) {
            uploadPostionService = Executors.newFixedThreadPool(1);
        }
        uploadPostionService.submit(new SetRemarkRunnable(text));
    }

    private class SetRemarkRunnable implements Runnable {
        String text;

        public SetRemarkRunnable(String text) {
            this.text = text;
        }

        @Override
        public void run() {
            try {
                FormBody.Builder builder = new FormBody.Builder();
                builder.add("remark", text);
                RequestBody requestBody = builder.build();
                Request request = new Request.Builder()
                        .url("http://bus.mysdnu.cn/users/bind/:" + MQTTManager.getInstance().clientId)
                        .post(requestBody)
                        .build();
                Response response = Utils.client.newCall(request).execute();
                String responseData = response.body().string();
                String responseCode = String.valueOf(response.code());
                Log.e("setRemark", responseCode + " " + responseData);
                if (responseCode.charAt(0) == '2' && responseData.contains("success")) {
                    Utils.uiToast("备注成功");
                    uploadPosition();
                } else if (responseData.contains("place login")) {
                    MainActivity.getActivity().startActivity(new Intent(MainActivity.getActivity(), LoginActivity.class));
                } else {
                    Utils.uiToast("失败了...");
                }
            } catch (Exception e) {
                Utils.uiToast("出现了一些问题");
                e.printStackTrace();
            }
        }
    }
}
