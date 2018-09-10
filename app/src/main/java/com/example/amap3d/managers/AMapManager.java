package com.example.amap3d.managers;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.example.amap3d.datas.Datas;
import com.example.amap3d.MainActivity;
import com.example.amap3d.gsons.BusPositionGson;
import com.example.amap3d.R;
import com.example.amap3d.Utils;
import com.example.amap3d.gsons.UploadPositionGson;
import com.example.amap3d.views.MapViewContainerView;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by BieTong on 2018/3/20.
 */

public class AMapManager {
    private static AMapManager aMapManager;
    public static AMap aMap;
    public static MapView mapView;

    private AMapManager() {
    }

    public static AMapManager getInstance() {
        if (aMapManager == null) {
            aMapManager = new AMapManager();
        }
        return aMapManager;
    }

    public void initMapView(Bundle savedInstanceState) {
        AMapManager.mapView = new MapView(MainActivity.getActivity());
        ((MapViewContainerView) MainActivity.getActivity().findViewById(R.id.mapViewContainerView)).addView(AMapManager.mapView);
        AMapManager.mapView.onCreate(savedInstanceState);
    }

    /* 设置定位模式 */
    public void setLocationStyle() {
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
        myLocationStyle.interval(1500);//连续定位间隔
        myLocationStyle.showMyLocation(true);
        myLocationStyle.radiusFillColor(Color.parseColor("#00000000"));//定位精度圈透明
        myLocationStyle.strokeColor(Color.parseColor("#00000000"));//定位精度圈边缘透明
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.me));
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.getUiSettings().setMyLocationButtonEnabled(true);
        aMap.setMyLocationEnabled(true);
    }

    /* 校车信息弹窗 */
    public AMap.InfoWindowAdapter infoWindowAdapter = new AMap.ImageInfoWindowAdapter() {
        View infoWindow = null;

        @Override
        public long getInfoWindowUpdateTime() {
            return 0;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        /* 自定义校车详情弹窗 */
        @Override
        public View getInfoContents(Marker marker) {
            if (infoWindow == null) {
                infoWindow = LayoutInflater.from(MainActivity.getActivity().getApplicationContext()).inflate(
                        R.layout.infowindow, null);
            }
            ((TextView) infoWindow.findViewById(R.id.text)).setText(marker.getTitle());
            Button button = infoWindow.findViewById(R.id.button);
            button.setClickable(false);
            final String num = marker.getSnippet();
            button.setText(num);
            return infoWindow;
        }
    };

    /* 校车信息弹窗点击事件，用以拨号 */
    public AMap.OnInfoWindowClickListener infoWindowClickListener = new AMap.OnInfoWindowClickListener() {
        @Override
        public void onInfoWindowClick(Marker marker) {
            final String num = marker.getSnippet();
            if (num == null) {
                return;
            } else {
                if (num.length() < 10) {
                    return;
                }
            }
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.getActivity())
                    .setTitle("拨号")
                    .setMessage("是否拨打 " + num)
                    .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (ContextCompat.checkSelfPermission(MainActivity.getActivity(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(MainActivity.getActivity(), new String[]{android.Manifest.permission.CALL_PHONE}, 0x003);
                            } else {
                                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + num));
                                MainActivity.getActivity().startActivity(intent);
                                dialog.dismiss();
                            }
                        }
                    })
                    .setPositiveButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            dialog.dismiss();
                        }
                    }).create();
            dialog.show();
        }
    };

    /* 校车标记点击事件，用以弹窗 */
    public AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            if (marker.isInfoWindowShown()) {
                marker.hideInfoWindow();
            } else {
                marker.showInfoWindow();
            }
            return true;
        }
    };

    /* 添加校车定位点 */
    public synchronized void addPoints() {
        if (Datas.busPositionList == null) {
            Utils.uiToast("校车位置获取失败");
            return;
        }
        try {
            for (BusPositionGson busPosition : Datas.busPositionList) {
                String key = busPosition.getGPSDeviceIMEI();
                LatLng latLng = new LatLng(Double.parseDouble(busPosition.getLat()), Double.parseDouble(busPosition.getLng()));
                Marker marker = aMap.addMarker(new MarkerOptions().position(latLng).title(Datas.busInformationMap.get(key)[0]).snippet(Datas.busInformationMap.get(key)[1]));
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bus1));
                Datas.busMarkerMap.put(key, marker);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.uiToast(e.getMessage());
        }
    }

    public void addPeople() {

    }

    /* 设置平滑移动点 */
    public void moveMarker(LatLng[] latLngs, final String key) {
        //TODO：移动点是一个新的对象，不能添加信息
        final SmoothMoveMarker smoothMarker = new SmoothMoveMarker(aMap);
        smoothMarker.setDescriptor(BitmapDescriptorFactory.fromResource(R.drawable.bus_move));
        smoothMarker.setPoints(Arrays.asList(latLngs));
        smoothMarker.setTotalDuration(3);
        smoothMarker.startSmoothMove();
        Datas.busMarkerMap.get(key).setVisible(false);
        smoothMarker.setMoveListener(new SmoothMoveMarker.MoveListener() {
            @Override
            public void move(double v) {
                if (v == 0) {//参数v为距终点距离
                    smoothMarker.stopMove();
                    smoothMarker.removeMarker();
                    Datas.busMarkerMap.get(key).setVisible(true);
                }
            }
        });
    }

    public void removeAllMarker() {
        aMap.clear();
    }

    private boolean isFirstMove = true;//用以判断在启动时移动地图至定位点

    private double lat = 0, lng = 0;

    /*设置aMap对象*/
    public void setAMap() {
        aMap.moveCamera(CameraUpdateFactory.zoomTo(17));//缩放
        aMap.setOnMyLocationChangeListener(new AMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if (isFirstMove) {
                    aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
                    isFirstMove = false;
                }
                if (onPositionChangedListener != null) {
//                    if (location.getLatitude() != lat && location.getLongitude() != lng) {
                    lat = location.getLatitude();
                    lng = location.getLongitude();
                    onPositionChangedListener.onPositionChanged(lng, lat);
//                    }
                }
            }
        });
        aMap.setOnMarkerClickListener(markerClickListener);

        UiSettings mUiSettings;//地图不可旋转，3D
        mUiSettings = aMap.getUiSettings();
        mUiSettings.setRotateGesturesEnabled(false);
        mUiSettings.setTiltGesturesEnabled(false);

        aMap.setInfoWindowAdapter(infoWindowAdapter);
        aMap.setOnInfoWindowClickListener(infoWindowClickListener);
    }

    public void destroy() {
        mapView.onDestroy();
        mapView = null;
        aMap.clear();
        aMap = null;
    }

    private OnPositionChangedListener onPositionChangedListener;

    public interface OnPositionChangedListener {

        void onPositionChanged(double longitude, double latitude);
    }

    public void setOnPositionChangedListener(OnPositionChangedListener onPositionChangedListener) {
        this.onPositionChangedListener = onPositionChangedListener;
    }
}
