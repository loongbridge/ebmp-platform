package com.ebmp.mobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * 企业商业管理平台移动端原生外壳。
 * 使用系统 WebView 加载打包在 assets/www 中的 H5 应用，不依赖 AndroidX / Gradle。
 */
public class MainActivity extends Activity {

    private static final String APP_URL = "file:///android_asset/www/index.html";

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(Color.parseColor("#13253d"));
        window.setNavigationBarColor(Color.parseColor("#ffffff"));

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        // 允许 file:// 页面发起跨域请求，保证 WebView 内可直接访问后端 API
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);

        webView.setBackgroundColor(Color.parseColor("#f4f6fa"));
        webView.addJavascriptInterface(new ConfigBridge(this), "NativeConfig");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new AppClient(this));

        if (savedInstanceState == null) {
            webView.loadUrl(APP_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
        setContentView(webView);
    }

    /** 电话、邮件、短信等协议交给系统处理，其余仍在应用内打开 */
    private boolean handleUrl(String url) {
        if (url == null) {
            return false;
        }
        if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:")) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception ignored) {
                // 设备无对应处理能力时忽略
            }
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("NativeConfig");
            webView.setVisibility(View.GONE);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    /** 使用命名内部类而非匿名类：build-tools 34 的 d8 对匿名内部类存在解析缺陷 */
    static class AppClient extends WebViewClient {

        private final MainActivity activity;

        AppClient(MainActivity activity) {
            this.activity = activity;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return activity.handleUrl(url);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return activity.handleUrl(request.getUrl().toString());
        }
    }
}
