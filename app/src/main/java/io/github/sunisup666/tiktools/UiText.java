package io.github.sunisup666.tiktools;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;

/** Display labels are localized without rewriting existing configuration values. */
public final class UiText {
    private UiText() {}
    public static final String CUSTOM_COORDINATES="自定义坐标",CUSTOM_DEVICE="自定义",
        COORDINATES="坐标",FAVORITE_COORDINATES="收藏坐标",MAP_POINT="地图选点";

    public static Context english(Context context){
        Configuration configuration=new Configuration(context.getResources().getConfiguration());
        configuration.setLocales(LocaleList.forLanguageTags("en"));
        return context.createConfigurationContext(configuration);
    }
    public static String label(Context context,String value){
        if(value==null)return "";
        for(Presets.Region region:Presets.ALL)if(region.name().equals(value))return region.displayName(context);
        for(Presets.City city:Presets.CITIES)if(city.toString().equals(value))return city.label(context);
        int resource=switch(value){
            case CUSTOM_COORDINATES -> R.string.custom_coordinates;
            case CUSTOM_DEVICE -> R.string.custom_profile;
            case COORDINATES -> R.string.coordinate;
            case FAVORITE_COORDINATES -> R.string.favorite_coordinates;
            case MAP_POINT -> R.string.map_picker;
            case "地区默认坐标" -> R.string.region_coordinates;
            case "本机真实设备（推荐）" -> R.string.device_actual;
            case "Redmi K40 · 同平台模板" -> R.string.device_k40;
            case "POCO F3 · 同平台模板" -> R.string.device_f3;
            default -> 0;
        };
        return resource==0?value:context.getString(resource);
    }
    public static String route(Context context,String mode){
        return context.getString(switch(mode){case "circle" -> R.string.route_circle;case "route" -> R.string.route_custom;default -> R.string.route_fixed;});
    }
    public static String error(Context context,Throwable error){
        if(error instanceof Error translated)return context.getString(translated.resource,translated.arguments);
        String message=error.getMessage();
        return message==null||message.isBlank()?context.getString(R.string.operation_failed):message;
    }
    public static final class Error extends IllegalArgumentException {
        private final int resource;private final Object[] arguments;
        public Error(int resource,String fallback,Object... arguments){super(fallback);this.resource=resource;this.arguments=arguments;}
    }
}
