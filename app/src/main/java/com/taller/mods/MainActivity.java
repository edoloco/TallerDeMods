package com.taller.mods;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MainActivity extends Activity {

    /** Repositorio donde se publican las versiones nuevas (pestaña Releases). */
    private static final String REPO = "edoloco/TallerDeMods";
    private static final String API_ULTIMA =
            "https://api.github.com/repos/" + REPO + "/releases/latest";

    private WebView web;
    private String urlApkNueva = null;
    private File apkPendiente = null;
    private volatile boolean descargando = false;

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
    protected void onResume() {
        super.onResume();
        // Si el usuario volvió de dar el permiso "instalar apps", seguimos con la instalación.
        if (apkPendiente != null && puedeInstalar()) {
            File f = apkPendiente;
            apkPendiente = null;
            llamarJs("onDescarga", "{\"ok\":true}");
            instalar(f);
        }
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript(
                "(function(){if(typeof scr!=='undefined'&&scr!=='home'){go('home');return 'atras';}return 'salir';})()",
                new android.webkit.ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String valor) {
                        if (valor == null || !valor.contains("atras")) {
                            finish();
                        }
                    }
                });
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

        // ---- Leer modelos del Minecraft instalado en este teléfono ----

        /** "ok", "no_mc" (no está instalado) o "no_assets:..." (no se encontraron los modelos). */
        @JavascriptInterface
        public String mcStatus() {
            return mcIndexar();
        }

        /** Lista (JSON) de archivos cuyo camino empieza con el prefijo, ej. "entity/". */
        @JavascriptInterface
        public String mcFiles(String prefijo) {
            if (!"ok".equals(mcIndexar())) return "[]";
            JSONArray a = new JSONArray();
            List<String> l = new ArrayList<>();
            for (String k : mc.idx.keySet()) if (k.startsWith(prefijo)) l.add(k);
            Collections.sort(l);
            for (String k : l) a.put(k);
            return a.toString();
        }

        /** Diagnóstico: paquetes encontrados y cuántos archivos tiene cada uno. */
        @JavascriptInterface
        public String mcInfo() {
            mcIndexar();
            return mc == null ? "" : mc.resumen.toString();
        }

        @JavascriptInterface
        public String mcText(String rel) {
            byte[] b = mcLeer(rel);
            if (b == null) return "";
            try {
                String t = new String(b, "UTF-8");
                return t.startsWith("\uFEFF") ? t.substring(1) : t;
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public String mcB64(String rel) {
            byte[] b = mcLeer(rel);
            return b == null ? "" : Base64.encodeToString(b, Base64.NO_WRAP);
        }

        @JavascriptInterface
        public String version() {
            return versionActual();
        }

        /** Busca en GitHub si hay una versión nueva. Responde llamando a window.onActualizacion(json). */
        @JavascriptInterface
        public void buscarActualizacion() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    JSONObject r = new JSONObject();
                    try {
                        r.put("actual", versionActual());
                        JSONObject rel = new JSONObject(descargarTexto(API_ULTIMA));
                        String tag = rel.optString("tag_name", "");
                        String apk = null;
                        JSONArray assets = rel.optJSONArray("assets");
                        if (assets != null) {
                            for (int i = 0; i < assets.length(); i++) {
                                JSONObject a = assets.getJSONObject(i);
                                if (a.optString("name").toLowerCase().endsWith(".apk")) {
                                    apk = a.optString("browser_download_url");
                                    break;
                                }
                            }
                        }
                        r.put("nueva", limpiar(tag));
                        r.put("notas", rel.optString("body", ""));
                        boolean hay = apk != null && esMasNueva(tag, versionActual());
                        r.put("hay", hay);
                        urlApkNueva = hay ? apk : null;
                    } catch (Exception e) {
                        try {
                            r.put("hay", false);
                            r.put("error", "sin internet o sin versiones publicadas");
                        } catch (Exception ignorar) { }
                    }
                    llamarJs("onActualizacion", r.toString());
                }
            }).start();
        }

        /** Descarga la APK nueva y abre el instalador de Android. */
        @JavascriptInterface
        public String actualizar() {
            final String url = urlApkNueva;
            if (url == null) return "no hay versión nueva";
            if (descargando) return "ya se está descargando";
            descargando = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        File dir = new File(getCacheDir(), "updates");
                        dir.mkdirs();
                        final File apk = new File(dir, "TallerDeMods.apk");
                        descargarArchivo(url, apk);
                        llamarJs("onDescarga", "{\"ok\":true}");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                instalar(apk);
                            }
                        });
                    } catch (Exception e) {
                        llamarJs("onDescarga", "{\"ok\":false}");
                    } finally {
                        descargando = false;
                    }
                }
            }).start();
            return "ok";
        }
    }

    // ---------- Minecraft instalado ----------

    private McIndice mc = null;
    private String mcEstado = null;

    private synchronized String mcIndexar() {
        if (mcEstado != null) return mcEstado;
        ApplicationInfo ai;
        try {
            ai = getPackageManager().getApplicationInfo("com.mojang.minecraftpe", 0);
        } catch (Exception e) {
            return mcEstado = "no_mc";
        }
        List<String> apks = new ArrayList<>();
        apks.add(ai.sourceDir);
        if (ai.splitSourceDirs != null) Collections.addAll(apks, ai.splitSourceDirs);
        McIndice m = new McIndice();
        m.indexar(apks);
        mc = m;
        if (m.idx.isEmpty()) return mcEstado = "no_assets:" + m.detalle + " | apks: " + apks.size();
        return mcEstado = "ok";
    }

    private byte[] mcLeer(String rel) {
        if (!"ok".equals(mcIndexar())) return null;
        return mc.leer(rel);
    }

    // ---------- Actualizaciones ----------

    private String versionActual() {
        try {
            PackageInfo p = getPackageManager().getPackageInfo(getPackageName(), 0);
            return p.versionName;
        } catch (Exception e) {
            return "0";
        }
    }

    private static String limpiar(String v) {
        return v == null ? "" : v.trim().replaceFirst("^[vV]", "");
    }

    /** Compara "1.2.10" con "1.2.9" número por número. */
    static boolean esMasNueva(String nueva, String actual) {
        String[] a = limpiar(nueva).split("[^0-9]+");
        String[] b = limpiar(actual).split("[^0-9]+");
        int n = Math.max(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int x = i < a.length && !a[i].isEmpty() ? Integer.parseInt(a[i]) : 0;
            int y = i < b.length && !b[i].isEmpty() ? Integer.parseInt(b[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private HttpURLConnection abrir(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "TallerDeMods");
        c.setRequestProperty("Accept", "application/vnd.github+json, application/octet-stream");
        return c;
    }

    private String descargarTexto(String url) throws Exception {
        HttpURLConnection c = abrir(url);
        try {
            if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
            InputStream in = c.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            return out.toString("UTF-8");
        } finally {
            c.disconnect();
        }
    }

    private void descargarArchivo(String url, File destino) throws Exception {
        // GitHub redirige a otro dominio; seguimos hasta 5 redirecciones a mano.
        for (int i = 0; i < 5; i++) {
            HttpURLConnection c = abrir(url);
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) {
                url = c.getHeaderField("Location");
                c.disconnect();
                continue;
            }
            if (code != 200) throw new Exception("HTTP " + code);
            long total = c.getContentLength();
            InputStream in = c.getInputStream();
            FileOutputStream out = new FileOutputStream(destino);
            byte[] buf = new byte[16384];
            long leido = 0;
            int ultimo = -1;
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                leido += n;
                if (total > 0) {
                    int pct = (int) (leido * 100 / total);
                    if (pct != ultimo && pct % 10 == 0) {
                        ultimo = pct;
                        llamarJs("onProgreso", String.valueOf(pct));
                    }
                }
            }
            out.close();
            in.close();
            c.disconnect();
            return;
        }
        throw new Exception("demasiadas redirecciones");
    }

    private boolean puedeInstalar() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || getPackageManager().canRequestPackageInstalls();
    }

    private void instalar(File apk) {
        if (!puedeInstalar()) {
            // Android pide permiso una sola vez; al volver (onResume) se instala solo.
            apkPendiente = apk;
            llamarJs("onPermiso", "1");
            Intent p = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName()));
            startActivity(p);
            return;
        }
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apk);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, "application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void llamarJs(final String funcion, final String arg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String a = JSONObject.quote(arg);
                web.evaluateJavascript(
                        "window." + funcion + "&&window." + funcion + "(" + a + ")", null);
            }
        });
    }

    // ---------- Minecraft ----------

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
