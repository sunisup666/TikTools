package io.github.sunisup666.tiktools;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import java.util.concurrent.TimeUnit;

public final class TranslationBroker {
    public static String translate(String text,String source,String target,boolean allowDownload) throws Exception {
        if (text==null || text.isBlank() || text.length()>3000) throw new UiText.Error(R.string.error_translation_length,"Translation text must contain 1–3000 characters");
        if (source==null || source.equals("auto")) {
            try (LanguageIdentifier identifier=LanguageIdentification.getClient()) {
                source=Tasks.await(identifier.identifyLanguage(text),8,TimeUnit.SECONDS);
            }
        }
        String from=TranslateLanguage.fromLanguageTag(source),to=TranslateLanguage.fromLanguageTag(target);
        if (from==null || to==null) throw new UiText.Error(R.string.error_translation_language,"This language is not supported for on-device translation");
        if (from.equals(to)) return text;
        try (Translator translator=Translation.getClient(new TranslatorOptions.Builder().setSourceLanguage(from).setTargetLanguage(to).build())) {
            if (allowDownload) Tasks.await(translator.downloadModelIfNeeded(new DownloadConditions.Builder().requireWifi().build()),60,TimeUnit.SECONDS);
            return Tasks.await(translator.translate(text),20,TimeUnit.SECONDS);
        }
    }
}
