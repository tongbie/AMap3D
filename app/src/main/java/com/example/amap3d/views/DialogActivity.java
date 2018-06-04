package com.example.amap3d.views;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.amap3d.R;

public class DialogActivity extends AppCompatActivity {
    private static DialogActivity dialogActivity;
    private DialogButtonClickEvent dialogButtonClickEvent;

    private Button leftButton;
    private Button rightButton;
    private TextView updateLogView;

    private String updateLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);
        initView();
        dialogActivity = this;
    }

    private void initView() {
        leftButton = findViewById(R.id.leftButton);
        rightButton = findViewById(R.id.rightButton);
        updateLogView = findViewById(R.id.updateLogView);
    }

    public static DialogActivity getInstance() {
        if (dialogActivity == null) {
            dialogActivity = new DialogActivity();
        }
        return dialogActivity;
    }

    public void setUpdateLog(String text) {
        updateLogView.setText(text);
    }

    public interface DialogButtonClickEvent{
        void leftButtonClickEvent(View v);
        void rightButtonClickEvent(View v);
    }
}
