package io.github.sunisup666.tiktools;

import org.json.JSONObject;
public final class Presets {
    public record Region(String name, String iso, String operator, String mcc, String locale, String zone, double lat, double lon) {
        @Override public String toString() { return name + " · " + iso.toUpperCase(); }
        public void apply(JSONObject j) throws Exception {
            apply(j,true);
        }
        public void apply(JSONObject j,boolean coordinates) throws Exception {
            j.put("country_name",name).put("sim",iso).put("net",iso).put("op",operator).put("mcc",mcc)
             .put("loc",locale).put("tz",zone);
            if(coordinates)j.put("lat",Double.toString(lat)).put("lon",Double.toString(lon)).put("location_source","region").put("location_name",defaultCity(iso)).put("location_country",iso).put("location_timezone",zone);
        }
    }
    public static final Region[] ALL = {
        new Region("菲律宾","ph","Globe","51502","en-PH","Asia/Manila",14.5995,120.9842),
        new Region("日本","jp","NTT DOCOMO","44010","ja-JP","Asia/Tokyo",35.6762,139.6503),
        new Region("美国","us","T-Mobile","310260","en-US","America/Los_Angeles",34.0522,-118.2437),
        new Region("英国","gb","Vodafone","23415","en-GB","Europe/London",51.5074,-0.1278),
        new Region("新加坡","sg","Singtel","52501","en-SG","Asia/Singapore",1.3521,103.8198),
        new Region("马来西亚","my","Maxis","50212","ms-MY","Asia/Kuala_Lumpur",3.139,101.6869),
        new Region("印度尼西亚","id","Telkomsel","51010","id-ID","Asia/Jakarta",-6.2088,106.8456),
        new Region("泰国","th","AIS","52001","th-TH","Asia/Bangkok",13.7563,100.5018),
        new Region("越南","vn","Viettel","45204","vi-VN","Asia/Ho_Chi_Minh",10.8231,106.6297),
        new Region("韩国","kr","SK Telecom","45005","ko-KR","Asia/Seoul",37.5665,126.978),
        new Region("德国","de","Vodafone","26202","de-DE","Europe/Berlin",52.52,13.405),
        new Region("法国","fr","Orange","20801","fr-FR","Europe/Paris",48.8566,2.3522),
        new Region("意大利","it","TIM","22201","it-IT","Europe/Rome",41.9028,12.4964),
        new Region("西班牙","es","Movistar","21407","es-ES","Europe/Madrid",40.4168,-3.7038),
        new Region("加拿大","ca","Rogers","302720","en-CA","America/Toronto",43.6532,-79.3832),
        new Region("澳大利亚","au","Telstra","50501","en-AU","Australia/Sydney",-33.8688,151.2093),
        new Region("巴西","br","Vivo","72406","pt-BR","America/Sao_Paulo",-23.5505,-46.6333),
        new Region("墨西哥","mx","Telcel","334020","es-MX","America/Mexico_City",19.4326,-99.1332),
        new Region("土耳其","tr","Turkcell","28601","tr-TR","Europe/Istanbul",41.0082,28.9784),
        new Region("阿联酋","ae","Etisalat","42402","ar-AE","Asia/Dubai",25.2048,55.2708),
        new Region("沙特阿拉伯","sa","STC","42001","ar-SA","Asia/Riyadh",24.7136,46.6753),
        new Region("南非","za","Vodacom","65501","en-ZA","Africa/Johannesburg",-26.2041,28.0473),
        new Region("中国大陆","cn","China Mobile","46000","zh-CN","Asia/Shanghai",39.9042,116.4074)
    };
    public static Region find(String iso) {
        for (Region r:ALL) if (r.iso().equalsIgnoreCase(iso)) return r;
        return null;
    }
    public record City(String name,String iso,double lat,double lon,String zone){
        @Override public String toString(){Region r=find(iso);return (r==null?iso:r.name())+" · "+name;}
        public void apply(JSONObject j)throws Exception{Region region=find(iso);if(region!=null)region.apply(j,false);j.put("lat",Double.toString(lat)).put("lon",Double.toString(lon)).put("tz",zone).put("location_source","city").put("location_name",toString()).put("location_country",iso).put("location_timezone",zone);}
    }
    public static String defaultCity(String iso){for(City c:CITIES)if(c.iso().equalsIgnoreCase(iso))return c.toString();return "地区默认坐标";}
    public static final City[] CITIES={
        new City("马尼拉","ph",14.5995,120.9842,"Asia/Manila"),new City("宿务","ph",10.3157,123.8854,"Asia/Manila"),
        new City("东京","jp",35.6762,139.6503,"Asia/Tokyo"),new City("大阪","jp",34.6937,135.5023,"Asia/Tokyo"),new City("京都","jp",35.0116,135.7681,"Asia/Tokyo"),
        new City("洛杉矶","us",34.0522,-118.2437,"America/Los_Angeles"),new City("纽约","us",40.7128,-74.0060,"America/New_York"),new City("旧金山","us",37.7749,-122.4194,"America/Los_Angeles"),new City("芝加哥","us",41.8781,-87.6298,"America/Chicago"),
        new City("伦敦","gb",51.5074,-0.1278,"Europe/London"),new City("曼彻斯特","gb",53.4808,-2.2426,"Europe/London"),
        new City("新加坡","sg",1.3521,103.8198,"Asia/Singapore"),new City("吉隆坡","my",3.1390,101.6869,"Asia/Kuala_Lumpur"),new City("槟城","my",5.4141,100.3288,"Asia/Kuala_Lumpur"),
        new City("雅加达","id",-6.2088,106.8456,"Asia/Jakarta"),new City("巴厘岛登巴萨","id",-8.6705,115.2126,"Asia/Makassar"),
        new City("曼谷","th",13.7563,100.5018,"Asia/Bangkok"),new City("清迈","th",18.7883,98.9853,"Asia/Bangkok"),
        new City("胡志明市","vn",10.8231,106.6297,"Asia/Ho_Chi_Minh"),new City("河内","vn",21.0278,105.8342,"Asia/Ho_Chi_Minh"),
        new City("首尔","kr",37.5665,126.9780,"Asia/Seoul"),new City("釜山","kr",35.1796,129.0756,"Asia/Seoul"),
        new City("柏林","de",52.5200,13.4050,"Europe/Berlin"),new City("慕尼黑","de",48.1351,11.5820,"Europe/Berlin"),
        new City("巴黎","fr",48.8566,2.3522,"Europe/Paris"),new City("里昂","fr",45.7640,4.8357,"Europe/Paris"),
        new City("罗马","it",41.9028,12.4964,"Europe/Rome"),new City("米兰","it",45.4642,9.1900,"Europe/Rome"),
        new City("马德里","es",40.4168,-3.7038,"Europe/Madrid"),new City("巴塞罗那","es",41.3874,2.1686,"Europe/Madrid"),
        new City("多伦多","ca",43.6532,-79.3832,"America/Toronto"),new City("温哥华","ca",49.2827,-123.1207,"America/Vancouver"),
        new City("悉尼","au",-33.8688,151.2093,"Australia/Sydney"),new City("墨尔本","au",-37.8136,144.9631,"Australia/Melbourne"),
        new City("圣保罗","br",-23.5505,-46.6333,"America/Sao_Paulo"),new City("里约热内卢","br",-22.9068,-43.1729,"America/Sao_Paulo"),
        new City("墨西哥城","mx",19.4326,-99.1332,"America/Mexico_City"),new City("伊斯坦布尔","tr",41.0082,28.9784,"Europe/Istanbul"),
        new City("迪拜","ae",25.2048,55.2708,"Asia/Dubai"),new City("利雅得","sa",24.7136,46.6753,"Asia/Riyadh"),
        new City("约翰内斯堡","za",-26.2041,28.0473,"Africa/Johannesburg"),new City("开普敦","za",-33.9249,18.4241,"Africa/Johannesburg"),
        new City("北京","cn",39.9042,116.4074,"Asia/Shanghai"),new City("上海","cn",31.2304,121.4737,"Asia/Shanghai"),new City("广州","cn",23.1291,113.2644,"Asia/Shanghai"),new City("深圳","cn",22.5431,114.0579,"Asia/Shanghai")
    };
}
