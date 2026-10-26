package org.Cry_Legende.sHDeathHologram;

import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.ItemHologramData;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class FancyHologramService {

    private final SHDeathHologram plugin;
    private final HologramManager hologramManager;
    private final Logger logger;
    // Hologrammes en attente de suppression : retirés à la désactivation du plugin (la tâche planifiée serait annulée)
    private final Set<Hologram> active = new HashSet<>();

    public FancyHologramService(SHDeathHologram plugin, HologramManager hologramManager) {
        this.plugin = plugin;
        this.hologramManager = hologramManager;
        this.logger = plugin.getLogger();
    }

    public void createDeathHolograms(Location deathLocation, List<String> textLines, ItemStack itemForHologram, long displayDurationTicks) {
        String textHoloName = "shdh_text_" + UUID.randomUUID().toString();
        Hologram textHologramObject = null; // Pour stocker l'objet hologramme de texte créé
        Hologram itemHologramObject = null; // Pour stocker l'objet hologramme d'item créé

        // --- Création de l'hologramme de texte ---
        double yTextOffset = plugin.getConfig().getDouble("hologram.text.y-offset", 0.7);
        Location textHologramLocation = deathLocation.clone().add(0, yTextOffset, 0);

        TextHologramData textData = new TextHologramData(textHoloName, textHologramLocation);
        textData.setText(textLines);
        textData.setPersistent(false); // Crucial: Ne pas sauvegarder cet hologramme
        applyTextHologramProperties(textData, plugin.getConfig().getConfigurationSection("hologram.text"));

        try {
            textHologramObject = hologramManager.create(textData);
            if (textHologramObject != null) {
                hologramManager.addHologram(textHologramObject); // Enregistrer l'hologramme auprès du manager
                active.add(textHologramObject);
                logger.info("Text hologram '" + textHoloName + "' created and added.");
            } else {
                logger.severe("Failed to create text hologram object for " + textHoloName + " (create() returned null).");
                return; // Si l'hologramme principal échoue, on arrête
            }
        } catch (Exception e) {
            logger.severe("Exception during text hologram creation or addition for " + textHoloName + ": " + e.getMessage());
            e.printStackTrace();
            return;
        }


        // --- Création de l'hologramme d'item (si applicable) ---
        String itemHoloName = null;
        if (itemForHologram != null && itemForHologram.getType() != Material.AIR) {
            itemHoloName = "shdh_item_" + UUID.randomUUID().toString();
            double yItemRelativeOffset = plugin.getConfig().getDouble("hologram.item.y-offset", 0.8);
            Location itemHologramLocation = textHologramLocation.clone().add(0, yItemRelativeOffset, 0);

            ItemHologramData itemData = new ItemHologramData(itemHoloName, itemHologramLocation);
            itemData.setItemStack(itemForHologram);
            itemData.setPersistent(false); // Crucial: Ne pas sauvegarder cet hologramme
            applyItemHologramProperties(itemData, plugin.getConfig().getConfigurationSection("hologram.item"));

            try {
                itemHologramObject = hologramManager.create(itemData);
                if (itemHologramObject != null) {
                    hologramManager.addHologram(itemHologramObject);
                    active.add(itemHologramObject);
                    logger.info("Item hologram '" + itemHoloName + "' created and added.");
                } else {
                    logger.severe("Failed to create item hologram object for " + itemHoloName + " (create() returned null).");
                    // On continue même si l'item holo échoue, le texte holo existe toujours
                }
            } catch (Exception e) {
                logger.severe("Exception during item hologram creation or addition for " + itemHoloName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Rendre les objets hologrammes "effectively final" pour la lambda
        final Hologram finalSharedTextHologramObject = textHologramObject;
        final Hologram finalSharedItemHologramObject = itemHologramObject;

        // --- Planification de la suppression ---
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            String logPrefix = "[Scheduler - " + textHoloName + "] ";
            logger.info(logPrefix + "Task running to remove holograms.");
            active.remove(finalSharedTextHologramObject);
            active.remove(finalSharedItemHologramObject);

            if (finalSharedTextHologramObject != null) {
                try {
                    // Vérifier si l'hologramme existe toujours dans le manager avant de tenter de le supprimer
                    // (même si setPersistent(false) est utilisé, il est bon de vérifier)
                    if (hologramManager.getHologram(finalSharedTextHologramObject.getName()).isPresent()) {
                        hologramManager.removeHologram(finalSharedTextHologramObject);
                        logger.info(logPrefix + "Text hologram '" + finalSharedTextHologramObject.getName() + "' removal requested.");
                    } else {
                        logger.warning(logPrefix + "Text hologram '" + finalSharedTextHologramObject.getName() + "' was not found in manager (already removed or desynced?).");
                    }
                } catch (Exception e) {
                    logger.severe(logPrefix + "Exception while removing text hologram '" + finalSharedTextHologramObject.getName() + "': " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                logger.warning(logPrefix + "Text hologram object was null, cannot remove.");
            }

            if (finalSharedItemHologramObject != null) {
                try {
                    if (hologramManager.getHologram(finalSharedItemHologramObject.getName()).isPresent()) {
                        hologramManager.removeHologram(finalSharedItemHologramObject);
                        logger.info(logPrefix + "Item hologram '" + finalSharedItemHologramObject.getName() + "' removal requested.");
                    } else {
                        logger.warning(logPrefix + "Item hologram '" + finalSharedItemHologramObject.getName() + "' was not found in manager.");
                    }
                } catch (Exception e) {
                    logger.severe(logPrefix + "Exception while removing item hologram '" + finalSharedItemHologramObject.getName() + "': " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                if (itemForHologram != null && itemForHologram.getType() != Material.AIR) { // Log only if an item was intended
                    logger.info(logPrefix + "Item hologram object was null (or not created), no removal needed.");
                }
            }
        }, displayDurationTicks);
    }

    /**
     * Retire tout de suite les hologrammes encore affichés (désactivation du plugin) : ils ne sont pas persistants,
     * mais resteraient dans le manager de FancyHolograms jusqu'au prochain redémarrage.
     */
    public void removeAll() {
        for (Hologram hologram : new ArrayList<>(active)) {
            try {
                hologramManager.removeHologram(hologram);
            } catch (Exception e) {
                logger.warning("Impossible de retirer l'hologramme '" + hologram.getName() + "' : " + e.getMessage());
            }
        }
        active.clear();
    }

    private void applyTextHologramProperties(TextHologramData textData, ConfigurationSection textSettings) {
        if (textSettings == null) {
            logger.warning("Configuration section 'hologram.text' not found. Using default text hologram properties.");
            textData.setBackground(Color.fromARGB(0, 0, 0, 0));
            textData.setBillboard(Display.Billboard.CENTER);
            textData.setTextAlignment(TextDisplay.TextAlignment.CENTER);
            textData.setTextShadow(true);
            textData.setSeeThrough(false);
            return;
        }

        String backgroundColorStr = textSettings.getString("background-color", "transparent");
        textData.setBackground(parseColor(backgroundColorStr, Color.fromARGB(0, 0, 0, 0)));

        try {
            String billboardStr = textSettings.getString("billboard", "CENTER").toUpperCase();
            Display.Billboard billboard = Display.Billboard.valueOf(billboardStr);
            textData.setBillboard(billboard);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid billboard value for text hologram: '" + textSettings.getString("billboard") + "'. Using CENTER. Error: " + e.getMessage());
            textData.setBillboard(Display.Billboard.CENTER);
        }

        try {
            String alignmentStr = textSettings.getString("alignment", "CENTER").toUpperCase();
            TextDisplay.TextAlignment alignment = TextDisplay.TextAlignment.valueOf(alignmentStr);
            textData.setTextAlignment(alignment);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid text-alignment value for text hologram: '" + textSettings.getString("alignment") + "'. Using CENTER. Error: " + e.getMessage());
            textData.setTextAlignment(TextDisplay.TextAlignment.CENTER);
        }

        textData.setTextShadow(textSettings.getBoolean("shadow", true));
        textData.setSeeThrough(textSettings.getBoolean("see-through", false));
    }

    private void applyItemHologramProperties(ItemHologramData itemData, ConfigurationSection itemSettings) {
        if (itemSettings == null) {
            logger.warning("Configuration section 'hologram.item' not found. Using default item hologram properties.");
            itemData.setBillboard(Display.Billboard.CENTER);
            itemData.setScale(new Vector3f(1.0f, 1.0f, 1.0f));
            return;
        }

        try {
            String billboardStr = itemSettings.getString("billboard", "CENTER").toUpperCase();
            Display.Billboard billboard = Display.Billboard.valueOf(billboardStr);
            itemData.setBillboard(billboard);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid billboard value for item hologram: '" + itemSettings.getString("billboard") + "'. Using CENTER. Error: " + e.getMessage());
            itemData.setBillboard(Display.Billboard.CENTER);
        }

        float scaleX = (float) itemSettings.getDouble("scale.x", 1.0);
        float scaleY = (float) itemSettings.getDouble("scale.y", 1.0);
        float scaleZ = (float) itemSettings.getDouble("scale.z", 1.0);
        itemData.setScale(new Vector3f(scaleX, scaleY, scaleZ));
    }

    private Color parseColor(String colorString, Color defaultColor) {
        if (colorString == null || colorString.trim().isEmpty() || "transparent".equalsIgnoreCase(colorString.trim())) {
            // logger.fine("Parsing color: '" + colorString + "' -> Transparent (ARGB: 0,0,0,0)"); // Trop verbeux pour fine peut-être
            return Color.fromARGB(0, 0, 0, 0);
        }
        String[] parts = colorString.split(",");
        try {
            if (parts.length == 3) { // R,G,B
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                // logger.fine("Parsing color: '" + colorString + "' -> RGB: " + r + "," + g + "," + b);
                return Color.fromRGB(r, g, b);
            } else if (parts.length == 4) { // A,R,G,B
                int a = Integer.parseInt(parts[0].trim());
                int r = Integer.parseInt(parts[1].trim());
                int g = Integer.parseInt(parts[2].trim());
                int b = Integer.parseInt(parts[3].trim());
                // logger.fine("Parsing color: '" + colorString + "' -> ARGB: " + a + "," + r + "," + g + "," + b);
                return Color.fromARGB(a, r, g, b);
            } else {
                logger.warning("Invalid color format: '" + colorString + "'. Using default. Expected 'transparent', 'R,G,B', or 'A,R,G,B'.");
                return defaultColor;
            }
        } catch (NumberFormatException e) {
            logger.warning("Error parsing color component in '" + colorString + "': " + e.getMessage() + ". Using default.");
            return defaultColor;
        }
    }
}
