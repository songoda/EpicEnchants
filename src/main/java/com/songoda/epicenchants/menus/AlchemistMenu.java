package com.songoda.epicenchants.menus;

import com.songoda.core.hooks.EconomyManager;
import com.songoda.core.nms.NmsManager;
import com.songoda.core.nms.nbt.NBTItem;
import com.songoda.epicenchants.EpicEnchants;
import com.songoda.epicenchants.objects.Enchant;
import com.songoda.epicenchants.objects.Group;
import com.songoda.epicenchants.objects.Placeholder;
import com.songoda.epicenchants.utils.objects.FastInv;
import com.songoda.epicenchants.utils.objects.ItemBuilder;
import com.songoda.epicenchants.utils.single.GeneralUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

import static com.songoda.epicenchants.objects.Placeholder.of;
import static com.songoda.epicenchants.utils.single.Experience.changeExp;
import static com.songoda.epicenchants.utils.single.Experience.getExp;
import static com.songoda.epicenchants.utils.single.GeneralUtils.color;
import static com.songoda.epicenchants.utils.single.GeneralUtils.getSlots;

public class AlchemistMenu extends FastInv {
    private final EpicEnchants instance;
    private final FileConfiguration config;
    private final int LEFT_SLOT, RIGHT_SLOT, PREVIEW_SLOT, ACCEPT_SLOT;
    private final ItemStack PREVIEW_ITEM, ACCEPT_ITEM;

    public AlchemistMenu(EpicEnchants instance, FileConfiguration config) {
        super(config.getInt("rows") * 9, color(config.getString("title")));

        this.instance = instance;
        this.config = config;

        LEFT_SLOT = config.getInt("left-slot");
        RIGHT_SLOT = config.getInt("right-slot");
        PREVIEW_SLOT = config.getInt("preview-slot");
        ACCEPT_SLOT = config.getInt("accept-slot");

        PREVIEW_ITEM = new ItemBuilder(config.getConfigurationSection("contents.preview")).build();
        ACCEPT_ITEM = new ItemBuilder(config.getConfigurationSection("contents.accept-before")).build();

        if (config.isConfigurationSection("fill")) {
            fill(new ItemBuilder(config.getConfigurationSection("fill")).build());
        }

        Set<String> filter = new HashSet<String>() {{
            add("preview");
            add("accept-before");
            add("accept-after");
        }};

        config.getConfigurationSection("contents").getKeys(false)
                .stream()
                .filter(s -> !filter.contains(s))
                .map(s -> "contents." + s)
                .map(config::getConfigurationSection)
                .forEach(section -> addItem(getSlots(section.getString("slot")), new ItemBuilder(section).build()));

        clear(RIGHT_SLOT);
        clear(LEFT_SLOT);

        updateSlots();

        // Player clicked an item in tinkerer
        onClick(event -> {
            if (event.getEvent().getClickedInventory() == null && event.getInventory().equals(this)) {
                return;
            }

            int slot = event.getSlot();

            if (slot != RIGHT_SLOT && slot != LEFT_SLOT) {
                return;
            }

            if (getInventory().getItem(slot) != null && getInventory().getItem(slot).getType() != Material.AIR) {
                event.getPlayer().getInventory().addItem(getInventory().getItem(slot));
                getInventory().clear(slot);
                updateSlots();
            }
        });

        // Player clicked his own inv
        onClick(event -> {
            if (event.getEvent().getClickedInventory() == null || event.getEvent().getClickedInventory().getType() != InventoryType.PLAYER) {
                return;
            }

            ItemStack itemStack = event.getItem();

            if (!handleItem(event.getPlayer(), itemStack)) {
                return;
            }

            if (itemStack.getAmount() > 1) {
                itemStack.setAmount(itemStack.getAmount() - 1);
                return;
            }

            event.getEvent().getClickedInventory().clear(event.getEvent().getSlot());
        });

        // Player closed inventory
        onClose(event -> {
            if (getInventory().getItem(RIGHT_SLOT) != null)
                event.getPlayer().getInventory().addItem(getInventory().getItem(RIGHT_SLOT));
            if (getInventory().getItem(LEFT_SLOT) != null)
                event.getPlayer().getInventory().addItem(getInventory().getItem(LEFT_SLOT));
        });
    }

