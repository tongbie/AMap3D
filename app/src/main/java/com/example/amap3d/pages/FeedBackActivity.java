package com.example.amap3d.pages;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.amap3d.R;

public class FeedBackActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {
    private FrameLayout qqLayout, weixinLayout;
    private ImageView backImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_back);
        initView();
    }

    private void initView() {
        qqLayout = findViewById(R.id.qqLayout);
        qqLayout.setOnClickListener(this);
        qqLayout.setOnLongClickListener(this);
        weixinLayout = findViewById(R.id.weixinLayout);
        weixinLayout.setOnClickListener(this);
        weixinLayout.setOnLongClickListener(this);
        backImage = findViewById(R.id.backImage);
        backImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onClick(View v) {
        judgeType(v.getId());
    }

    @Override
    public boolean onLongClick(View v) {
        judgeType(v.getId());
        return true;
    }

    private void judgeType(int id) {
        StringBuilder text=new StringBuilder("");
        switch (id) {
            case R.id.qqLayout:
                text.append("2489550615");
                break;
            case R.id.weixinLayout:
                text.append("13665365485");
                break;
            default:
        }
        copyToShearPlate(text.toString());
        Toast.makeText(this, "已复制到剪切板", Toast.LENGTH_SHORT).show();
    }

    private void copyToShearPlate(String text) {
        ClipboardManager clipboardManager = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        assert clipboardManager != null;
        clipboardManager.setPrimaryClip(ClipData.newPlainText(null, text));
        if (clipboardManager.hasPrimaryClip()) {
            clipboardManager.getPrimaryClip().getItemAt(0).getText();
        }
    }
}
