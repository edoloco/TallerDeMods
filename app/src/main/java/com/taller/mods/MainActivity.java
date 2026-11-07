package com.taller.mods;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private class Bridge {
        @JavascriptInterface
        public String enviar(String nombre, String base64) {
            try {
                byte[] datos = Base64.decode(base64, Base64.DEFAULT);
                File dir = new File(getCacheDir(), "addons");
                dir.mkdirs();
                final File archivo = new File(dir, nombre + ".mcaddon");
                FileOutputStream salida = new FileOutputStream(archivo);
                salida.write(datos);
                salida.close();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        abrirEnMinecraft(archivo);
                    }
                });
                return "ok";
            } catch (Exception e) {
                return "no se pudo guardar el archivo";
            }
        }
    }

    private void abrirEnMinecraft(File archivo) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", archivo);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/octet-stream");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setPackage("com.mojang.minecraftpe");
        try {
            startActivity(intent);
        } catch (Exception e) {
            Intent otro = new Intent(Intent.ACTION_VIEW);
            otro.setDataAndType(uri, "application/octet-stream");
            otro.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(otro, "Abrir con"));
        }
    }
}