    private boolean handleItem(Player player, ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        ItemStack toHandle = itemStack.clone();
        toHandle.setAmount(1);

        NBTItem nbtItem = NmsManager.getNbt().of(toHandle);

        if (!nbtItem.has("book-item") && !nbtItem.has("dust")) {
            instance.getLocale().getMessage("alchemist.notinterested").sendPrefixedMessage(player);
            return false;
        }

        // Both slots occupied
        if (getInventory().getItem(LEFT_SLOT) != null && getInventory().getItem(RIGHT_SLOT) != null) {
            instance.getLocale().getMessage("alchemist.maxtwoitems").sendPrefixedMessage(player);
            return false;
        }

        int successRate = nbtItem.getNBTObject("success-rate").asInt();

        // Both slots empty
        if (getInventory().getItem(LEFT_SLOT) == null && getInventory().getItem(RIGHT_SLOT) == null) {
            if (nbtItem.has("book-item")) {
                Enchant enchant = instance.getEnchantManager().getValue(nbtItem.getNBTObject("enchant").asString()).orElseThrow(() -> new IllegalStateException("Book without enchant!"));
                int level = nbtItem.getNBTObject("level").asInt();

                if (enchant.getMaxLevel() == level) {
                    instance.getLocale().getMessage("alchemist.maxlevelbook")
                            .sendPrefixedMessage(player);
                    return false;
                }
            } else {
                Group group = instance.getGroupManager().getValue(nbtItem.getNBTObject("group").asString()).orElseThrow(() -> new IllegalStateException("Dust without group!"));

                if (group.getOrder() == instance.getGroupManager().getValues().stream().mapToInt(Group::getOrder).max().orElse(0) || successRate == 100) {
                    instance.getLocale().getMessage("alchemist." + (successRate == 100 ? "maxpercentagedust" : "highestgroupdust"))
                            .sendPrefixedMessage(player);
                    return false;
                }
            }

            getInventory().setItem(LEFT_SLOT, toHandle);
            return true;
        }

        NBTItem other = NmsManager.getNbt().of(getInventory().getItem(getInventory().getItem(LEFT_SLOT) == null ? RIGHT_SLOT : LEFT_SLOT));
        int emptySlot = getInventory().getItem(LEFT_SLOT) == null ? LEFT_SLOT : RIGHT_SLOT;

        if (other.has("book-item")) {
            if (!nbtItem.getNBTObject("enchant").asString().equals(other.getNBTObject("enchant").asString())) {
                instance.getLocale().getMessage("alchemist.differentenchantment").sendPrefixedMessage(player);
                return false;
            }

            if (nbtItem.getNBTObject("level").asInt() != other.getNBTObject("level").asInt()) {
                instance.getLocale().getMessage("alchemist.differentlevels").sendPrefixedMessage(player);
                return false;
            }
        } else {
            if (!nbtItem.getNBTObject("group").asString().equals(other.getNBTObject("group").asString())) {
                instance.getLocale().getMessage("alchemist.differentgroups").sendPrefixedMessage(player);
                return false;
            }

            if (successRate >= 100) {
                instance.getLocale().getMessage("alchemist.maxpercentagedust").sendPrefixedMessage(player);
                return false;
            }
        }

        getInventory().setItem(emptySlot, toHandle);
        updateSlots();
        return true;
    }

