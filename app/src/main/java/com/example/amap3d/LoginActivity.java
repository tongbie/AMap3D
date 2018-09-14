package com.example.amap3d;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.amap3d.managers.PeopleManager;
import com.example.amap3d.managers.StorageManager;

public class LoginActivity extends AppCompatActivity {
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setWebView();
    }

    private void setWebView() {
        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);//支持JS
        webSettings.setUseWideViewPort(true); //将图片调整到适合webview的大小
        webSettings.setLoadWithOverviewMode(true); //缩放至屏幕的大小
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        //调用重构的WebViewClient
        webView.setWebViewClient(new MyWebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();//表示等待证书响应
            }
        });
        webView.loadUrl("http://bus.mysdnu.cn/login");
    }

    class MyWebViewClient extends WebViewClient {

        public boolean shouldOverrideUrlLoading(WebView webview, String url) {
            webview.loadUrl(url);
            return super.shouldOverrideUrlLoading(webview, url);
        }

        @Override
        public void onPageStarted(WebView webView, String url, Bitmap bitmap) {
            String returnUrl = webView.getUrl();
            if (returnUrl.contains("oauth_verifier") && (!returnUrl.contains("oauth_verifier=null"))) {
                try {
                    String[] oauthKeys = splitReturnUrl(returnUrl);
                    if (oauthKeys[0] != null && oauthKeys[1] != null) {
                        StorageManager.storage("oauth_token", oauthKeys[0]);
                        StorageManager.storage("oauth_verifier", oauthKeys[1]);
                        PeopleManager.getInstance().attemptLogin();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finish();
            }
            super.onPageStarted(webView, url, bitmap);
        }
    }

    private String[] splitReturnUrl(String s) {
        String[] oauthKeys = new String[]{null, null};
        String[] splittedStrings = s.split("#");
        String splittedString = "";
        for (String string : splittedStrings) {
            if (string.contains("oauth_token=") && string.contains("oauth_verifier=")) {
                splittedString = string;
                break;
            }
        }
        if (splittedString.length() > 0) {
            oauthKeys[0] = splittedString.substring(splittedString.indexOf("oauth_token=") + "oauth_token=".length(), splittedString.indexOf("&"));
            oauthKeys[1] = splittedString.substring(splittedString.indexOf("oauth_verifier=") + "oauth_verifier=".length());
        }
        return oauthKeys;
    }
}