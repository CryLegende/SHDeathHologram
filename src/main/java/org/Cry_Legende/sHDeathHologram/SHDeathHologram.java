package org.Cry_Legende.sHDeathHologram;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;

public class SHDeathHologram extends JavaPlugin {

    private HologramManager hologramManager; // Stockage de la référence
    private boolean placeholderAPIEnabled;

    @Override
    public void onEnable() {
        saveDefaultConfig(); // Crée config.yml à partir des ressources s'il n'existe pas

        if (!setupDependencies()) {
            getLogger().severe("Échec de la configuration des dépendances. Désactivation de SHDeathHologram.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Enregistrer le listener seulement si les dépendances sont OK
        // Le DeathListener lui-même vérifiera si hologramManager est non-null avant d'agir
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getLogger().info("SHDeathHologram a été activé avec succès !");
    }

    @Override
    public void onDisable() {
        // Logique de désactivation si nécessaire (ex: supprimer des hologrammes persistants créés par ce plugin)
        getLogger().info("SHDeathHologram a été désactivé.");
    }

    private boolean setupDependencies() {
        // 1. Vérification et initialisation de FancyHolograms
        if (getServer().getPluginManager().getPlugin("FancyHolograms") == null) {
            getLogger().severe("FancyHolograms n'a pas été trouvé. Ce plugin est indispensable pour SHDeathHologram.");
            return false;
        }

        try {
            // Tenter de récupérer le HologramManager.
            // Si FancyHolograms est bien listé en 'depend' dans plugin.yml,
            // son onEnable() aura été appelé avant celui-ci, et .get() devrait être sûr.
            this.hologramManager = FancyHologramsPlugin.get().getHologramManager();
            if (this.hologramManager == null) {
                // Ce cas ne devrait pas arriver si FancyHologramsPlugin.get() réussit
                // et que le plugin est correctement codé, mais c'est une sécurité.
                getLogger().severe("FancyHolograms est détecté, mais son HologramManager n'a pas pu être initialisé.");
                return false;
            }
            getLogger().info("Connecté avec succès à FancyHolograms.");
        } catch (NoClassDefFoundError e) {
            getLogger().severe("Erreur de définition de classe lors de la connexion à FancyHolograms. " +
                "Assurez-vous que le JAR de FancyHolograms est correctement installé et compatible. Erreur: " + e.getMessage());
            return false;
        } catch (IllegalStateException e) {
            getLogger().severe("État illégal lors de la tentative d'accès à FancyHolograms. " +
                "Cela peut indiquer que FancyHolograms n'est pas encore complètement activé. Erreur: " + e.getMessage());
            return false;
        } catch (Exception e) { // Capture d'autres exceptions inattendues
            getLogger().severe("Une erreur inattendue est survenue lors de la connexion à FancyHolograms: " + e.getMessage());
            e.printStackTrace(); // Afficher la stack trace pour un débogage plus poussé
            return false;
        }

        // 2. Vérification de PlaceholderAPI (dépendance optionnelle/soft)
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderAPIEnabled = true;
            getLogger().info("Connecté avec succès à PlaceholderAPI.");
        } else {
            this.placeholderAPIEnabled = false;
            getLogger().info("PlaceholderAPI n'a pas été trouvé. Les placeholders de PAPI ne seront pas disponibles.");
        }

        return true;
    }

    /**
     * Vérifie si PlaceholderAPI est activé sur le serveur.
     *
     * @return true si PlaceholderAPI est activé, false sinon.
     */
    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    /**
     * Récupère le HologramManager de FancyHolograms.
     *
     * @return Le HologramManager, ou null si FancyHolograms n'est pas disponible ou n'a pas pu être initialisé.
     */
    public HologramManager getHologramManager() {
        return this.hologramManager; // Retourne la référence stockée
    }
}
