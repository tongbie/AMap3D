package com.example.amap3d.datas;

import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.example.amap3d.gsons.BusPositionGson;
import com.example.amap3d.gsons.UserInfo;

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

    public static SmoothMoveMarker smoothMarker;
    public static List<LatLng> latLngList = new ArrayList<>();

    public static String currentInfoWindowRemark = "";//用户上传位置备注

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
        userInfo = null;
    }

    public static UserInfo userInfo = new UserInfo();
}
