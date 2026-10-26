package io.github.sunisup666.tiktools;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SettingsProvider extends ContentProvider {
    public static final String AUTHORITY="io.github.sunisup666.tiktools.settings";
    public static final Uri URI=Uri.parse("content://"+AUTHORITY);
    public static final Set<String> TARGETS=Set.of("com.ss.android.ugc.trill","com.zhiliaoapp.musically","io.github.sunisup666.tiktools.probe");
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private static final AtomicBoolean FETCHING=new AtomicBoolean();
    @Override public boolean onCreate() { return true; }
    private String caller() {
        if (Binder.getCallingUid()==Process.myUid()) return "io.github.sunisup666.tiktools";
        String[] packages=getContext().getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if (packages!=null) for (String p:packages) if (TARGETS.contains(p)) {
            if(p.equals("io.github.sunisup666.tiktools.probe")&&getContext().getPackageManager().checkSignatures("io.github.sunisup666.tiktools",p)!=android.content.pm.PackageManager.SIGNATURE_MATCH)throw new SecurityException("Untrusted diagnostic app");
            return p;
        }
        throw new SecurityException("This provider only serves configured target applications");
    }
    private Config effectiveConfig(Context context,String pkg) throws Exception {
        Config config=Config.load(context);
        if(pkg.equals("io.github.sunisup666.tiktools.probe")) {
            java.io.File fixture=new java.io.File(context.getFilesDir(),"probe-config.json");
            if(fixture.isFile()&&fixture.length()<=65536){JSONObject test=new JSONObject(new String(java.nio.file.Files.readAllBytes(fixture.toPath()),StandardCharsets.UTF_8));Config.validate(test);config=new Config(test);}
        }
        return config;
    }
    @Override public Bundle call(String method,String arg,Bundle extras) {
        String pkg=caller(); Context context=getContext(); Bundle result=new Bundle();
        long identity=Binder.clearCallingIdentity();
        try {
            switch (method) {
                case "config" -> {
                    Config config=effectiveConfig(context,pkg);
                    result.putString("json",config.json.toString());
                    if (!pkg.equals("io.github.sunisup666.tiktools.probe") && config.on("ip_link")) scheduleIp(context);
                }
                case "report" -> {
                    String message=extras==null?"":extras.getString("json","");
                    if (message.length()>12000) throw new IllegalArgumentException("Diagnostic too large");
                    new JSONObject(message);
                    Config.storage(context).getSharedPreferences(pkg.equals("io.github.sunisup666.tiktools.probe")?"status-probe":"status",0).edit().putString("package",pkg)
                        .putLong("time",System.currentTimeMillis()).putString("report",message).apply();
                    result.putBoolean("ok",true);
                }
                case "translate" -> {
                    if (extras==null) throw new IllegalArgumentException("No text");
                    Config c=effectiveConfig(context,pkg);
                    if (!pkg.equals("io.github.sunisup666.tiktools") && !c.on("trans")) throw new SecurityException("Translation disabled");
                    String translated=TranslationBroker.translate(extras.getString("text",""),extras.getString("source",c.text("translation_source","auto")),extras.getString("target",c.text("translang","zh")),extras.getBoolean("download",false));
                    result.putString("text",translated); result.putBoolean("ok",true);
                }
                case "download" -> {
                    if(!effectiveConfig(context,pkg).on("download_restrictions"))throw new SecurityException("Downloads disabled");
                    String url=extras==null?"":extras.getString("url","");Uri uri=Uri.parse(url);
                    if(!"https".equals(uri.getScheme())||uri.getHost()==null||url.length()>8192)throw new IllegalArgumentException("Invalid media URL");
                    String name="video_"+System.currentTimeMillis()+".mp4";
                    android.app.DownloadManager.Request request=new android.app.DownloadManager.Request(uri).setTitle(name)
                        .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_MOVIES,"TikTools/"+name);
                    long id=((android.app.DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
                    Config.storage(context).getSharedPreferences("status",0).edit().putLong("last_download_id",id).putLong("last_download_time",System.currentTimeMillis()).apply();
                    result.putLong("id",id);result.putBoolean("ok",true);
                }
                default -> throw new IllegalArgumentException("Unknown operation");
            }
        } catch (Exception e) { result.putBoolean("ok",false);result.putString("error",e.getClass().getSimpleName()+": "+UiText.error(getContext(),e)); }
        finally { Binder.restoreCallingIdentity(identity); }
        return result;
    }
    public static void scheduleIp(Context context) {
        long last=Config.storage(context).getSharedPreferences("status",0).getLong("ip_time",0);
        if (System.currentTimeMillis()-last<900000 || !FETCHING.compareAndSet(false,true)) return;
        IO.execute(()->{ try { refreshIp(context); } catch(Exception ignored) {} finally { FETCHING.set(false); } });
    }
    public static String refreshIp(Context context) throws Exception {
        HttpURLConnection conn=(HttpURLConnection)new URL("https://ipwho.is/").openConnection();
        conn.setConnectTimeout(7000);conn.setReadTimeout(7000);conn.setRequestProperty("User-Agent","TikTools/"+BuildConfig.VERSION_NAME);
        try (InputStream stream=conn.getInputStream()) {
            byte[] data=stream.readNBytes(128*1024);JSONObject response=new JSONObject(new String(data,StandardCharsets.UTF_8));
            if (!response.optBoolean("success",false)) throw new UiText.Error(R.string.error_ip_query,"IP country lookup failed");
            String iso=response.getString("country_code");Presets.Region region=Presets.find(iso);
            if (region==null) throw new UiText.Error(R.string.error_ip_no_preset,"No preset for IP country: "+iso,iso);
            JSONObject j=Config.load(context).json;boolean follow=j.optString("location_source","region").equals("region");String pointCountry=j.optString("location_country",""),pointZone=j.optString("location_timezone","");region.apply(j,follow);if(!follow&&pointCountry.equalsIgnoreCase(iso)&&!pointZone.isEmpty())j.put("tz",pointZone);Config.save(context,j);
            Config.storage(context).getSharedPreferences("status",0).edit().putLong("ip_time",System.currentTimeMillis()).putString("ip_country",iso).apply();
            return region.toString();
        } finally { conn.disconnect(); }
    }
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String o) { caller();MatrixCursor c=new MatrixCursor(new String[]{"config"});c.addRow(new Object[]{Config.load(getContext()).json.toString()});return c; }
    @Override public String getType(Uri uri) { return "application/json"; }
    @Override public Uri insert(Uri uri,ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri,ContentValues values,String s,String[] a) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri,String s,String[] a) { throw new UnsupportedOperationException(); }
}
