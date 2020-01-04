package com.example.amap3d.datas;

import com.amap.api.maps.model.Marker;
import com.example.amap3d.gsons.AdressGson;
import com.example.amap3d.gsons.BusPositionGson;
import com.example.amap3d.gsons.UserInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by BieTong on 2018/5/13.
 */

public class Datas {
    private static HashMap<String, String[]> busInformationMap = new HashMap<>(16);//校车信息
    private static HashMap<String, Marker> busMarkerMap = new HashMap<>(16);//校车定位点
    private static HashMap<String, Marker> peopleMap = new HashMap<>();//人员位置

    private static List<AdressGson> adressList = new ArrayList<>(4);//地址
    private static List<BusPositionGson> busPositionList = new ArrayList<>();//校车位置

    private static String currentInfoWindowRemark = "";//用户上传位置备注
    private static int currentSelectorId = 0;

    public static void clear() {
        busInformationMap.clear();
        busMarkerMap.clear();
        busPositionList.clear();
        adressList.clear();
        peopleMap.clear();
    }

    public static void destroy() {
        clear();
//        busInformationMap = null;
//        busMarkerMap = null;
//        busPositionList = null;
//        peopleMap = null;
//        userInfo = null;
    }

    private static UserInfo userInfo = new UserInfo();

    public synchronized static HashMap<String, String[]> getBusInformationMap() {
        return busInformationMap;
    }

    public synchronized static HashMap<String, Marker> getBusMarkerMap() {
        return busMarkerMap;
    }

    public synchronized static List<BusPositionGson> getBusPositionList() {
        return busPositionList;
    }

    public synchronized static void setBusPositionList(List<BusPositionGson> busPositionList) {
        Datas.busPositionList = busPositionList;
    }

    public synchronized static HashMap<String, Marker> getPeopleMap() {
        return peopleMap;
    }

    public synchronized static String getCurrentInfoWindowRemark() {
        return currentInfoWindowRemark;
    }

    public synchronized static void setCurrentInfoWindowRemark(String currentInfoWindowRemark) {
        Datas.currentInfoWindowRemark = currentInfoWindowRemark;
    }

    public synchronized static UserInfo getUserInfo() {
        return userInfo;
    }

    public synchronized static void setUserInfo(UserInfo userInfo) {
        Datas.userInfo = userInfo;
    }

    public synchronized static void setAdressList(List<AdressGson> var) {
        adressList = var;
    }

    public synchronized static List<AdressGson> getAdressList() {
        return adressList;
    }

    public synchronized static String getAdress(int i) {
        String adress = "";
        for (AdressGson a : adressList) {
            if (i == a.getId()) {
                adress = a.getName();
                break;
            }
        }
        return adress;
    }

    public synchronized static void setCurrentSelectorId(int id) {
        currentSelectorId = id;
    }

    public synchronized static int getCurrentSelectorId() {
        return currentSelectorId;
    }
}
