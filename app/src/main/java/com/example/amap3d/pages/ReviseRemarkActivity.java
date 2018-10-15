package com.example.amap3d.pages;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.amap3d.MainActivity;
import com.example.amap3d.R;
import com.example.amap3d.datas.Fields;
import com.example.amap3d.managers.PeopleManager;
import com.example.amap3d.managers.StorageManager;

public class ReviseRemarkActivity extends AppCompatActivity {
    EditText remarkEdit;
    TextView wordCountTextView;
    Button uploadButton;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revise_remark);
        initView();
    }

    private void initView() {
        remarkEdit = findViewById(R.id.remarkEdit);
        wordCountTextView = findViewById(R.id.wordCountTextView);
        uploadButton = findViewById(R.id.uploadButton);
        findViewById(R.id.backImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        wordCountTextView.setText("(0/50)");
        String positionRemark = StorageManager.get(Fields.STORAGE_REMARK);
        if (positionRemark != null) {
            remarkEdit.setText(positionRemark);
            wordCountTextView.setText("(" + positionRemark.length() + "/50)");
        }
        remarkEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return false;
            }
        });
        remarkEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void afterTextChanged(Editable s) {
                int content = remarkEdit.getText().length();
                wordCountTextView.setText("(" + content + "/50)");
            }
        });
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = remarkEdit.getText().toString();
                if (text.length() > 50) {
                    Toast.makeText(MainActivity.getInstance(), "字数超过限制", Toast.LENGTH_SHORT).show();
                } else {
                    PeopleManager.getInstance().uploadRemark(text, false);
                    StorageManager.storage(Fields.STORAGE_REMARK, text);
                }
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                IBinder iBinder = getWindow().getDecorView().getWindowToken();
                inputMethodManager.hideSoftInputFromWindow(iBinder, 0);
            }
        });
    }
}
