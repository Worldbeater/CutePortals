package net.worldbeater.CutePortals;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;

class Commands implements CommandExecutor {

    private CutePortals plugin;

    Commands(CutePortals plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        switch (args.length) {
            case 0:
                if (!hasPermission("cuteportals.create", sender)) break;
                sender.sendMessage(String.format("%s %sCutePortals v%s",
                        ChatColor.YELLOW, ChatColor.BOLD, plugin.getDescription().getVersion()));
                sender.sendMessage(String.format("%s/cps reload %s- %sReloads all files and data.",
                        ChatColor.GREEN, ChatColor.WHITE, ChatColor.RED));
                sender.sendMessage(String.format("%s/cps create <server> <planned command> %s- %sCreates portals.",
                        ChatColor.GREEN, ChatColor.WHITE, ChatColor.RED));
                sender.sendMessage(String.format("%sExample: %s/cps create lobby warp spawn {PLAYER}",
                        ChatColor.YELLOW, ChatColor.GRAY));
                sender.sendMessage(String.format("%s{PLAYER} %sstates for player name, %s{NONE} %sstates for no " +
                                "server or no command.",
                        ChatColor.GRAY, ChatColor.YELLOW, ChatColor.GRAY, ChatColor.YELLOW));
                sender.sendMessage(String.format("%s/cps remove %s- %sRemoves portals in selection.",
                        ChatColor.GREEN, ChatColor.WHITE, ChatColor.RED));
                break;
            case 1:
                switch (args[0].toLowerCase()) {
                    case "reload":
                        if (!hasPermission("cuteportals.create", sender)) break;
                        plugin.loadFiles();
                        plugin.loadPortalsData();
                        plugin.messages.clear();
                        sender.sendMessage(ChatColor.GREEN + "Configs and data have been reloaded.");
                        break;
                    case "remove":
                        if (!hasPermission("cuteportals.create", sender)) break;
                        CuboidSelection selection = getWorldEditSelection(sender);
                        if (selection == null) break;
                        List<Location> locations = getLocationsFromCuboid(selection);
                        for (Location location : locations) {
                            Block block = ((Player)sender).getWorld().getBlockAt(location);
                            String portal = block.getWorld().getName() + "#"
                                        + String.valueOf(block.getX()) + "#"
                                        + String.valueOf(block.getY()) + "#"
                                        + String.valueOf(block.getZ());
                            if (plugin.portalData.containsKey(portal)) {
                                plugin.portalData.remove(portal);
                                plugin.getLogger().log(Level.INFO,"Removing portal block at: " + portal);
                            }
                        }
                        plugin.savePortalsData();
                        sender.sendMessage(ChatColor.GREEN + "Portals have been removed.");
                        break;
                    default:
                        if (hasPermission("cuteportals.create", sender)) showHelp(sender);
                        break;
                }
                break;
            default:
                switch (args[0].toLowerCase()) {
                    case "create":
                        if (!hasPermission("cuteportals.create", sender)) break;
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.GREEN + "Invalid syntax for create command. Type " +
                                    ChatColor.RED + "/cuteportals " + ChatColor.GREEN + "for help.");
                            break;
                        }
                        StringBuilder command = new StringBuilder();
                        for (int counter = 2; counter < args.length; counter++) {
                            command.append(args[counter]);
                            command.append(" ");
                        }
                        CuboidSelection selection = getWorldEditSelection(sender);
                        if (selection == null) break;
                        List<Location> locations = getLocationsFromCuboid(selection);
                        for (Location location : locations) {
                            Block block = ((Player)sender).getWorld().getBlockAt(location);
                            String portal = block.getWorld().getName() + "#"
                                        + String.valueOf(block.getX()) + "#"
                                        + String.valueOf(block.getY()) + "#"
                                        + String.valueOf(block.getZ());
                            plugin.portalData.put(portal, args[1] + "#" + command.toString());
                        }
                        plugin.savePortalsData();
                        sender.sendMessage(ChatColor.GREEN + String.valueOf(locations.size()) +
                                " portals have been created. Portal data has been saved to disk.");
                        break;
                    case "goto":
                        if (!hasPermission("cuteportals.create", sender)) break;
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.RED + "Bad syntax in goto command.");
                            break;
                        }
                        StringBuilder gotocmd = new StringBuilder();
                        for (int counter = 2; counter < args.length; counter++) {
                            gotocmd.append(args[counter]);
                            gotocmd.append(" ");
                        }
                        plugin.TransferPlayer((Player)sender, args[1], gotocmd.toString());
                        break;
                    default:
                        if (hasPermission("cuteportals.create", sender)) showHelp(sender);
                        break;
                }
                break;
        }
        return true;
    }

    private boolean hasPermission(String permission, CommandSender sender) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(ChatColor.DARK_RED + "Insuffient permissions.");
            return false;
        } else return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(String.format("%sUnknown syntax. Type %s/cuteportals %sfor help!",
                ChatColor.GREEN, ChatColor.RED, ChatColor.GREEN));
    }

    private CuboidSelection getWorldEditSelection(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return null;
        }
        Player player = (Player)sender;
        Selection selection = plugin.worldEdit.getSelection(player);
        if ((selection == null) || !(selection instanceof CuboidSelection)) {
            sender.sendMessage(ChatColor.RED + "Please, select a cuboid using WorldEdit.");
            return null;
        }
        return (CuboidSelection) selection;
    }

    private List<Location> getLocationsFromCuboid(CuboidSelection cuboid) {
        List<Location> locations = new ArrayList<>();
        Location minLocation = cuboid.getMinimumPoint();
        Location maxLocation = cuboid.getMaximumPoint();
        for (int i1 = minLocation.getBlockX(); i1 <= maxLocation.getBlockX(); i1++) {
            for (int i2 = minLocation.getBlockY(); i2 <= maxLocation.getBlockY(); i2++) {
                for (int i3 = minLocation.getBlockZ(); i3 <= maxLocation.getBlockZ(); i3++) {
                    locations.add(new Location(cuboid.getWorld(), i1, i2, i3));
                }
            }
        }
        return locations;
    }

}
