package com.example.amap3d.Managers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.example.amap3d.Gsons.MQTTAccountGson;
import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by BieTong on 2018/3/20.
 */

public class MQTTManager {
    private Activity activity;
    private Context context;
    private MqttConnectOptions mqttOptions;
    private MqttClient mqttClient;
    public static OkHttpClient client;
    public static Gson gson;

    public MQTTManager(Context context, Activity activity){
        this.context=context;
        this.activity=activity;

        mqttOptions = new MqttConnectOptions();
        mqttOptions.setCleanSession(true);
        mqttOptions.setConnectionTimeout(20);//超时时间20s
        mqttOptions.setKeepAliveInterval(120);//心跳时间，用以服务端判断客户端在线状态

        client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    /* 连接MQTT服务器 */
    public void linkMQTT(MqttCallback mqttCallback) {
        try {
            Request request = new Request.Builder()
                    .url("http://111.231.201.179/bus/mqtt")
                    .build();
            Response response = client.newCall(request).execute();
            String responseData = response.body().string();
            String responseCode = String.valueOf(response.code());
            if (responseData != null && responseCode.charAt(0) == '2') {
                try {
                    Log.e("mqttRunnable", responseData);
                    MQTTAccountGson account = gson.fromJson(responseData, MQTTAccountGson.class);
                    String username = account.getUsername();
                    String password = account.getPassword();
                    TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    //使用设备码作为唯一标识key，需要电话权限
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(context, "未获得电话权限，程序无法正常使用", Toast.LENGTH_LONG).show();
                        activity.finish();
                    }
                    String ssn = tm.getSimSerialNumber();
                    //断线不支持重连，必须申请新的username
                    mqttOptions.setUserName(username);
                    mqttOptions.setPassword(password.toCharArray());
                    mqttClient = new MqttClient("tcp://111.231.201.179:1880", ssn, new MemoryPersistence());
                    if (mqttClient.isConnected()) {
                        toast("服务器已连接");
                    } else {
                        mqttClient.setCallback(mqttCallback);
                        mqttClient.connect(mqttOptions);
                        subscribeMsg("BusMoveList", 0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    toast("连接服务器失败");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* 订阅消息 */
    public void subscribeMsg(String topic, int qos) {
        if (mqttClient != null) {
            int[] Qos = {qos};
            String[] topic1 = {topic};
            try {
                mqttClient.subscribe(topic1, Qos);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /* 断开连接 */
    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private void toast(final String text) {
        try {
            activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
