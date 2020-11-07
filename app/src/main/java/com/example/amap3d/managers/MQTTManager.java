package com.example.amap3d.managers;

import android.os.Build;

import com.example.amap3d.datas.Fields;
import com.example.amap3d.gsons.MQTTAccountGson;
import com.example.amap3d.utils.Utils;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.SocketTimeoutException;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by BieTong on 2018/3/20.
 */

public class MQTTManager {
    private static MQTTManager mqttManager;
    private MqttConnectOptions mqttConnectOptions;
    public MqttClient mqttClient;
    public static String deviceId;

    public boolean isShowMoving = true;

    private static class MQTTManagerFactory {
        public static MQTTManager instance = new MQTTManager();
    }

    public static MQTTManager getInstance() {
        return MQTTManagerFactory.instance;
    }

    private MQTTManager() {
        mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(true);//服务端保留客户端连接记录
        mqttConnectOptions.setConnectionTimeout(10);//超时时间20s
        mqttConnectOptions.setKeepAliveInterval(120);//心跳时间，用以服务端判断客户端在线状态

        deviceId = "35" + Build.BOARD.length() % 10 +
                Build.BRAND.length() % 10 +
                Build.CPU_ABI.length() % 10 +
                Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 +
                Build.HOST.length() % 10 +
                Build.ID.length() % 10 +
                Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 +
                Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 +
                Build.TYPE.length() % 10 +
                Build.USER.length() % 10;
    }

    /* 连接MQTT服务器 */
    public synchronized void linkMQTT(MqttCallback mqttCallback) {
        if (mqttClient != null && mqttClient.isConnected()) {
            return;
        }
        try {
            Request request = new Request.Builder()
                    .url(Fields.URL_APPLY_FOR_MQTT)
                    .build();
            Response response = Utils.client.newCall(request).execute();
            String responseData = response.body().string();
            String responseCode = String.valueOf(response.code());
            if (responseCode.charAt(0) == '2') {
                MQTTAccountGson account = Utils.gson.fromJson(responseData, MQTTAccountGson.class);
                String username = account.getUsername();
                String password = account.getPassword();
                //断线不支持重连，必须申请新的username
                mqttConnectOptions.setUserName(username);
                mqttConnectOptions.setPassword(password.toCharArray());
                mqttClient = new MqttClient(Fields.URL_LINK_MQTT, deviceId, new MemoryPersistence());
                mqttClient.setCallback(mqttCallback);
                mqttClient.connect(mqttConnectOptions);
                subscribeTopic(Fields.MQTT_TOPIC_BUS_MOVE);
                subscribeTopic(Fields.MQTT_TOPIC_UPLOAD_POSITION);
                if (StorageManager.getSetting(Fields.SETTING_RECEIVE)) {
                    subscribeTopic("BusMoveLis"); //订阅主题
                }
            }
        } catch (SocketTimeoutException e) {
            Utils.uiToast("连接超时，请检查网络设置");
        } catch (Exception e) {
            e.printStackTrace();
            Utils.uiToast("连接服务器失败");
        }
    }

    /* 订阅消息 */
    public void subscribeTopic(String topic) {
        if (mqttClient != null) {
            try {
                mqttClient.subscribe(topic, 0);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /* 取消订阅 */
    public void unSubscribeTopic(String topic) {
        if (mqttClient != null) {
            try {
                mqttClient.unsubscribe(topic);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /* 推送消息 */
    public void publish(String topic, String msg, boolean isRetained) {
        try {
            int qos = 0;
            if (mqttClient != null) {
                MqttMessage message = new MqttMessage();
                message.setQos(qos);
                message.setRetained(isRetained);
                message.setPayload(msg.getBytes());
                mqttClient.publish(topic, message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            Utils.uiToast("失去连接，请刷新重试");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            if (isShowMoving && topic.equals(Fields.MQTT_TOPIC_BUS_MOVE)) {
                BusManager.getInstance().moveBus(message);
            } else if (topic.equals(Fields.MQTT_TOPIC_UPLOAD_POSITION)) {
                PeopleManager.getInstance().receiveMqttMessage(message);
            } else if (topic.equals("BusMoveLis")) {
                BusManager.getInstance().moveTestBus(message);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    };

    /* 断开连接 */
    private void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void destroy() {
        mqttClient = null;
        mqttConnectOptions = null;
        disconnect();
    }
}
