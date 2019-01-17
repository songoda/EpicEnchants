package com.songoda.epicenchants.utils;

import com.songoda.epicenchants.objects.ActionClass;
import com.songoda.epicenchants.objects.BookItem;
import com.songoda.epicenchants.objects.LeveledModifier;
import com.songoda.epicenchants.wrappers.*;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import java.util.stream.Collectors;

import static com.songoda.epicenchants.utils.Chat.color;

public class ConfigParser {
    public static ActionClass parseActionClass(ConfigurationSection section) {
        return ActionClass.builder()
                .modifyDamageGiven(LeveledModifier.of(section.getString("modify-damage-given")))
                .modifyDamageTaken(LeveledModifier.of(section.getString("modify-damage-taken")))
                .potionEffectsWearer(ConfigParser.getPotionChanceSet(section.getConfigurationSection("potion-effects-defendant")))
                .potionEffectOpponent(ConfigParser.getPotionChanceSet(section.getConfigurationSection("potion-effects-opponent")))
                .build();
    }

    private static Set<PotionChanceWrapper> getPotionChanceSet(ConfigurationSection section) {
        return section.getKeys(false).stream()
                .map(section::getConfigurationSection)
                .map(ConfigParser::parsePotionChanceEffect)
                .collect(Collectors.toSet());
    }

    public static PotionChanceWrapper parsePotionChanceEffect(ConfigurationSection section) {
        return PotionChanceWrapper.chanceBuilder()
                .type(PotionEffectType.getByName(section.getName()))
                .amplifier(LeveledModifier.of(section.getString("amplifier")))
                .duration(LeveledModifier.of(section.getString("duration")))
                .chance(LeveledModifier.of(section.getString("chance")))
                .build();
    }

    public static PotionEffectWrapper parsePotionEffect(ConfigurationSection section) {
        return PotionEffectWrapper.builder()
                .type(PotionEffectType.getByName(section.getName()))
                .amplifier(LeveledModifier.of(section.getString("amplifier")))
                .duration(LeveledModifier.of(section.getString("duration")))
                .build();
    }

    public static MobWrapper parseMobWrapper(ConfigurationSection section) {
        return MobWrapper.builder()
                .amount(section.getInt("amount"))
                .spawnPercentage(LeveledModifier.of(section.getString("spawn-percentage")))
                .health(section.getInt("health"))
                .attackDamage(section.getDouble("attack-damage"))
                .hostile(section.getBoolean("hostile"))
                .displayName(color(section.getString("display-name")))
                .helmet(new ItemBuilder(section.getConfigurationSection("armor.helmet")))
                .leggings(new ItemBuilder(section.getConfigurationSection("armor.chest-plate")))
                .chestPlate(new ItemBuilder(section.getConfigurationSection("armor.leggings")))
                .boots(new ItemBuilder(section.getConfigurationSection("armor.boots")).build())
                .build();
    }

    public static EnchantmentWrapper parseEnchantmentWrapper(String key) {
        return EnchantmentWrapper.builder()
                .amplifier(LeveledModifier.of(key.contains(":") ? key.split(":")[1] : ""))
                .enchantment(Enchantment.getByName(key.split(":")[0]))
                .build();
    }

    public static BookItem parseBookItem(ConfigurationSection section) {
        return BookItem.builder()
                .material(Material.valueOf(section.getString("material")))
                .displayName(color(section.getString("display-name")))
                .lore(section.getStringList("lore").stream().map(Chat::color).collect(Collectors.toList()))
                .build();
    }
}
