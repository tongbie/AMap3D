package com.example.amap3d;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class WelcomeActivity extends AppCompatActivity {
    List<String> permissionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        requestPermissions();
    }

    private void init(){
        permissionList=new ArrayList<>();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(WelcomeActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
            } else if (ContextCompat.checkSelfPermission(WelcomeActivity.this, android.Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(android.Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS);
            }
            else if (ContextCompat.checkSelfPermission(WelcomeActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(android.Manifest.permission.READ_PHONE_STATE);
            }
            if (!permissionList.isEmpty()) {
                String[] permissions = permissionList.toArray(new String[permissionList.size()]);
                ActivityCompat.requestPermissions(WelcomeActivity.this, permissions, 0x000);
            }else {
                startActivity(new Intent(WelcomeActivity.this,MainActivity.class));
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(!permissionList.isEmpty()){
            Toast.makeText(getApplicationContext(),"请允许所有权限后重启应用",Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
