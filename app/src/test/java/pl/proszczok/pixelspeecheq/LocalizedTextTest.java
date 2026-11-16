package pl.proszczok.pixelspeecheq;

import org.junit.Test;
import java.util.regex.Pattern;
import java.util.TreeSet;
import java.util.Set;
import static org.junit.Assert.*;

public class LocalizedTextTest {
    private Set<String> placeholders(String text) {
        Set<String> result = new TreeSet<>();
        var matcher = Pattern.compile("%[0-9]+\\$[sdf]").matcher(text);
        while (matcher.find()) result.add(matcher.group());
        return result;
    }

    @Test public void allLanguagesPreserveFormattingArguments() {
        assertTrue(LocalizedText.complete());
        assertTrue(LocalizedText.keys().size() > 100);
        for (String key : LocalizedText.keys()) {
            for (String language : new String[]{"en", "de", "ru", "es", "fr", "it"}) {
                String translated = LocalizedText.textForLanguage(language, key);
                assertFalse(language + ": " + key, translated.isBlank());
                assertEquals(language + ": " + key, placeholders(key), placeholders(translated));
            }
        }
    }
}
