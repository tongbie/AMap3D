package com.example.amap3d.bustimetable;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.example.amap3d.R;

public class BusTimetableActivity extends AppCompatActivity {
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bus_timetable);
        initView();
    }

    private void initView() {
        recyclerView = findViewById(R.id.recycleView);
    }
}
