package com.ebmp.mobile;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

/**
 * 暴露给 H5 的原生能力：服务器地址与登录令牌持久化、轻提示。
 * 使用 SharedPreferences（原生侧）而非 localStorage，避免 file:// 场景下存储不稳定。
 */
public class ConfigBridge {

    private static final String PREF = "ebmp_mobile";
    private static final String KEY_API_BASE = "api_base";
    private static final String KEY_TOKEN = "token";
    private static final String DEFAULT_API_BASE = "http://10.0.2.2:8080";

    private final Activity activity;
    private final SharedPreferences prefs;

    public ConfigBridge(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    @JavascriptInterface
    public String getApiBase() {
        return prefs.getString(KEY_API_BASE, DEFAULT_API_BASE);
    }

    @JavascriptInterface
    public void setApiBase(final String value) {
        prefs.edit().putString(KEY_API_BASE, value == null ? "" : value.trim()).apply();
    }

    @JavascriptInterface
    public String getToken() {
        return prefs.getString(KEY_TOKEN, "");
    }

    @JavascriptInterface
    public void setToken(final String value) {
        prefs.edit().putString(KEY_TOKEN, value == null ? "" : value).apply();
    }

    @JavascriptInterface
    public void clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    @JavascriptInterface
    public String appVersion() {
        return "1.0.0";
    }

    @JavascriptInterface
    public void toast(final String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        activity.runOnUiThread(() -> Toast.makeText(activity, message, Toast.LENGTH_SHORT).show());
    }
}
