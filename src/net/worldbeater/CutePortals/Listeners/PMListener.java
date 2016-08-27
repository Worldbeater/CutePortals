package net.worldbeater.CutePortals.Listeners;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import net.worldbeater.CutePortals.CutePortals;

public class PMListener implements PluginMessageListener {

    private CutePortals plugin;
    public PMListener(CutePortals plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            ByteArrayDataInput input = ByteStreams.newDataInput(message);
            String subchannel = input.readUTF();
            if (!subchannel.equals("MCraft")) return;

            short len = input.readShort();
            byte[] messagebytes = new byte[len];
            input.readFully(messagebytes);
            DataInputStream messagestream = new DataInputStream(new ByteArrayInputStream(messagebytes));

            try {
                String[] commandData = messagestream.readUTF().split("#"); // HERE WE READ A FULL COMMAND
                String command = commandData[0];
                String playerName = commandData[1];
                Server server = plugin.getServer();
                if (player.getName().equals(playerName)) {
                    // Finally let's execute command!
                    if (command.charAt(command.length() - 2) == '@') {
                        command = command.substring(0, command.length() - 2);
                        player.performCommand(command);
                    } else {
                        server.dispatchCommand(server.getConsoleSender(), command.replace("{PLAYER}", playerName));
                    }
                } else {
                    plugin.messages.put(playerName, command);
                    plugin.logger.log(Level.INFO, "New PM data put for: " + playerName + ", with command: " + command);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }, 5L);
    }
}





