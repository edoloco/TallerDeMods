package com.taller.mods;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Índice de los paquetes de recursos "vanilla" de Minecraft dentro de su APK.
 * Lee archivos sueltos (resource_packs/vanilla/entity/x.json) y también los
 * paquetes .brarchive (resource_packs/vanilla/__brarchive/entity.brarchive)
 * que usan las versiones nuevas de Minecraft.
 */
public class McIndice {

    /** rel -> {apk, entrada del zip} o {apk, entrada, inicio, largo} si está dentro de un .brarchive */
    public final Map<String, String[]> idx = new HashMap<>();
    public final StringBuilder resumen = new StringBuilder();
    public String detalle = "";
    private final Map<String, ZipFile> zips = new HashMap<>();
    private final Map<String, byte[]> cacheArchivos = new HashMap<>();

    private static final String CLAVE = "resource_packs/";
    private static final long MAGIA = 0x267052A0B125277DL;

    static long rangoPaquete(String pack) {
        if (pack.equals("vanilla")) return 0;
        String[] p = pack.replaceAll("[^0-9.]", "").split("\\.");
        long r = 0;
        for (int i = 0; i < 4; i++) {
            r *= 1000;
            if (i < p.length && !p[i].isEmpty()) {
                try { r += Math.min(999, Long.parseLong(p[i])); } catch (Exception ignorar) { }
            }
        }
        return r + 1;
    }

    public void indexar(List<String> apks) {
        Map<String, Long> rango = new HashMap<>();
        Map<String, Integer> conteo = new HashMap<>();
        StringBuilder vistos = new StringBuilder();
        int archivos = 0;
        for (String apk : apks) {
            try {
                ZipFile z = new ZipFile(apk);
                zips.put(apk, z);
                Enumeration<? extends ZipEntry> en = z.entries();
                while (en.hasMoreElements()) {
                    ZipEntry e = en.nextElement();
                    String n = e.getName();
                    if (e.isDirectory()) continue;
                    int k = n.indexOf(CLAVE);
                    if (k < 0) {
                        if (n.startsWith("assets/")) {
                            String[] partes = n.split("/");
                            if (partes.length > 2) {
                                String d = partes[0] + "/" + partes[1] + "/" + (partes.length > 3 ? partes[2] : "");
                                if (vistos.length() < 300 && vistos.indexOf(d + " ") < 0) vistos.append(d).append(' ');
                            }
                        }
                        continue;
                    }
                    String resto = n.substring(k + CLAVE.length());
                    int s = resto.indexOf('/');
                    if (s < 0) continue;
                    String pack = resto.substring(0, s);
                    Integer c = conteo.get(pack);
                    conteo.put(pack, c == null ? 1 : c + 1);
                    if (!pack.startsWith("vanilla") || pack.contains("music")) continue;
                    String rel = resto.substring(s + 1);
                    long r = rangoPaquete(pack);
                    if (rel.startsWith("__brarchive/") && rel.endsWith(".brarchive")) {
                        // __brarchive/models/entity.brarchive  ->  carpeta "models/entity/"
                        String carpeta = rel.substring("__brarchive/".length(), rel.length() - ".brarchive".length()) + "/";
                        archivos += leerIndiceArchivo(z, apk, n, carpeta, r, rango);
                        continue;
                    }
                    poner(rel, new String[]{apk, n}, r, rango);
                }
            } catch (Exception ignorar) { }
        }
        List<String> claves = new ArrayList<>(conteo.keySet());
        Collections.sort(claves);
        for (String c : claves) if (resumen.length() < 500) resumen.append(c).append('=').append(conteo.get(c)).append(' ');
        resumen.append("brarchives=").append(archivos);
        detalle = vistos.toString().trim();
    }

    private void poner(String rel, String[] ref, long r, Map<String, Long> rango) {
        Long viejo = rango.get(rel);
        if (viejo == null || viejo <= r) {
            idx.put(rel, ref);
            rango.put(rel, r);
        }
    }

    /** Lee solo la lista de archivos de un .brarchive (encabezado + descriptores de 256 bytes). */
    private int leerIndiceArchivo(ZipFile z, String apk, String nombre, String carpeta, long r, Map<String, Long> rango) {
        try (InputStream in = z.getInputStream(z.getEntry(nombre))) {
            DataInputStream d = new DataInputStream(in);
            byte[] h = new byte[16];
            d.readFully(h);
            long magia = 0;
            for (int i = 7; i >= 0; i--) magia = (magia << 8) | (h[i] & 0xff);
            if (magia != MAGIA) return 0;
            long cantidad = u32(h, 8);
            if (cantidad <= 0 || cantidad > 200000) return 0;
            long base = 16 + cantidad * 256;
            byte[] desc = new byte[256];
            for (long i = 0; i < cantidad; i++) {
                d.readFully(desc);
                int largoNombre = desc[0] & 0xff;
                if (largoNombre > 247) largoNombre = 247;
                String entrada = new String(desc, 1, largoNombre, "UTF-8").replace('\\', '/');
                long off = u32(desc, 248), len = u32(desc, 252);
                if (len == 0) continue;
                String rel = entrada.startsWith(carpeta) ? entrada : carpeta + entrada;
                poner(rel, new String[]{apk, nombre, String.valueOf(base + off), String.valueOf(len)}, r, rango);
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static long u32(byte[] b, int o) {
        return (b[o] & 0xffL) | ((b[o + 1] & 0xffL) << 8) | ((b[o + 2] & 0xffL) << 16) | ((b[o + 3] & 0xffL) << 24);
    }

    private byte[] leerEntrada(String apk, String nombre) throws Exception {
        ZipFile z = zips.get(apk);
        try (InputStream in = z.getInputStream(z.getEntry(nombre))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        }
    }

    public synchronized byte[] leer(String rel) {
        String[] r = idx.get(rel);
        if (r == null) return null;
        try {
            if (r.length == 2) return leerEntrada(r[0], r[1]);
            String clave = r[0] + "|" + r[1];
            byte[] todo = cacheArchivos.get(clave);
            if (todo == null) {
                todo = leerEntrada(r[0], r[1]);
                cacheArchivos.put(clave, todo);
            }
            int ini = (int) Long.parseLong(r[2]), len = (int) Long.parseLong(r[3]);
            if (ini < 0 || ini + len > todo.length) return null;
            byte[] out = new byte[len];
            System.arraycopy(todo, ini, out, 0, len);
            return out;
        } catch (Exception e) {
            return null;
        }
    }
}
