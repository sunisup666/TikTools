package io.github.sunisup666.tiktools;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.net.Uri;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.json.JSONObject;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HookEntry implements IXposedHookLoadPackage {
    private static final Set<String> PACKAGES=Set.of("com.ss.android.ugc.trill","com.zhiliaoapp.musically","io.github.sunisup666.tiktools.probe");
    private static final AtomicBoolean STARTED=new AtomicBoolean();
    static volatile String mediaUrl="";
    private Config config;
    private GeoEngine geo;
    private final Map<String,Integer> installed=new LinkedHashMap<>();
    private final Map<String,String> errors=new LinkedHashMap<>();
    private Context context;
    private ClassLoader loader;
    private String packageName;
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        if(!PACKAGES.contains(lp.packageName))return;
        XposedHelpers.findAndHookMethod(Application.class,"attach",Context.class,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam p){
                if(!STARTED.compareAndSet(false,true))return;
                context=(Context)p.args[0];loader=context.getClassLoader();packageName=lp.packageName;
                try{config=readConfig(context);geo=new GeoEngine(config);if(config.on("enabled")){installRegion();installLocation();installCellWifi();installBuild();installDownload();UiAdapter.install(context,config,HookEntry.this::report);}report();}
                catch(Throwable e){XposedBridge.log("TikTools initialization: "+e.getClass().getSimpleName());}
            }
        });
    }
    private Config readConfig(Context c) {
        try { Bundle b=c.getContentResolver().call(Uri.parse("content://io.github.sunisup666.tiktools.settings"),"config",null,null);if(b!=null&&b.containsKey("json"))return new Config(new JSONObject(b.getString("json"))); }
        catch(Throwable e){ errors.put("config_provider",e.getClass().getSimpleName()); }
        try { XSharedPreferences prefs=new XSharedPreferences(new File("/data/user_de/0/io.github.sunisup666.tiktools/shared_prefs/tiktools.xml"));prefs.reload();String j=prefs.getString("config",null);if(j!=null)return new Config(new JSONObject(j)); }
        catch(Throwable e){errors.put("config_fallback",e.getClass().getSimpleName());}
        return new Config(Config.defaults());
    }
    private interface Value { Object get(XC_MethodHook.MethodHookParam p) throws Throwable; }
    private void hook(String group,String className,String methodName,Class<?> returnType,Value value) {
        try { hook(group,Class.forName(className,false,loader),methodName,returnType,value); }
        catch(Throwable e){ errors.put(group+":"+className+"."+methodName,e.getClass().getSimpleName()); }
    }
    private void hook(String group,Class<?> clazz,String name,Class<?> returnType,Value value) {
        for(Method m:clazz.getDeclaredMethods()) {
            if(!m.getName().equals(name)||m.getReturnType()!=returnType||Modifier.isAbstract(m.getModifiers()))continue;
            try {
                XposedBridge.hookMethod(m,new XC_MethodHook(){
                    @Override protected void beforeHookedMethod(MethodHookParam p){try{Object v=value.get(p);if(v!=null)p.setResult(v);}catch(Throwable e){/* Keep original behavior on an incompatible invocation. */}}
                });installed.merge(group,1,Integer::sum);
            }catch(Throwable e){errors.put(group+":"+name,e.getClass().getSimpleName());}
        }
    }
    private void installRegion() {
        if(config.on("region_enabled")) {
            String cls="android.telephony.TelephonyManager";
            hook("region",cls,"getSimCountryIso",String.class,p->config.text("sim","ph").toLowerCase(Locale.ROOT));
            hook("region",cls,"getNetworkCountryIso",String.class,p->config.text("net","ph").toLowerCase(Locale.ROOT));
            for(String n:new String[]{"getSimOperator","getNetworkOperator"})hook("region",cls,n,String.class,p->config.text("mcc","51502"));
            for(String n:new String[]{"getSimOperatorName","getNetworkOperatorName"})hook("region",cls,n,String.class,p->config.text("op","Globe"));
            hook("region","android.telephony.SubscriptionInfo","getCountryIso",String.class,p->config.text("sim","ph"));
            hook("region","android.telephony.SubscriptionInfo","getMccString",String.class,p->config.text("mcc","51502").substring(0,3));
            hook("region","android.telephony.SubscriptionInfo","getMncString",String.class,p->config.text("mcc","51502").substring(3));
        }
        if(config.on("language_enabled")) {
            Locale selected=config.locale();Locale.setDefault(selected);
            hook("language",Locale.class,"getDefault",Locale.class,p->selected);
            android.content.res.Configuration changed=new android.content.res.Configuration(context.getResources().getConfiguration());changed.setLocale(selected);changed.setLocales(new android.os.LocaleList(selected));context.getResources().updateConfiguration(changed,context.getResources().getDisplayMetrics());
        }
        if(config.on("timezone_enabled")) {
            TimeZone zone=TimeZone.getTimeZone(config.text("tz","Asia/Manila"));TimeZone.setDefault(zone);
            hook("timezone",TimeZone.class,"getDefault",TimeZone.class,p->zone.clone());
        }
    }
    private void installLocation() {
        if(!config.on("location_inject"))return;
        hook("location",Location.class,"getLatitude",double.class,p->Double.valueOf(geo.position()[0]));
        hook("location",Location.class,"getLongitude",double.class,p->Double.valueOf(geo.position()[1]));
        hook("location",Location.class,"getAltitude",double.class,p->Double.valueOf(config.number("altitude",12)));
        hook("location",Location.class,"getAccuracy",float.class,p->Float.valueOf((float)config.number("accuracy",15)));
        hook("location",Location.class,"getSpeed",float.class,p->Float.valueOf(config.text("route_mode","fixed").equals("fixed")?0f:(float)config.number("route_speed",1.4)));
        // Deliberately retain provider availability, permission checks, timestamps, and mock flags.
    }
    private void installCellWifi() {
        if(config.on("cell_spoof")) {
            hook("cell","android.telephony.gsm.GsmCellLocation","getLac",int.class,p->Integer.valueOf(config.integer("lac",100)));
            hook("cell","android.telephony.gsm.GsmCellLocation","getCid",int.class,p->Integer.valueOf(config.integer("cid",10001)));
            for(String cls:new String[]{"android.telephony.CellIdentityGsm","android.telephony.CellIdentityWcdma","android.telephony.CellIdentityLte","android.telephony.CellIdentityNr"}) {
                hook("cell",cls,"getLac",int.class,p->Integer.valueOf(config.integer("lac",100)));
                hook("cell",cls,"getTac",int.class,p->Integer.valueOf(config.integer("lac",100)));
                hook("cell",cls,"getCid",int.class,p->Integer.valueOf(config.integer("cid",10001)));
                hook("cell",cls,"getCi",int.class,p->Integer.valueOf(config.integer("cid",10001)));
                hook("cell",cls,"getNci",long.class,p->Long.valueOf((long)config.number("cid",10001)));
                hook("cell",cls,"getPci",int.class,p->Integer.valueOf(config.integer("pci",10)));
            }
        }
        if(config.on("wifi_spoof")) {
            hook("wifi","android.net.wifi.WifiInfo","getSSID",String.class,p->"\""+config.text("wifi_ssid","Test WiFi").replace("\"","")+"\"");
            hook("wifi","android.net.wifi.WifiInfo","getBSSID",String.class,p->config.text("wifi_bssid","02:11:22:33:44:55"));
        }
    }
    private void installBuild() {
        if(config.on("device_spoof")) for(String field:new String[]{"BRAND","MANUFACTURER","MODEL","DEVICE","PRODUCT","BOARD","HARDWARE","FINGERPRINT"}) {
            String value=config.text("spoof_"+field.toLowerCase(Locale.ROOT),"");if(value.isBlank())continue;
            try{XposedHelpers.setStaticObjectField(Build.class,field,value);installed.merge("device",1,Integer::sum);}catch(Throwable e){errors.put("device:"+field,e.getClass().getSimpleName());}
        }
        if(config.on("version_spoof"))try{XposedHelpers.setStaticObjectField(Build.VERSION.class,"RELEASE",config.text("spoof_release","17"));XposedHelpers.setStaticIntField(Build.VERSION.class,"SDK_INT",config.integer("spoof_sdk",37));XposedHelpers.setStaticObjectField(Build.VERSION.class,"SECURITY_PATCH",config.text("spoof_patch","2026-10-01"));installed.put("version",3);}catch(Throwable e){errors.put("version",e.getClass().getSimpleName());}
    }
    private void installDownload() {
        if(!config.on("download_restrictions"))return;
        try{installed.put("media_catalog",MediaCatalog.install(loader));}catch(Throwable e){errors.put("media_catalog",e.getClass().getSimpleName());}
        for(String cls:new String[]{"com.ss.android.ugc.aweme.feed.model.VideoControl","com.ss.android.ugc.aweme.feed.model.AwemeACLShare"}) {
            for(String name:new String[]{"isAllowDownload","getAllowDownload","isCanDownload","getCanDownload"})hook("download_options",cls,name,boolean.class,p->Boolean.TRUE);
        }
    }
    private void report() {
        try {
            JSONObject j=new JSONObject().put("module","TikTools "+BuildConfig.VERSION_NAME).put("package",packageName).put("enabled",config.on("enabled"))
                .put("installed_hooks",new JSONObject(installed)).put("compatibility_notes",new JSONObject(errors)).put("ui_matches",UiAdapter.matches()).put("ui_total_matches",UiAdapter.totalMatches()).put("view_ids",UiAdapter.ids()).put("view_samples",UiAdapter.samples()).put("media_address_observed",!mediaUrl.isEmpty()).put("media_catalog_size",MediaCatalog.size()).put("native_download_controls_observed",MediaCatalog.controls());
            Bundle b=new Bundle();b.putString("json",j.toString());context.getContentResolver().call(Uri.parse("content://io.github.sunisup666.tiktools.settings"),"report",null,b);
        }catch(Throwable e){XposedBridge.log("TikTools report: "+e.getClass().getSimpleName());}
    }
}
