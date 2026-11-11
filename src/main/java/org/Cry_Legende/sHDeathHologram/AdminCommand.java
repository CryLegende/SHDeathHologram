package org.Cry_Legende.sHDeathHologram;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /shdeathhologram reload : seule sous-commande, utilisable depuis la console.
 */
final class AdminCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_RELOAD = "shdeathhologram.admin.reload";

    private final SHDeathHologram plugin;

    AdminCommand(SHDeathHologram plugin) {
        this.plugin = plugin;
    }

    /** Sous-commandes que l'expéditeur a le droit d'utiliser. */
    private List<String> allowed(CommandSender sender) {
        List<String> subs = new ArrayList<>();
        if (sender.hasPermission(PERMISSION_RELOAD)) {
            subs.add("reload");
        }
        return subs;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.messages();
        List<String> subs = allowed(sender);
        if (subs.isEmpty()) {
            sender.sendMessage(messages.get("pas-de-permission"));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            String error = plugin.reloadAll();
            if (error == null) {
                sender.sendMessage(messages.get("reload-ok"));
            } else {
                String[] parts = error.split(" : ", 2);
                sender.sendMessage(messages.get("reload-erreur", "{fichier}", parts[0], "{erreur}", parts.length > 1 ? parts[1] : ""));
            }
            return true;
        }
        sender.sendMessage(messages.get("usage-liste", "{sous-commandes}", String.join("|", subs)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String start = args[0].toLowerCase(Locale.ROOT);
            for (String sub : allowed(sender)) {
                if (sub.startsWith(start)) {
                    result.add(sub);
                }
            }
        }
        return result;
    }
}
