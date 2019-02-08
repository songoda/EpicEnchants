package com.songoda.epicenchants.effect;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import static com.google.common.base.CaseFormat.*;

public class EffectManager {

    public static Optional<EffectExecutor> getEffect(ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }

        try {
            String formatted = UPPER_UNDERSCORE.to(UPPER_CAMEL, section.getName().toLowerCase()).replaceAll("-.*$", "");
            Class<?> clazz = Class.forName("com.songoda.epicenchants.effect.effects." + formatted);
            Constructor<?> constructor = clazz.getConstructor(ConfigurationSection.class);
            Object object = constructor.newInstance(section);
            return Optional.of((EffectExecutor) object);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException | ClassCastException e) {
            Bukkit.getLogger().severe("Invalid effect: " + section.getName());
        }

        return Optional.empty();
    }
}