    private void updateSlots() {
        if (getInventory().getItem(LEFT_SLOT) == null || getInventory().getItem(RIGHT_SLOT) == null) {
            addItem(ACCEPT_SLOT, ACCEPT_ITEM);
            addItem(PREVIEW_SLOT, PREVIEW_ITEM);
            return;
        }

        NBTItem leftItem = NmsManager.getNbt().of(getInventory().getItem(LEFT_SLOT));
        NBTItem rightItem = NmsManager.getNbt().of(getInventory().getItem(RIGHT_SLOT));
        int ecoCost;
        int expCost;

        if (leftItem.has("book-item")) {
            int level = leftItem.getNBTObject("level").asInt();
            Enchant enchant = instance.getEnchantManager().getValue(leftItem.getNBTObject("enchant").asString()).orElseThrow(() -> new IllegalStateException("Book without enchant!"));
            int leftSuccess = leftItem.getNBTObject("success-rate").asInt();
            int rightSuccess = rightItem.getNBTObject("success-rate").asInt();
            int leftDestroy = leftItem.getNBTObject("destroy-rate").asInt();
            int rightDestroy = rightItem.getNBTObject("destroy-rate").asInt();

            Placeholder[] placeholders = new Placeholder[]{
                    of("left_success_rate", leftSuccess),
                    of("right_success_rate", rightSuccess),
                    of("left_destroy_rate", leftDestroy),
                    of("right_destroy_rate", rightDestroy),
                    of("max_destroy_rate", Math.max(leftDestroy, rightDestroy)),
                    of("min_destroy_rate", Math.min(leftDestroy, rightDestroy)),
                    of("max_success_rate", Math.max(leftSuccess, rightSuccess)),
                    of("min_success_rate", Math.min(leftSuccess, rightSuccess))
            };

            int successRate = getFromFormula("book.success-rate-formula", placeholders);
            int destroyRate = getFromFormula("book.destroy-rate-formula", placeholders);

            Placeholder[] costPlaceholders = new Placeholder[]{
                    of("group_order_index", enchant.getGroup().getOrder()),
                    of("final_success_rate", successRate),
                    of("final_destroy_rate", destroyRate),
            };

            ecoCost = getFromFormula("book.eco-cost-formula", costPlaceholders);
            expCost = getFromFormula("book.exp-cost-formula", costPlaceholders);

            getInventory().setItem(PREVIEW_SLOT, enchant.getBook().get(enchant, level + 1, successRate, destroyRate));
        } else {
            Group group = instance.getGroupManager().getValue(leftItem.getNBTObject("group").asString()).orElseThrow(() -> new IllegalStateException("Dust without group!"));

            Placeholder[] placeholders = new Placeholder[]{
                    of("left_percentage", leftItem.getNBTObject("percentage").asInt()),
                    of("right_percentage", rightItem.getNBTObject("percentage").asInt())
            };

            int successRate = getFromFormula("dust.percentage-formula", placeholders);

            Placeholder[] costPlaceholders = new Placeholder[]{
                    of("group_order_index", group.getOrder()),
                    of("final_success_rate", successRate),
            };

            ecoCost = getFromFormula("dust.eco-cost-formula", costPlaceholders);
            expCost = getFromFormula("dust.exp-cost-formula", costPlaceholders);

            Group newGroup = instance.getGroupManager().getValues().stream()
                    .filter(s -> s.getOrder() == group.getOrder() + 1)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No group higher than: " + group.getIdentifier()));

            getInventory().setItem(PREVIEW_SLOT, instance.getSpecialItems().getDust(newGroup, "magic", successRate, true));
        }

        addItem(ACCEPT_SLOT, new ItemBuilder(config.getConfigurationSection("contents.accept-after"),
                of("eco_cost", ecoCost),
                of("exp_cost", expCost)
        ).build(), event -> {
            if (!EconomyManager.hasBalance(event.getPlayer(), ecoCost) || getExp(event.getPlayer()) < expCost) {
                instance.getLocale().getMessage("alchemist.cannotafford").sendPrefixedMessage(event.getPlayer());
                return;
            }

            EconomyManager.withdrawBalance(event.getPlayer(), ecoCost);
            changeExp(event.getPlayer(), -expCost);
            instance.getLocale().getMessage("alchemist.success")
                    .processPlaceholder("eco_cost", ecoCost)
                    .processPlaceholder("exp_cost", expCost)
                    .sendPrefixedMessage(event.getPlayer());

            event.getPlayer().getInventory().addItem(getInventory().getItem(PREVIEW_SLOT));
            clear(RIGHT_SLOT);
            clear(LEFT_SLOT);
            event.getPlayer().closeInventory();
        });
    }

    private int getFromFormula(String path, Placeholder... placeholders) {
        String toTest = config.getString(path);

        for (Placeholder placeholder : placeholders)
            toTest = toTest.replace(placeholder.getPlaceholder(), placeholder.getToReplace().toString());

        return (int) Double.parseDouble(GeneralUtils.parseJS(toTest, "alchemist expression", 0).toString());
    }
}