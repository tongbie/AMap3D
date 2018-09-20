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
import com.example.amap3d.gsons.PeopleRemarkGson;
import com.example.amap3d.utils.Utils;
import com.example.amap3d.datas.Datas;
import com.example.amap3d.gsons.UploadPositionGson;
import com.google.gson.reflect.TypeToken;

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
    public void requireAllPosition() throws Exception {
        String url = "http://bus.mysdnu.cn/client";
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = Utils.client.newCall(request).execute();
        String data = response.body().string();
        int code = response.code();
        if (code == 200) {
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
    }

    private UploadPositionGson uploadPositionGson;

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
        }
        Utils.uiToast("位置上传中...");
    }

    /*destroy*/
    public void destroy() {
        uploadPositionGson = null;
        AMapManager.getInstance().setOnPositionChangedListener(null);
        try {
            if (MQTTManager.getInstance().mqttClient != null) {
                MQTTManager.getInstance().mqttClient.unsubscribe(uploadPositionTitle);
            }
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
                            return StorageManager.getCookieList(Datas.storageCookie);
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
                            return StorageManager.getCookieList(Datas.storageCookie);
                        }
                    })
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();
        }
        return getRemarkClient;
    }

    private void attemptLogin() {
        if (!Datas.isLogin) {
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
                    Log.e("attemptLogin", body);
                    if (code.charAt(0) == '2') {
                        Datas.isLogin = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    deleteKey();
                }
            }
        }
    }

    /*上传备注*/
    public void uploadRemark(String text) {
        if (uploadPostionService == null) {
            uploadPostionService = Executors.newFixedThreadPool(1);
        }
        uploadPostionService.submit(new UploadRemarkRunnable(text));
    }

    private class UploadRemarkRunnable implements Runnable {
        String text;

        UploadRemarkRunnable(String text) {
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
                        .url("http://bus.mysdnu.cn/users/bind/" + MQTTManager.deviceId)
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

    public void mqttUpload(MqttMessage message) {
        try {
            String data = message.toString();
            UploadPositionGson peopleGson = Utils.gson.fromJson(data, UploadPositionGson.class);
            String deviceId = peopleGson.getDeviceId();
            boolean isShowingInfoWindow = false;
            if (Datas.peopleMap.containsKey(deviceId)) {
                Marker marker = Datas.peopleMap.get(deviceId);
                if (marker.isInfoWindowShown()) {
                    isShowingInfoWindow = true;
                }
                marker.remove();
                Datas.peopleMap.remove(deviceId);
            }
            LatLng latLng = new LatLng(Double.parseDouble(peopleGson.getLat()), Double.parseDouble(peopleGson.getLng()));
            Marker marker = AMapManager.aMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .snippet(Datas.currentInfoWindowRemark)
                    .title("ID" + deviceId));
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.people));
            if (isShowingInfoWindow) {
                marker.showInfoWindow();
            }
            Datas.peopleMap.put(deviceId, marker);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteKey() {
        StorageManager.delete("oauth_token");
        StorageManager.delete("oauth_verifier");
        StorageManager.delete(Datas.storageCookie);
    }

    public void requireRemark(String deviceId, final Marker marker) {
        String remark;
        Request request = new Request.Builder()
                .url("http://bus.mysdnu.cn/users/reportInfo/" + deviceId)
                .build();
        try {
            Response response = getRemarkClient().newCall(request).execute();
            String code = String.valueOf(response.code());
            String data = response.body().string();
            if (code.charAt(0) == '2') {
                PeopleRemarkGson peopleRemarkGson = Utils.gson.fromJson(data, PeopleRemarkGson.class);
                remark = peopleRemarkGson.getRemark();
                Datas.currentInfoWindowRemark = remark;
                final String finalRemark = remark;
                MainActivity.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        marker.setSnippet(finalRemark);
                        String deviceId = marker.getTitle().substring(2);
                        LatLng latLng = marker.getPosition();
                        Datas.peopleMap.get(deviceId).remove();
                        Datas.peopleMap.remove(deviceId);
                        Marker newMarker = AMapManager.aMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .snippet(finalRemark)
                                .title("ID" + deviceId));
                        newMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.people));
                        Datas.peopleMap.put(deviceId, newMarker);
                        newMarker.showInfoWindow();
                    }
                });
            } else if (data.contains("place login")) {
                Utils.uiToast("上传位置后可查看人员信息");
            } else {
                throw new RuntimeException("PeopleManager.requireRemark");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.uiToast("获取人员信息失败");
        }
    }
}
