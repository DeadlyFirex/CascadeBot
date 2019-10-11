package org.cascadebot.cascadebot.data.graphql.services;

import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.GraphQLRootContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cascadebot.cascadebot.commandmeta.Module;
import org.cascadebot.cascadebot.data.graphql.objects.QLContext;
import org.cascadebot.cascadebot.data.graphql.objects.SettingsWrapper;
import org.cascadebot.cascadebot.data.objects.GuildSettingsCore;
import org.cascadebot.cascadebot.data.objects.Setting;
import org.cascadebot.cascadebot.data.objects.Tag;
import org.cascadebot.cascadebot.utils.ReflectionUtils;
import org.cascadebot.cascadebot.utils.SettingsUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SettingsService {

    @Getter
    private static SettingsService instance = new SettingsService();

    @GraphQLQuery
    public List<SettingsWrapper> settingsInformation() {
        return SettingsUtils.getAllSettings()
                .values()
                .stream()
                .map(field -> SettingsWrapper.from(field, field.getAnnotation(Setting.class)))
                .collect(Collectors.toList());
    }

    @GraphQLMutation
    public Set<Module> enableModule(@GraphQLRootContext QLContext context, long guildId, Module module) {
        return context.runIfAuthenticatedGuild(guildId, (guild, member) -> {
            GuildSettingsCore coreSettings = context.getGuildData(guildId).getCoreSettings();
            coreSettings.enableModule(module);
            return coreSettings.getEnabledModules();
        });
    }

    @GraphQLMutation
    public Set<Module> disableModule(@GraphQLRootContext QLContext context, long guildId, Module module) {
        return context.runIfAuthenticatedGuild(guildId, (guild, member) -> {
            GuildSettingsCore coreSettings = context.getGuildData(guildId).getCoreSettings();
            coreSettings.disableModule(module);
            return coreSettings.getEnabledModules();
        });
    }

    @GraphQLMutation
    public Map<String, Tag> updateTags(@GraphQLRootContext QLContext context, long guildId, Map<String, Tag> tags) {
        return context.runIfAuthenticatedGuild(guildId, (guild, member) -> {
            GuildSettingsCore coreSettings = context.getGuildData(guildId).getCoreSettings();
            tags.forEach(coreSettings::addTag);
            return coreSettings.getTags();
        });
    }

    @GraphQLMutation
    public Map<String, Tag> removeTags(@GraphQLRootContext QLContext context, long guildId, Set<String> tags) {
        return context.runIfAuthenticatedGuild(guildId, (guild, member) -> {
            GuildSettingsCore coreSettings = context.getGuildData(guildId).getCoreSettings();
            tags.forEach(coreSettings::removeTag);
            return coreSettings.getTags();
        });
    }

    @GraphQLMutation
    public String setPrefix(@GraphQLRootContext QLContext context, long guildId, String prefix) {
        return context.runIfAuthenticatedGuild(guildId, (guild, member) -> {
            char[] chars = prefix.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                if (i == 0) {
                    assert chars[i] > 32;
                } else {
                    assert chars[i] >= 32;
                }
            }
            context.getGuildData(guildId).getCoreSettings().setPrefix(prefix);
            return prefix;
        });
    }

    @GraphQLMutation
    public GuildSettingsCore updateCoreSettings(@GraphQLRootContext QLContext context, long guildId, @GraphQLNonNull Map<String, Object> newSettings) {
        return context.runIfAuthenticatedGuild(guildId, (guild, member) -> {
            try {
                GuildSettingsCore coreSettings = context.getGuildData(guildId).getCoreSettings();
                for (String key : newSettings.keySet()) {
                    try {
                        Field field = GuildSettingsCore.class.getDeclaredField(key);
                        if (field.getAnnotation(Setting.class) != null) {
                            Setting setting = field.getAnnotation(Setting.class);
                            if (setting.directlyEditable()) {
                                field.setAccessible(true);
                                field.set(coreSettings, newSettings.get(key));
                            } else {
                                throw new RuntimeException(String.format("You are trying to modify the setting \"%s\" which isn't directly editable!", key));
                            }
                        }
                    } catch (NoSuchFieldException ignored) {

                    }
                }
                return coreSettings;
            } catch (IllegalAccessException e) {
                // Rethrow this to be shown to graphql
                throw new RuntimeException(e);
            }
        });
    }

}
