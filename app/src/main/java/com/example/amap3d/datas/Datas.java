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
    private static String uploadPositionInformationText = "";

    public static void setUploadPositionInformationText(String text) {
        if (null == text) {
            text = "";
        }
        uploadPositionInformationText = text;
    }

    public static String getUploadPositionInformationText(){
        return uploadPositionInformationText;
    }

    public static void clear(){
        busInformationMap.clear();
        busMarkerMap.clear();
        busPositionList.clear();
    }

    public static void destroy() {
        busInformationMap.clear();
        busMarkerMap.clear();
        busPositionList.clear();
        busInformationMap = null;
        busMarkerMap = null;
        busPositionList = null;
    }
}
