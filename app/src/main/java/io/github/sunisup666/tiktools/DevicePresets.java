package io.github.sunisup666.tiktools;

import android.os.Build;
import org.json.JSONObject;

public final class DevicePresets {
    public record Device(String label,String brand,String manufacturer,String model,String device,String product,String board,String hardware,String fingerprint){
        @Override public String toString(){return label;}
        public void apply(JSONObject j)throws Exception{
            String[] keys={"brand","manufacturer","model","device","product","board","hardware","fingerprint"};
            String[] values={brand,manufacturer,model,device,product,board,hardware,fingerprint};
            for(int i=0;i<keys.length;i++)j.put("spoof_"+keys[i],values[i]);
            j.put("device_preset",label).put("version_spoof",false).put("spoof_release",Build.VERSION.RELEASE).put("spoof_sdk",Integer.toString(Build.VERSION.SDK_INT)).put("spoof_patch",Build.VERSION.SECURITY_PATCH);
        }
    }
    public static Device actual(){return new Device("本机真实设备（推荐）",Build.BRAND,Build.MANUFACTURER,Build.MODEL,Build.DEVICE,Build.PRODUCT,Build.BOARD,Build.HARDWARE,Build.FINGERPRINT);}
    public static Device[] all(){
        if(!Build.DEVICE.startsWith("alioth")&&!Build.MODEL.startsWith("M2012K11A"))return new Device[]{actual()};
        return new Device[]{actual(),new Device("Redmi K40 · 同平台模板","Redmi","Xiaomi","M2012K11AC","alioth","alioth","kona","qcom",""),
            new Device("POCO F3 · 同平台模板","POCO","Xiaomi","M2012K11AG","alioth","alioth_global","kona","qcom","")};
    }
}
