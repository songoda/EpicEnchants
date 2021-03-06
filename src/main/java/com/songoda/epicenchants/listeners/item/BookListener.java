package com.songoda.epicenchants.listeners.item;

import com.songoda.core.nms.nbt.NBTItem;
import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.epicenchants.EpicEnchants;
import com.songoda.epicenchants.enums.EnchantResult;
import com.songoda.epicenchants.events.EnchantApplyEvent;
import com.songoda.epicenchants.objects.Enchant;
import com.songoda.epicenchants.objects.Group;
import com.songoda.epicenchants.utils.Tuple;
import com.songoda.epicenchants.utils.single.GeneralUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

import static com.songoda.epicenchants.enums.EnchantResult.ALREADY_APPLIED;
import static com.songoda.epicenchants.enums.EnchantResult.BROKEN_FAILURE;
import static com.songoda.epicenchants.enums.EnchantResult.CONFLICT;
import static com.songoda.epicenchants.enums.EnchantResult.MAXED_OUT;
import static java.util.concurrent.ThreadLocalRandom.current;

public class BookListener extends ItemListener {
    public BookListener(EpicEnchants instance) {
        super(instance);
    }

    @Override
    void onApply(InventoryClickEvent event, NBTItem cursor, NBTItem current) {
        if (!cursor.has("book-item") || !cursor.getNBTObject("book-item").asBoolean()) {
            return;
        }

        event.setCancelled(true);

        ItemStack toApply = event.getCurrentItem();
        Enchant enchant = instance.getEnchantManager().getValue(cursor.getNBTObject("enchant").asString()).orElseThrow(() -> new IllegalStateException("Book without enchant!"));

        if (!enchant.getItemWhitelist().contains(CompatibleMaterial.getMaterial(current.finish()))) {
            return;
        }
        // get total amount of enchantments on item
        int currentEnchantmentTotal = instance.getEnchantUtils().getEnchants(toApply).size();
        int maxAllowedOverride = instance.getEnchantUtils().getMaximumEnchantsCanApply((Player) event.getWhoClicked());
        int maxAllowedApply = instance.getEnchantUtils().getMaximumEnchantsCanApplyItem(toApply, (Player) event.getWhoClicked());
        maxAllowedApply = Math.min(maxAllowedApply, maxAllowedOverride);
        // item is at max enchantments
        if (currentEnchantmentTotal >= maxAllowedApply) {
            instance.getLocale().getMessage("enchants.maxallowed").processPlaceholder("max_enchants", maxAllowedApply).sendPrefixedMessage(event.getWhoClicked());
            return;
        }

        int level = cursor.getNBTObject("level").asInt();
        int successRate = cursor.getNBTObject("success-rate").asInt();
        int destroyRate = cursor.getNBTObject("destroy-rate").asInt();

        EnchantApplyEvent enchantEvent = new EnchantApplyEvent(toApply, enchant, level, successRate, destroyRate);
        Bukkit.getPluginManager().callEvent(enchantEvent);

        if (enchantEvent.isCancelled()) {
            return;
        }

        Tuple<ItemStack, EnchantResult> result = instance.getEnchantUtils().apply(toApply, enchant, enchantEvent.getLevel(), enchantEvent.getSuccessRate(), enchantEvent.getDestroyRate());

        instance.getLocale().getMessage(GeneralUtils.getMessageFromResult(result.getRight()))
                .processPlaceholder("enchant", enchant.getIdentifier())
                .sendPrefixedMessage(event.getWhoClicked());

        if (result.getRight() == BROKEN_FAILURE) {
            event.getClickedInventory().clear(event.getSlot());
        }

        if (result.getRight() != CONFLICT && result.getRight() != MAXED_OUT && result.getRight() != ALREADY_APPLIED) {
            useItem(event);
        }

        event.getClickedInventory().setItem(event.getSlot(), result.getLeft());
    }

    @Override
    void onClick(PlayerInteractEvent event, NBTItem clicked) {
        if (!clicked.has("mystery-book") || !clicked.getNBTObject("mystery-book").asBoolean()) {
            return;
        }

        event.setCancelled(true);

        if (event.getItem().getAmount() != 1 && event.getPlayer().getInventory().firstEmpty() == -1) {
            return;
        }

        Group group = instance.getGroupManager().getValue(clicked.getNBTObject("group").asString()).orElseThrow(() -> new IllegalStateException("Book without group!"));

        Optional<Enchant> enchant = instance.getEnchantManager().getRandomEnchant(group);

        if (!enchant.isPresent()) {
            throw new IllegalStateException("The " + group.getName() + " group does not have any enchants.");
        }

        int level = current().nextInt(enchant.get().getMaxLevel()) + 1;

        useItem(event);
        event.getPlayer().getInventory().addItem(enchant.get().getBook().get(enchant.get(), level));

        event.getPlayer().sendMessage(instance.getLocale().getMessage("book.discover")
                .processPlaceholder("group_name", group.getName())
                .processPlaceholder("group_color", group.getColor())
                .processPlaceholder("enchant_format", enchant.get().getFormat())
                .processPlaceholder("level", level)
                .getPrefixedMessage());
    }
}
