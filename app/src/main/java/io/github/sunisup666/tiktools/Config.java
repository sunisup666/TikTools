package io.github.sunisup666.tiktools;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;
import java.util.TimeZone;

public final class Config {
    public final JSONObject json;
    public Config(JSONObject json) { this.json = json; }
    public boolean on(String key) { return json.optBoolean(key, false); }
    public String text(String key, String fallback) { return json.optString(key, fallback); }
    public double number(String key, double fallback) {
        try { double n = Double.parseDouble(text(key, "")); return Double.isFinite(n) ? n : fallback; }
        catch (Exception e) { return fallback; }
    }
    public int integer(String key, int fallback) { return (int)number(key, fallback); }
    public Locale locale() { return Locale.forLanguageTag(text("loc", "en-PH").replace('_', '-')); }
    public static JSONObject defaults() {
        JSONObject j = new JSONObject();
        try {
            j.put("schema", 1).put("enabled", false).put("region_enabled", true)
             .put("language_enabled", false).put("timezone_enabled", false)
             .put("sim", "ph").put("net", "ph").put("mcc", "51502").put("op", "Globe")
             .put("loc", "en-PH").put("tz", "Asia/Manila").put("country_name", "菲律宾")
             .put("lat", "14.5995").put("lon", "120.9842").put("altitude", "12")
             .put("accuracy", "15").put("route_mode", "fixed").put("route_radius", "20")
             .put("route_speed", "1.4").put("route_points", "")
             .put("lac", "100").put("cid", "10001").put("pci", "10")
             .put("wifi_ssid", "Test WiFi").put("wifi_bssid", "02:11:22:33:44:55")
             .put("spoof_brand", android.os.Build.BRAND).put("spoof_manufacturer", android.os.Build.MANUFACTURER)
             .put("spoof_model", android.os.Build.MODEL).put("spoof_device", android.os.Build.DEVICE)
             .put("spoof_product", android.os.Build.PRODUCT).put("spoof_board", android.os.Build.BOARD).put("spoof_hardware", android.os.Build.HARDWARE)
             .put("spoof_fingerprint", android.os.Build.FINGERPRINT).put("spoof_release", android.os.Build.VERSION.RELEASE).put("spoof_sdk", Integer.toString(android.os.Build.VERSION.SDK_INT))
             .put("spoof_patch", android.os.Build.VERSION.SECURITY_PATCH).put("translang", "zh")
             .put("translation_source", "auto").put("filter_keywords", "")
             .put("show_tool_button", true).put("custom_selectors", "{}")
             .put("location_source","region").put("location_name","菲律宾 · 马尼拉")
             .put("map_tile_url","https://tile.openstreetmap.org/{z}/{x}/{y}.png");
        } catch (Exception impossible) { throw new IllegalStateException(impossible); }
        return j;
    }
    public static Context storage(Context context) { return context.createDeviceProtectedStorageContext(); }
    public static Config load(Context context) {
        String raw = storage(context).getSharedPreferences("tiktools", 0).getString("config", null);
        try { return new Config(raw == null ? defaults() : new JSONObject(raw)); }
        catch (Exception e) { return new Config(defaults()); }
    }
    public static void save(Context context, JSONObject j) throws Exception {
        validate(j);
        if (!storage(context).getSharedPreferences("tiktools", 0).edit().putString("config", j.toString()).commit())
            throw new IllegalStateException("配置保存失败");
        context.getPackageManager().setComponentEnabledSetting(new android.content.ComponentName(context,"io.github.sunisup666.tiktools.LauncherAlias"),
            j.optBoolean("hide_icon",false)?android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED:android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,android.content.pm.PackageManager.DONT_KILL_APP);
    }
    public static void validate(JSONObject j) throws Exception {
        if (j.optInt("schema", 1) != 1) throw new IllegalArgumentException("不支持的配置版本");
        Config c = new Config(j);
        if(!java.util.Set.of("region","city","custom").contains(c.text("location_source","region")))throw new IllegalArgumentException("定位来源无效");
        String tiles=c.text("map_tile_url","https://tile.openstreetmap.org/{z}/{x}/{y}.png");
        java.net.URI tileUri=new java.net.URI(tiles.replace("{z}","1").replace("{x}","1").replace("{y}","1"));
        if(!tiles.contains("{z}")||!tiles.contains("{x}")||!tiles.contains("{y}")||!"https".equals(tileUri.getScheme())||tileUri.getHost()==null||tileUri.getUserInfo()!=null)throw new IllegalArgumentException("地图地址需为含 {z}/{x}/{y} 的 HTTPS 模板");
        for (String key : new String[]{"sim", "net"})
            if (!c.text(key, "ph").matches("[a-zA-Z]{2}")) throw new IllegalArgumentException("国家码必须为两个字母");
        if (!c.text("mcc", "51502").matches("[0-9]{5,6}")) throw new IllegalArgumentException("运营商编号需为 5–6 位数字");
        checkNumber(c, "lat", -90, 90); checkNumber(c, "lon", -180, 180);
        checkNumber(c, "route_radius", 0, 100000); checkNumber(c, "route_speed", 0, 200);
        checkNumber(c, "accuracy", 1, 10000); checkNumber(c, "spoof_sdk", 27, Math.max(37,android.os.Build.VERSION.SDK_INT));
        if (!c.text("wifi_bssid", "02:11:22:33:44:55").matches("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))
            throw new IllegalArgumentException("BSSID 格式应为 02:11:22:33:44:55");
        boolean zoneFound = false;
        for (String zone : TimeZone.getAvailableIDs()) if (zone.equals(c.text("tz", "Asia/Manila"))) zoneFound = true;
        if (!zoneFound) throw new IllegalArgumentException("时区名称无效");
        String points = c.text("route_points", "").trim();
        if (!points.isEmpty()) for (String point : points.split("[;\\n]+")) {
            String[] pair = point.trim().split(",");
            if (pair.length != 2) throw new IllegalArgumentException("路径点格式：纬度,经度；每行一个点");
            double lat = Double.parseDouble(pair[0].trim()), lon = Double.parseDouble(pair[1].trim());
            if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat)>90 || Math.abs(lon)>180)
                throw new IllegalArgumentException("路径坐标超出范围");
        }
        JSONObject selectors = new JSONObject(c.text("custom_selectors", "{}"));
        if (selectors.length() > 30) throw new IllegalArgumentException("自定义规则过多");
        for (java.util.Iterator<String> iterator=selectors.keys();iterator.hasNext();) {
            String key=iterator.next();
            JSONArray ids = selectors.getJSONArray(key);
            if (ids.length() > 40) throw new IllegalArgumentException("单组规则过多");
            for (int i=0;i<ids.length();i++) if (!ids.getString(i).matches("[A-Za-z0-9_.]+"))
                throw new IllegalArgumentException("自定义规则只能使用资源名称");
        }
    }
    private static void checkNumber(Config c, String key, double min, double max) {
        double n = c.number(key, Double.NaN);
        if (!Double.isFinite(n) || n < min || n > max) throw new IllegalArgumentException(key+" 超出范围");
    }
}
