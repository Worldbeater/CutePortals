package net.worldbeater.CutePortals.Listeners;

import java.util.LinkedList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import net.worldbeater.CutePortals.CutePortals;
import org.bukkit.event.player.PlayerPortalEvent;

@SuppressWarnings("unused")
public class EventListener implements Listener {

    private CutePortals plugin;
    private LinkedList<String> status = new LinkedList<>();

    public EventListener(CutePortals plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        if (!status.contains(playerName)) status.add(player.getName());

        // Schedule a command executor task
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            Server server = Bukkit.getServer();
            if (plugin.messages.containsKey(playerName)) {
                // Finally let's execute command!
                String command = plugin.messages.get(playerName);
                if (command.charAt(command.length() - 2) == '@') {
                    command = command.substring(0, command.length() - 2);
                    player.performCommand(command);
                } else {
                    server.dispatchCommand(server.getConsoleSender(), command);
                }
                plugin.messages.remove(playerName);
            }
        }, 10L);

        // Schedule a slight delay (this prevents a player for reusing portal on target server)
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> status.remove(playerName), 40L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (plugin.UseQuickPortals) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("cuteportals.use")) {
            player.sendMessage(ChatColor.DARK_RED + "You don't have permission to use portals.");
            return;
        }

        Block block = player.getWorld().getBlockAt(player.getLocation());
        String data = String.format("%s#%s#%s#%s", block.getWorld().getName(),
                String.valueOf(block.getX()), String.valueOf(block.getY()), String.valueOf(block.getZ()));

        if (plugin.portalData.containsKey(data)) {
             String[] args = plugin.portalData.get(data).split("#");
             plugin.TransferPlayer(player, args[0], args[1]);
             event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.UseQuickPortals) return;

        Player player = event.getPlayer();
        String playerName = player.getName();
        if (status.contains(playerName)) return;

        Block block = player.getWorld().getBlockAt(player.getLocation());
        if (block.getType() != Material.PORTAL) return;

        if (!player.hasPermission("cuteportals.use")) {
            player.sendMessage(ChatColor.DARK_RED + "You don't have permission to use portals.");
            return;
        }

        String data = String.format("%s#%s#%s#%s", block.getWorld().getName(),
                String.valueOf(block.getX()), String.valueOf(block.getY()), String.valueOf(block.getZ()));

        if (plugin.portalData.containsKey(data)) {
            status.add(playerName);
            String[] args = plugin.portalData.get(data).split("#");
            plugin.TransferPlayer(player, args[0], args[1]);
        }

        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> status.remove(playerName), 40L);
    }

}
