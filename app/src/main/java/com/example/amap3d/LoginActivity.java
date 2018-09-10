package com.example.amap3d;

import android.content.Intent;
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

import com.example.amap3d.managers.MQTTManager;

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
        webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        //调用重构的WebViewClient
        webView.setWebViewClient(new MyWebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();    //表示等待证书响应
                // handler.cancel();      //表示挂起连接，为默认方式
                // handler.handleMessage(null);    //可做其他处理
            }
        });
        webView.loadUrl("http://bus.mysdnu.cn/login");
    }

    class MyWebViewClient extends WebViewClient {

        public boolean shouldOverrideUrlLoading(WebView webview, String url) {
            webview.loadUrl(url);
            return super.shouldOverrideUrlLoading(webview, url);
        }

        public void onPageFinished(WebView view, String url) {
            CookieManager cookieManager = CookieManager.getInstance();
            String CookieStr = cookieManager.getCookie(url);
//            Log.e("这是截取的Cookies", "Cookies = " + CookieStr);
            super.onPageFinished(view, url);
        }

        @Override
        public void onPageStarted(WebView webView, String url, Bitmap bitmap) {
            String returnUrl = webView.getUrl();
//            Log.i("这是截取的url", "url = " + returnUrl);
            if (returnUrl.contains("oauth_verifier") && (!returnUrl.contains("oauth_verifier=null"))) {
                String oauth_token = subString(returnUrl, "oauth_token=", "&");
                String oauth_verifier = subString(returnUrl + "#", "oauth_verifi.er=", "#");
                Log.e("onPageStarted", oauth_token + "\n" + oauth_verifier);
//                MQTTManager.getInstance().uploadPosition();
                finish();
            }
            super.onPageStarted(webView, url, bitmap);
        }
    }

    public String subString(String str, String strStart, String strEnd) {
        int strStartIndex, strEndIndex;
        String result = null;
        try {
            strStartIndex = str.indexOf(strStart);
            strEndIndex = str.indexOf(strEnd);
            result = str.substring(strStartIndex, strEndIndex).substring(strStart.length());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}