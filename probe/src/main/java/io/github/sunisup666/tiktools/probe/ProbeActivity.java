package io.github.sunisup666.tiktools.probe;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.widget.*;
import org.json.JSONObject;
import java.util.Locale;
import java.util.TimeZone;

public final class ProbeActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(32,80,32,80);ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
        JSONObject report=new JSONObject();
        try{
            TelephonyManager tm=(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            report.put("sim",tm.getSimCountryIso()).put("net",tm.getNetworkCountryIso()).put("operator",tm.getNetworkOperator()).put("operator_name",tm.getNetworkOperatorName());
            Location loc=new Location("test");loc.setLatitude(1);loc.setLongitude(2);loc.setAccuracy(7);loc.setAltitude(3);
            report.put("latitude",loc.getLatitude()).put("longitude",loc.getLongitude()).put("accuracy",loc.getAccuracy()).put("altitude",loc.getAltitude());
            GsmCellLocation cell=new GsmCellLocation();report.put("lac",cell.getLac()).put("cid",cell.getCid());
            WifiInfo wifi=((WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE)).getConnectionInfo();report.put("ssid",wifi.getSSID()).put("bssid",wifi.getBSSID());
            report.put("locale",Locale.getDefault().toLanguageTag()).put("timezone",TimeZone.getDefault().getID());
            report.put("model",Build.MODEL).put("brand",Build.BRAND).put("sdk",Build.VERSION.SDK_INT).put("release",Build.VERSION.RELEASE);
        }catch(Throwable e){try{report.put("error",e.toString());}catch(Exception ignored){}}
        try{java.nio.file.Files.write(new java.io.File(getFilesDir(),"probe.json").toPath(),report.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception ignored){}
        TextView output=new TextView(this);output.setText(report.toString());output.setTextSize(15);root.addView(output);
        TextView example=new TextView(this);example.setId(R.id.sample_caption);example.setText("Sample video description for translation.");example.setContentDescription("Video description");example.setTextSize(18);root.addView(example);
        Button like=new Button(this);like.setText("Like video");like.setContentDescription("Like video");root.addView(like);
        TextView author=new TextView(this);author.setId(R.id.sample_author);author.setText("Sample author");root.addView(author);
        int[] sampleIds={R.id.sample_music,R.id.sample_comment,R.id.sample_location,R.id.sample_topbar,R.id.sample_channel,R.id.sample_tabbar,R.id.sample_sidebtn,R.id.sample_logo,R.id.sample_translate,R.id.sample_paywall};
        for(int id:sampleIds){TextView v=new TextView(this);v.setId(id);v.setText(id==R.id.sample_comment?"forbidden comment":"UI fixture "+getResources().getResourceEntryName(id));root.addView(v);}
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{
            try{Location next=new Location("test2");next.setLatitude(1);next.setLongitude(2);report.put("latitude_after",next.getLatitude()).put("longitude_after",next.getLongitude());JSONObject visibility=new JSONObject();visibility.put("sample_author",author.getVisibility()).put("sample_caption",example.getVisibility());for(int id:sampleIds)visibility.put(getResources().getResourceEntryName(id),findViewById(id).getVisibility());report.put("ui_visibility",visibility);java.nio.file.Files.write(new java.io.File(getFilesDir(),"probe.json").toPath(),report.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));output.setText(report.toString());}catch(Throwable ignored){}
        },4500);
        if(getIntent().getBooleanExtra("translation_probe",false)) {
            new Thread(()->{JSONObject result=new JSONObject();try{Bundle args=new Bundle();args.putString("text","Hello, welcome to the toolbox.");args.putString("source","en");args.putString("target","zh");args.putBoolean("download",true);Bundle b=getContentResolver().call(android.net.Uri.parse("content://io.github.sunisup666.tiktools.settings"),"translate",null,args);result.put("ok",b!=null&&b.getBoolean("ok"));if(b!=null)result.put("text",b.getString("text","")).put("error",b.getString("error",""));}catch(Throwable e){try{result.put("error",e.toString());}catch(Exception ignored){}}try{java.nio.file.Files.write(new java.io.File(getFilesDir(),"translation.json").toPath(),result.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception ignored){}},"TikToolsTranslationProbe").start();
        }
    }
}
