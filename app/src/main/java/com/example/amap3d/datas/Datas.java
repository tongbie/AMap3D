package com.example.amap3d.datas;

import com.amap.api.maps.model.Marker;
import com.example.amap3d.gsons.BusPositionGson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by BieTong on 2018/5/13.
 */

public class Datas {
    public static HashMap<String, String[]> busInformationMap = new HashMap<>();//校车信息
    public static HashMap<String, Marker> busMarkerMap = new HashMap<>();//校车定位点
    public static List<BusPositionGson> busPositionList = new ArrayList<>();//校车位置
    public static HashMap<String, Marker> peopleMap = new HashMap<>();//人员位置
    public static HashMap<String, String> peopleRemarkList = new HashMap<>();//人员信息

    public static final String storageRemark = "storageRemark";
    public static final String storageCookie = "storageCookie";

    public static boolean isLogin = false;

    public static void clear() {
        busInformationMap.clear();
        busMarkerMap.clear();
        busPositionList.clear();
        peopleMap.clear();
        peopleRemarkList.clear();
    }

    public static void destroy() {
        clear();
        busInformationMap = null;
        busMarkerMap = null;
        busPositionList = null;
        peopleMap = null;
        peopleRemarkList = null;
    }
}
