package com.example.amap3d;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissions();
    }

    private List<String> permissionList;

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionList = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(WelcomeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (ContextCompat.checkSelfPermission(WelcomeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (permissionList.isEmpty()) {
                startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                finish();
            } else {
                requestPermissions(permissionList.toArray(new String[permissionList.size()]), 0x000);
            }
        } else {
            startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0x000:
                boolean isAllPermissionAgreed = true;
                for (int i = 0; i < permissions.length; i++) {
                    if (ContextCompat.checkSelfPermission(WelcomeActivity.this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                        isAllPermissionAgreed = false;
                    }
                }
                if (isAllPermissionAgreed) {
                    startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                    finish();
                }else {
                    Toast.makeText(WelcomeActivity.this, "未获得全部权限，无法使用该功能", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }
}
