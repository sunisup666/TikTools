package io.github.sunisup666.tiktools.probe;

import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Base64;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Exercise the installed module's resources and validation without writing user settings. */
public final class LocaleInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static Context localized(Context context,String locale){Configuration c=new Configuration(context.getResources().getConfiguration());c.setLocales(LocaleList.forLanguageTags(locale));return context.createConfigurationContext(c);}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            String module="io.github.sunisup666.tiktools";
            Context probe=getTargetContext();
            check(probe.getPackageManager().checkSignatures(probe.getPackageName(),module)==PackageManager.SIGNATURE_MATCH,"Module signature differs");
            Context context=probe.createPackageContext(module,Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
            Context en=localized(context,"en"),zh=localized(context,"zh-Hans");
            ClassLoader loader=context.getClassLoader();Class<?> presets=loader.loadClass(module+".Presets"),text=loader.loadClass(module+".UiText"),config=loader.loadClass(module+".Config");
            Method label=text.getMethod("label",Context.class,String.class);
            JSONObject defaults=(JSONObject)config.getMethod("defaults").invoke(null);String before=defaults.toString();
            int cityCount=0,regionCount=0;
            for(Object city:(Object[])presets.getField("CITIES").get(null)){
                String value=(String)city.getClass().getMethod("label",Context.class).invoke(city,en);
                check(!value.matches(".*[\\p{IsHan}].*"),"Chinese in English city label");
                check(label.invoke(null,en,city.toString()).equals(value),"Stored city label mismatch");cityCount++;
            }
            for(Object region:(Object[])presets.getField("ALL").get(null)){
                String value=(String)region.getClass().getMethod("displayName",Context.class).invoke(region,en);
                check(!value.isBlank()&&!value.matches(".*[\\p{IsHan}].*"),"Invalid English region label");regionCount++;
            }
            String custom="My custom location";check(label.invoke(null,en,custom).equals(custom),"Custom label changed");
            JSONObject invalid=new JSONObject(defaults.toString()).put("lat","100");Throwable failure;
            try{config.getMethod("validate",JSONObject.class).invoke(null,invalid);throw new AssertionError("Invalid latitude accepted");}
            catch(InvocationTargetException e){failure=e.getCause();}
            Method error=text.getMethod("error",Context.class,Throwable.class);
            String english=(String)error.invoke(null,en,failure),chinese=(String)error.invoke(null,zh,failure);
            check(english.equals("lat is out of range"),"English validation missing");check(chinese.equals("lat 超出范围"),"Chinese validation missing");
            check(defaults.toString().equals(before),"Display localization changed configuration");
            JSONObject report=new JSONObject().put("passed",true).put("city_labels",cityCount).put("region_labels",regionCount).put("validation_en",english).put("validation_zh",chinese).put("custom_label_preserved",true).put("input_configuration_unchanged",true);
            result.putString("result_b64",Base64.encodeToString(report.toString().getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP));finish(-1,result);
        }catch(Throwable e){result.putString("error",e.getClass().getSimpleName()+": "+e.getMessage());finish(0,result);}
    }
}
