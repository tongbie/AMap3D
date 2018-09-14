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
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PeopleManager {
    private static PeopleManager peopleManager;
    private OkHttpClient loginCheckClient;
    private OkHttpClient getRemarkClient;

    public static PeopleManager getInstance() {
        if (peopleManager == null) {
            peopleManager = new PeopleManager();
        }
        return peopleManager;
    }

    /*启动时获取位置列表*/
    public void getAllPosition() {
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
                for (Map.Entry<String, Marker> entry : Datas.peopleMap.entrySet()) {
                    entry.getValue().remove();
                }
                Datas.peopleMap.clear();
                for (UploadPositionGson peopleGson : uploadPositionGsonList) {
                    LatLng latLng = new LatLng(Double.parseDouble(peopleGson.getLat()), Double.parseDouble(peopleGson.getLng()));
                    Marker marker = AMapManager.aMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title("ID" + peopleGson.getDeviceId()));
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
            uploadPositionGson.setDeviceId(MQTTManager.deviceId);
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
            if (MQTTManager.getInstance().mqttClient != null) {
                MQTTManager.getInstance().mqttClient.unsubscribe(uploadPositionTitle);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ExecutorService uploadPostionService;

    private OkHttpClient getLoginClient() {
        if (loginCheckClient == null) {
            loginCheckClient = new OkHttpClient.Builder()
                    .cookieJar(new CookieJar() {
                        @Override
                        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                            StorageManager.storage(Datas.storageCookie, cookies);
                        }

                        @Override
                        public List<Cookie> loadForRequest(HttpUrl url) {
                            return StorageManager.get(Datas.storageCookie, null);
                        }
                    })
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();
        }
        return loginCheckClient;
    }

    private OkHttpClient getRemarkClient() {
        if (getRemarkClient == null) {
            getRemarkClient = new OkHttpClient.Builder()
                    .cookieJar(new CookieJar() {
                        @Override
                        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {

                        }

                        @Override
                        public List<Cookie> loadForRequest(HttpUrl url) {
                            return StorageManager.get(Datas.storageCookie, null);
                        }
                    })
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();
        }
        return getRemarkClient;
    }

    public void attemptLogin() {
//        if (!Datas.isLogin) {
        String oauth_token = StorageManager.get("oauth_token");
        String oauth_verifier = StorageManager.get("oauth_verifier");
        if (oauth_token != null && oauth_verifier != null) {
            FormBody formBody = new FormBody.Builder()
                    .add("oauth_token", oauth_token)
                    .add("oauth_verifier", oauth_verifier)
                    .build();
            Request request = new Request.Builder()
                    .url("http://bus.mysdnu.cn/login")
                    .post(formBody)
                    .build();
            try {
                Response response = getLoginClient().newCall(request).execute();
                String code = String.valueOf(response.code());
                String body = response.body().string();
                if (code.charAt(0) == '2' && body.contains("success")) {
                    Datas.isLogin = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        }
    }

    /*上传位置*/
    public void uploadRemark(String text) {
        if (uploadPostionService == null) {
            uploadPostionService = Executors.newFixedThreadPool(1);
        }
        uploadPostionService.submit(new UploadRemarkRunnable(text));
    }

    private class UploadRemarkRunnable implements Runnable {
        String text;

        public UploadRemarkRunnable(String text) {
            this.text = text;
        }

        @Override
        public void run() {
            try {
                attemptLogin();
                FormBody.Builder builder = new FormBody.Builder();
                builder.add("remark", text);
                RequestBody requestBody = builder.build();
                Request request = new Request.Builder()
                        .url("http://bus.mysdnu.cn/users/bind/" + MQTTManager.getInstance().deviceId)
                        .post(requestBody)
                        .build();
                Response response = getLoginClient().newCall(request).execute();
                String responseCode = String.valueOf(response.code());
                String responseData = response.body().string();
                if (responseCode.charAt(0) == '2' && responseData.contains("success")) {
                    uploadPosition();
                } else if (responseData.contains("place login")) {
                    MainActivity.getActivity().startActivity(new Intent(MainActivity.getActivity(), LoginActivity.class));
                    Datas.isLogin = false;
                } else {
                    deleteKey();
                    Utils.uiToast("失败了...");
                }
            } catch (Exception e) {
                Utils.uiToast("出现了一些问题");
                e.printStackTrace();
            }
        }
    }

    public void upload(MqttMessage message) {
        try {
            String data = message.toString();
            UploadPositionGson peopleGson = Utils.gson.fromJson(data, UploadPositionGson.class);
            String deviceId = peopleGson.getDeviceId();
            if (MQTTManager.deviceId.equals(deviceId)) {
                return;
            }
            if (Datas.peopleMap.containsKey(deviceId)) {
                Datas.peopleMap.get(deviceId).remove();
                Datas.peopleMap.remove(deviceId);
            }
            LatLng latLng = new LatLng(Double.parseDouble(peopleGson.getLat()), Double.parseDouble(peopleGson.getLng()));
            Marker marker = AMapManager.aMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("ID" + deviceId));
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.people));
            Datas.peopleMap.put(deviceId, marker);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteKey() {
        StorageManager.delete("oauth_token");
        StorageManager.delete("oauth_verifier");
        StorageManager.delete(Datas.storageCookie);
    }

    public void getPeopleRemark(String deviceId) {
        if (Datas.isLogin) {
            FormBody formBody = new FormBody.Builder()
                    .add("deviceId", deviceId)
                    .build();
            Request request = new Request.Builder()
                    .url("http://bus.mysdnu.cn/users/reportInfo/" + deviceId)
                    .put(formBody)
                    .build();
            try {
                Response response = getRemarkClient().newCall(request).execute();
                String code = String.valueOf(response.code());
                String data = response.body().string();
                if (code.charAt(0) == '2' && data != null) {
                    Log.e("getPeopleRemark", data);
                    Utils.uiToast(data);
                } else {
                    throw new Exception("未获得数据 " + code + " " + data);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Utils.uiToast("获取人员信息失败");
            }
        }
    }
}
