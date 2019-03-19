package com.songoda.epicenchants.effect.effects;

import com.songoda.epicenchants.effect.EffectExecutor;
import com.songoda.epicenchants.enums.EventType;
import com.songoda.epicenchants.enums.TriggerType;
import com.songoda.epicenchants.objects.LeveledModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class Potion extends EffectExecutor {
    public Potion(ConfigurationSection section) {
        super(section);
    }

    @Override
    public void execute(@NotNull Player wearer, LivingEntity opponent, int level, EventType eventType) {
        if (!getSection().isString("potion-type")) {
            return;
        }

        LeveledModifier amplifier = LeveledModifier.of(getSection().getString("amplifier"));
        PotionEffectType effectType = PotionEffectType.getByName(getSection().getString("potion-type"));

        if (effectType == null) {
            return;
        }

        if (this.getTriggerType() == TriggerType.STATIC_EFFECT || this.getTriggerType() == TriggerType.HELD_ITEM) {
            if (eventType == EventType.ON) {
                consume(entity -> entity.addPotionEffect(new PotionEffect(effectType, Integer.MAX_VALUE, ((int) amplifier.get(level, 0)),
                        false, false)), wearer, opponent);
            } else if (eventType == EventType.OFF) {
                consume(entity -> entity.removePotionEffect(effectType), wearer, opponent);
            }
            return;
        }

        LeveledModifier duration = LeveledModifier.of(getSection().getString("duration"));

        consume(entity -> entity.addPotionEffect(new PotionEffect(effectType, ((int) duration.get(level, 60)),
                ((int) amplifier.get(level, 0)), false, false)), wearer, opponent);
    }

}