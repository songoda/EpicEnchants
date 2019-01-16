package com.songoda.epicenchants.managers;

import com.songoda.epicenchants.EpicEnchants;
import com.songoda.epicenchants.objects.Enchant;
import com.songoda.epicenchants.utils.ConfigParser;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.io.File.separator;
import static java.util.Arrays.asList;

public class FileManager {
    private final EpicEnchants instance;
    private final Map<String, FileConfiguration> configurationMap;

    public FileManager(EpicEnchants instance) {
        this.instance = instance;
        this.configurationMap = new HashMap<>();
    }

    public void createFiles() {
        File dir = new File(instance.getDataFolder() + separator + "enchants" + separator);

        if (!dir.exists()) {
            File def = new File(instance.getDataFolder() + separator + "enchants" + separator + "StrengthEnchant.yml");
            try {
                FileUtils.copyInputStreamToFile(instance.getResource("StrengthEnchant.yml"), def);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (String name : asList("config", "bookMenu")) {
            File file = new File(instance.getDataFolder(), name + ".yml");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                instance.saveResource(file.getName(), false);
            }
            FileConfiguration configuration = new YamlConfiguration();
            try {
                configuration.load(file);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
            configurationMap.put(name, configuration);
        }
    }

    public void loadEnchants() {
        File dir = new File(instance.getDataFolder() + separator + "enchants" + separator);
        Arrays.stream(dir.listFiles((dir1, filename) -> filename.endsWith(".yml"))).forEach(file -> {
            try {
                instance.getEnchantManager().addEnchant(loadEnchant(YamlConfiguration.loadConfiguration(file)));
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage("Something went wrong loading the enchant from file " + file.getName());
                Bukkit.getConsoleSender().sendMessage("Please check to make sure there are no errors in the file.");
                e.printStackTrace();
            }
        });
    }

    private Enchant loadEnchant(FileConfiguration config) {
        return Enchant.builder()
                .identifier(config.getString("identifier"))
                .tier(config.getInt("tier"))
                .maxLevel(config.getInt("max-tier"))
                .format(config.getString("applied-format"))
                .action(ConfigParser.parseActionClass(config.getConfigurationSection("action")))
                .bookItem(ConfigParser.parseBookItem(config.getConfigurationSection("book-item")))
                .itemWhitelist(config.getStringList("item-whitelist").stream().map(Material::valueOf).collect(Collectors.toSet()))
                .potionEffects(config.getConfigurationSection("potion-effects").getKeys(false).stream()
                        .map(s -> "potion-effects." + s)
                        .map(config::getConfigurationSection)
                        .map(ConfigParser::parsePotionEffect)
                        .collect(Collectors.toSet()))
                .mobs(config.getConfigurationSection("mobs").getKeys(false).stream()
                        .map(s -> "mobs." + s)
                        .map(config::getConfigurationSection)
                        .map(ConfigParser::parseMobWrapper).collect(Collectors.toSet()))
                .build();
    }

    public FileConfiguration getConfiguration(String key) {
        return configurationMap.get(key);
    }
}
