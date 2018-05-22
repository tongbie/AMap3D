package com.example.amap3d;

import android.app.Activity;
import android.content.Context;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.Marker;
import com.example.amap3d.Gsons.BusPositionGson;

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

    public static Activity activity;
    public static Context context;
}
