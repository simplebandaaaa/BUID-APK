package com.sayhi.extensions;

import android.content.Context;
import android.net.Uri;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Lightweight WebExtension content-script loader for Android WebView.
 *  This is NOT a full Chrome extension runtime.
 */
public final class ExtensionManager {
    private final File root;
    private final Context ctx;
    private final ArrayList<Ext> exts = new ArrayList<>();

    public ExtensionManager(Context context) {
        ctx = context.getApplicationContext();
        root = new File(ctx.getFilesDir(), "extensions");
        if (!root.exists()) root.mkdirs();
        load();
    }

    private static final class Ext {
        String id;
        String name;
        File dir;
        final ArrayList<String> js = new ArrayList<>();
        final ArrayList<String> css = new ArrayList<>();
        final ArrayList<String> matches = new ArrayList<>();
    }

    public ArrayList<String> list() {
        ArrayList<String> result = new ArrayList<>();
        for (Ext e : exts) result.add(e.name == null ? e.id : e.name);
        return result;
    }

    private void load() {
        File[] files = root.listFiles();
        if (files == null) return;
        for (File dir : files) {
            if (!dir.isDirectory()) continue;
            if (!new File(dir, "manifest.json").isFile()) continue;
            try {
                parse(dir);
            } catch (Exception ignored) {
                // Ignore one broken extension and keep the browser usable.
            }
        }
    }

    public void install(Uri uri) throws Exception {
        if (uri == null) throw new IllegalArgumentException("No ZIP selected");

        String id = "ext_" + System.currentTimeMillis();
        File dir = new File(root, id);
        if (!dir.mkdirs()) throw new Exception("Cannot create extension directory");

        File zip = new File(root, "_install.zip");
        copyUriToFile(uri, zip);

        try {
            unzipSafely(zip, dir);
            parse(dir); // Validate manifest before reporting success.
        } catch (Exception e) {
            deleteRecursive(dir);
            throw e;
        } finally {
            if (zip.exists()) zip.delete();
        }
    }

    private void copyUriToFile(Uri uri, File target) throws Exception {
        InputStream in = ctx.getContentResolver().openInputStream(uri);
        if (in == null) throw new Exception("Cannot open selected ZIP");
        try (InputStream input = in; FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) out.write(buffer, 0, count);
        }
    }

    private void unzipSafely(File zip, File destination) throws Exception {
        String base = destination.getCanonicalPath() + File.separator;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                File out = new File(destination, entry.getName());
                String canonical = out.getCanonicalPath();
                if (!canonical.startsWith(base)) {
                    zis.closeEntry();
                    throw new SecurityException("Unsafe ZIP entry");
                }
                if (entry.isDirectory()) {
                    if (!out.exists() && !out.mkdirs()) throw new Exception("Cannot create directory");
                } else {
                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new Exception("Cannot create directory");
                    }
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        int n;
                        while ((n = zis.read(buffer)) != -1) fos.write(buffer, 0, n);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void parse(File dir) throws Exception {
        File manifest = new File(dir, "manifest.json");
        if (!manifest.isFile()) throw new Exception("manifest.json not found");

        JSONObject json = new JSONObject(read(manifest));
        Ext ext = new Ext();
        ext.id = dir.getName();
        ext.dir = dir;
        ext.name = json.optString("name", ext.id);

        JSONArray contentScripts = json.optJSONArray("content_scripts");
        if (contentScripts != null) {
            for (int i = 0; i < contentScripts.length(); i++) {
                JSONObject item = contentScripts.optJSONObject(i);
                if (item == null) continue;

                JSONArray matches = item.optJSONArray("matches");
                if (matches != null) {
                    for (int j = 0; j < matches.length(); j++) {
                        String value = matches.optString(j, null);
                        if (value != null) ext.matches.add(value);
                    }
                }

                JSONArray scripts = item.optJSONArray("js");
                if (scripts != null) {
                    for (int j = 0; j < scripts.length(); j++) {
                        String value = scripts.optString(j, null);
                        if (value != null) ext.js.add(value);
                    }
                }

                JSONArray styles = item.optJSONArray("css");
                if (styles != null) {
                    for (int j = 0; j < styles.length(); j++) {
                        String value = styles.optString(j, null);
                        if (value != null) ext.css.add(value);
                    }
                }
            }
        }

        exts.removeIf(existing -> existing.id.equals(ext.id));
        exts.add(ext);
    }

    public void inject(WebView webView, String url) {
        if (webView == null || url == null) return;

        for (Ext ext : exts) {
            if (!matches(ext.matches, url)) continue;

            for (String file : ext.css) {
                try {
                    String css = read(new File(ext.dir, file));
                    String script = "(function(){var s=document.createElement('style');" +
                            "s.textContent=" + toJsString(css) + ";" +
                            "(document.head||document.documentElement).appendChild(s);})();";
                    webView.evaluateJavascript(script, null);
                } catch (Exception ignored) { }
            }

            for (String file : ext.js) {
                try {
                    String source = read(new File(ext.dir, file));
                    webView.evaluateJavascript("(function(){\n" + source + "\n})();", null);
                } catch (Exception ignored) { }
            }
        }
    }

    private boolean matches(ArrayList<String> patterns, String url) {
        for (String pattern : patterns) {
            if ("<all_urls>".equals(pattern)) return true;
            if (globMatches(pattern, url)) return true;
        }
        return false;
    }

    private boolean globMatches(String pattern, String value) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') regex.append(".*");
            else if (c == '?') regex.append('.');
            else {
                if ("\\.^$|()[]{}+".indexOf(c) >= 0) regex.append('\\');
                regex.append(c);
            }
        }
        regex.append('$');
        return value.matches(regex.toString());
    }

    private static String toJsString(String value) {
        return JSONObject.quote(value == null ? "" : value);
    }

    private static String read(File file) throws Exception {
        if (!file.isFile()) throw new Exception("Missing extension file: " + file.getName());
        try (InputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteRecursive(child);
        file.delete();
    }
}
