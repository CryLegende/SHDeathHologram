package org.Cry_Legende.sHDeathHologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

// Imports pour FancyHolograms (uniquement le manager est nécessaire ici maintenant)
import de.oliver.fancyholograms.api.HologramManager;

import java.util.ArrayList;
import java.util.List;

public class DeathListener implements Listener {

    private final SHDeathHologram plugin;
    private FancyHologramService hologramService; // Service pour gérer les hologrammes

    public DeathListener(SHDeathHologram plugin) {
        this.plugin = plugin;
        // Initialiser le service si FancyHolograms est disponible
        HologramManager hologramManager = plugin.getHologramManager();
        if (hologramManager != null) {
            this.hologramService = new FancyHologramService(plugin, hologramManager);
        } else {
            plugin.getLogger().severe("FancyHolograms HologramManager not found. Death holograms will not function.");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (hologramService == null) {
            // Le service n'a pas pu être initialisé (FancyHolograms manquant ou problème)
            return;
        }

        Player victim = event.getEntity();
        World world = victim.getWorld();

        if (!plugin.getConfig().getStringList("hologram.worlds").contains(world.getName())) {
            return;
        }

        Player killer = victim.getKiller();
        List<String> rawTextLinesFromConfig = getConfiguredLines(killer != null);
        List<String> linesWithPlaceholders = replacePlaceholders(rawTextLinesFromConfig, victim, killer);
        ItemStack itemStackForHologram = getHologramItem(victim, killer);
        long durationTicks = plugin.getConfig().getLong("hologram.duration", 10) * 20L; // en ticks

        try {
            hologramService.createDeathHolograms(victim.getLocation(), linesWithPlaceholders, itemStackForHologram, durationTicks);
        } catch (Exception e) {
            plugin.getLogger().severe("An unexpected error occurred while trying to create death holograms: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<String> getConfiguredLines(boolean hasKiller) {
        String path = hasKiller ? "hologram.lines-with-killer" : "hologram.lines-without-killer";
        return plugin.getConfig().getStringList(path);
    }

    private ItemStack getHologramItem(Player victim, Player killer) {
        ConfigurationSection itemSettings = plugin.getConfig().getConfigurationSection("hologram.item");
        if (itemSettings == null) {
            plugin.getLogger().fine("No 'hologram.item' configuration section found. No item will be displayed.");
            return null;
        }
        String itemTypeString = itemSettings.getString("type", "NONE").toUpperCase();

        try {
            ItemType itemType = ItemType.valueOf(itemTypeString);
            switch (itemType) {
                case WEAPON:
                    if (killer != null && killer.getInventory().getItemInMainHand().getType() != Material.AIR) {
                        return killer.getInventory().getItemInMainHand().clone();
                    }
                    return null; // Pas de tueur ou main vide
                case VICTIM_HEAD:
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    if (meta != null) { // méta ne devrait jamais être null pour PLAYER_HEAD
                        meta.setOwningPlayer(victim); // setOwningPlayer est sûr même si deprecated, setPlayerProfile est mieux si disponible
                        head.setItemMeta(meta);
                    }
                    return head;
                case CUSTOM:
                    ItemStack customStack = plugin.getConfig().getItemStack("hologram.item.custom-item-stack");
                    if (customStack == null) {
                        plugin.getLogger().warning("Custom item type selected but 'hologram.item.custom-item-stack' is not defined or invalid.");
                    }
                    return customStack; // Peut être null si non défini
                case NONE:
                default:
                    return null;
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid value for 'hologram.item.type': " + itemTypeString + ". Valid types are: NONE, WEAPON, VICTIM_HEAD, CUSTOM.");
            return null;
        }
    }

    // L'enum ItemType devrait être accessible par getHologramItem
    private enum ItemType {
        NONE, WEAPON, VICTIM_HEAD, CUSTOM
    }

    private List<String> replacePlaceholders(List<String> lines, Player victim, Player killer) {
        List<String> replacedLines = new ArrayList<>();
        String unknownKiller = plugin.getConfig().getString("hologram.unknown-killer-placeholder", "Quelqu'un");

        for (String line : lines) {
            String processedLine = line.replace("%victim%", victim.getName())
                .replace("%killer%", killer != null ? killer.getName() : unknownKiller);

            if (plugin.isPlaceholderAPIEnabled() && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                // Appliquer les placeholders PAPI pour la victime
                processedLine = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(victim, processedLine);
                // Appliquer les placeholders PAPI pour le tueur, s'il existe
                // Attention : si le tueur est null, PAPI pourrait avoir un comportement inattendu ou retourner les placeholders non remplacés.
                // Normalement, setPlaceholders(null, text) retourne le texte inchangé.
                processedLine = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(killer, processedLine); // killer peut être null
            }
            replacedLines.add(processedLine);
        }
        return replacedLines;
    }
}
