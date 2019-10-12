package org.cascadebot.cascadebot.data.graphql.services;

import com.google.gson.JsonObject;
import io.github.binaryoverload.JSONConfig;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.cascadebot.cascadebot.data.language.Language;
import org.cascadebot.cascadebot.data.language.Locale;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LanguageService {

    @Getter
    private static LanguageService instance = new LanguageService();

    @GraphQLQuery(description = "Returns the language file corresponding to the language key specified. Optionally, a sub path can be passed in to retrieve a sub-section of the language file.")
    public JsonObject language(@Nonnull String languageKey, String subPath) {
        Locale locale = Arrays.stream(Locale.values())
                .filter(locale1 -> locale1.getLanguageCode().equalsIgnoreCase(languageKey))
                .findFirst()
                .orElse(null);

        if (locale == null) {
            return null;
        }
        JSONConfig languageFile = Language.getLanguage(locale);
        if (languageFile == null) {
            return null;
        }

        if (!StringUtils.isBlank(subPath)) {
            Optional<JSONConfig> subConfig = languageFile.getSubConfig(subPath);
            if (subConfig.isPresent()) {
                languageFile = subConfig.get();
            }
        }

        return languageFile.getObject();
    }

}
