package com.example.amap3d.managers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
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
import com.example.amap3d.utils.Utils;
import com.example.amap3d.views.MapViewContainerView;

import java.util.Arrays;

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
        MapViewContainerView mapViewContainerView = MainActivity.getActivity().findViewById(R.id.mapViewContainerView);
        mapViewContainerView.addView(AMapManager.mapView);
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
    private AMap.InfoWindowAdapter infoWindowAdapter = new AMap.ImageInfoWindowAdapter() {
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
        @SuppressLint("InflateParams")
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
    private AMap.OnInfoWindowClickListener infoWindowClickListener = new AMap.OnInfoWindowClickListener() {
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
                                ActivityCompat.requestPermissions(MainActivity.getActivity(), new String[]{Manifest.permission.CALL_PHONE}, 0x003);
                            } else {
//                                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + num));
//                                MainActivity.getActivity().startActivity(intent);
//                                dialog.dismiss();
                                Toast.makeText(MainActivity.getActivity(), "测试版暂时关闭拨号功能", Toast.LENGTH_SHORT).show();
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
    private AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(final Marker marker) {
            final String title = marker.getTitle();
            if (title == null) {
                return true;
            } else if (title.contains("ID")) {
                if (!marker.isInfoWindowShown()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final String deviceId = title.substring(2);
                            PeopleManager.getInstance().requireRemark(deviceId, marker);
                        }
                    }).start();
                }
            }
            if (marker.isInfoWindowShown()) {
                marker.hideInfoWindow();
            } else {
                marker.showInfoWindow();
            }
            return true;
        }
    };

    /* 添加校车定位点 */
    public synchronized void addBusMarker() {
        if (Datas.busPositionList == null) {
            Utils.uiToast("校车位置获取失败");
            return;
        }
        for (BusPositionGson busPosition : Datas.busPositionList) {
            String key = busPosition.getGPSDeviceIMEI();
            LatLng latLng = new LatLng(Double.parseDouble(busPosition.getLat()), Double.parseDouble(busPosition.getLng()));
            Marker marker = aMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(Datas.busInformationMap.get(key)[0])
                    .snippet(Datas.busInformationMap.get(key)[1]));
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.bus1));
            Datas.busMarkerMap.put(key, marker);
        }
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
            public void move(double distance) {
                if (distance == 0) {
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

    private long lastUploadTime = 0;

    private Handler tiemHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            tiemHandler.sendEmptyMessageDelayed(0, 5000);
            long currentTime = System.currentTimeMillis();
            if (onPositionChangedListener != null && (currentTime - lastUploadTime) > 5000) {
                onPositionChangedListener.onPositionChanged(lng, lat);
                lastUploadTime = currentTime;
            }
            return false;
        }
    });

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
                    if (location.getLatitude() != lat && location.getLongitude() != lng) {
                        lat = location.getLatitude();
                        lng = location.getLongitude();
                        onPositionChangedListener.onPositionChanged(lng, lat);
                        lastUploadTime = System.currentTimeMillis();
                    }
                }
            }
        });

        aMap.setOnMarkerClickListener(markerClickListener);
        UiSettings mUiSettings;
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
        tiemHandler.sendEmptyMessageDelayed(0, 5000);
    }

//    private void location() {
//        AMapLocationClient aMapLocationClient;
//        AMapLocationClientOption aMapLocationClientOption;
//        aMapLocationClient = new AMapLocationClient(MainActivity.getActivity());
//        aMapLocationClient.setLocationListener(this);
//        aMapLocationClientOption = new AMapLocationClientOption();
//        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
//        aMapLocationClientOption.setNeedAddress(true);
//        aMapLocationClientOption.setOnceLocation(false);
//        aMapLocationClientOption.setWifiActiveScan(true);
//        aMapLocationClientOption.setMockEnable(false);
//        aMapLocationClientOption.setInterval(1000);
//        aMapLocationClientOption.setLocationCacheEnable(false);
//        aMapLocationClientOption.setOnceLocationLatest(true);
//        aMapLocationClient.setLocationOption(aMapLocationClientOption);
//        aMapLocationClient.startLocation();
//
//    }
//
//
//    @Override
//    public void onLocationChanged(AMapLocation aMapLocation) {
//        StringBuilder userPosition = new StringBuilder("");
//        userPosition.append(aMapLocation.getProvince());
//        userPosition.append(aMapLocation.getCity());
//        userPosition.append(aMapLocation.getDistrict());
//        userPosition.append(aMapLocation.getStreet());
//        Toast.makeText(MainActivity.getActivity(), userPosition.toString(), Toast.LENGTH_SHORT).show();
//    }
}
