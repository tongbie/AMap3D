package com.example.amap3d;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.amap3d.BusTimetable.BusTimetableActivity;
import com.example.amap3d.Managers.AMapManager;
import com.example.amap3d.Managers.BusDataManager;
import com.example.amap3d.Managers.MQTTManager;
import com.example.amap3d.Managers.UpdateManager;
import com.example.amap3d.Views.MenuButton;
import com.example.amap3d.Views.RefreshButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private MQTTManager mqttManager;
    private AMapManager aMapManager;
    private BusDataManager busDataManager;
    private PopupMenu popupMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.hideTitleBar(this);
        setContentView(R.layout.activity_main);
        new Utils(this,getApplicationContext());
        initView();
        aMapManager.initMapView(savedInstanceState);
        if (AMapManager.aMap == null) {
            AMapManager.aMap = AMapManager.mapView.getMap();
        }
        aMapManager.setAMap(AMapManager.aMap);
        aMapManager.setLocationStyle(AMapManager.aMap);
        getAllData();
        update(null);
    }

    private void update(final String text) {
        if (!Utils.checkNetworkState(getApplicationContext())) {
            Toast.makeText(this, "无网络连接", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                int updateState = UpdateManager.isNeedUpdate(getApplicationContext());
                if (updateState == UpdateManager.UPDATE_NOT_NEED) {
                    if (text != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    return;
                }
                UpdateManager updateManager = new UpdateManager();
                updateManager.dealWithUpdateState(updateState);
            }
        }).start();
    }

    private void initView() {
        mqttManager = new MQTTManager();
        aMapManager = new AMapManager(getApplicationContext(), MainActivity.this);
        busDataManager = new BusDataManager();

        findViewById(R.id.refresh).setOnClickListener(this);
        findViewById(R.id.menu).setOnClickListener(this);

//        final CoordinatorLayout coordinatorLayout = findViewById(R.id.coordinatorLayout);
//        final NestedScrollView nestedScrollView = findViewById(R.id.nestedScrollView);
        final TextView upwardSlideTextView = findViewById(R.id.upTextView);
        final TextView downwardSlideTextView = findViewById(R.id.downTextView);
        ((AppBarLayout) findViewById(R.id.appBarLayout)).addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (verticalOffset < 0) {
                    upwardSlideTextView.setText("下拉返回");
                    downwardSlideTextView.setVisibility(View.GONE);
                } else if (verticalOffset == 0) {
                    upwardSlideTextView.setText("上滑查看发车时间");
                }
            }
        });
        initPopupMenu();
    }

    private void initPopupMenu() {
        final MenuButton menuButton = findViewById(R.id.menu);
        popupMenu = new PopupMenu(getApplicationContext(), menuButton);
        popupMenu.getMenuInflater().inflate(R.menu.main, popupMenu.getMenu());
        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
            @Override
            public void onDismiss(PopupMenu menu) {
                menuButton.setIsShow(0);
            }
        });
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.timeTable:
                        startActivity(new Intent(MainActivity.this, BusTimetableActivity.class));
                        break;
                    case R.id.upDate:
                        update("已是最新版本");
                        break;
                }
                return false;
            }
        });
    }

    private synchronized void getAllData() {
        if (!Utils.checkNetworkState(getApplicationContext())) {
            Toast.makeText(this, "无网络连接", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                busDataManager.setBusInformationToMap();
                Datas.busPositionList = busDataManager.getBusPosition();
                aMapManager.addPoints(AMapManager.aMap);
                mqttManager.linkMQTT(mqttManager.mqttCallback);
                ((RefreshButton) findViewById(R.id.refresh)).setRefreshing(false);
                isRefreshing = false;
            }
        }).start();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.refresh:
                ((RefreshButton) view).setRefreshing(true);
                Toast.makeText(this, "正在刷新...", Toast.LENGTH_SHORT).show();
                refresh();
                break;
            case R.id.menu:
                MenuButton menuButton = ((MenuButton) view);
                if (menuButton.getIsShow() != 1) {
                    menuButton.setIsShow(1);
                } else if (menuButton.getIsShow() == 1) {
                    menuButton.setIsShow(0);
                }
                popupMenu.show();
                break;
        }
    }

    private boolean isRefreshing = false;

    private void refresh() {
        if (isRefreshing) {
            return;
        }
        if (!Utils.checkNetworkState(this)) {
            Toast.makeText(this, "无网络连接", Toast.LENGTH_SHORT).show();
            return;
        }
        AMapManager.aMap.clear();
        Datas.busInformationMap.clear();
        Datas.busPositionList.clear();
        Datas.busMarkerMap.clear();
        isRefreshing = true;
        getAllData();
    }


    /*----------------------------------------------------------------------------------------------------------------------------------------------*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0x003:
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Utils.uiToast("未获得拨号权限，无法拨打电话");
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AMapManager.mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        AMapManager.mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        AMapManager.mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AMapManager.mapView.onDestroy();
        mqttManager.disconnect();
        System.exit(0);
    }
}
