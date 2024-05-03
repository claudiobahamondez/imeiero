package com.example.imeiero;
import android.app.Application;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializa el CookieManager global
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
    }
}