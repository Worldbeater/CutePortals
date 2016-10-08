package net.worldbeater.CutePortals;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.worldbeater.CutePortals.Listeners.EventListener;
import net.worldbeater.CutePortals.Listeners.PMListener;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class CutePortals extends JavaPlugin {

    public LinkedHashMap<String, String> messages = new LinkedHashMap<>();
    public Logger logger = this.getLogger();
    public Map<String, String> portalData = new HashMap<>();
    public boolean UseQuickPortals;
    private boolean UseMetrics;
    private YamlConfiguration portalsFile;
    WorldEditPlugin worldEdit;

    @Override
    public void onEnable() {
        long time = System.currentTimeMillis();

        if (getServer().getPluginManager().getPlugin("WorldEdit") == null) {
            getPluginLoader().disablePlugin(this);
            throw new NullPointerException(ChatColor.RED + "WorldEdit not found! Disabling...");
        }

        worldEdit = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
        getCommand("cuteportals").setExecutor(new Commands(this));
        logger.log(Level.INFO, "Commands registered!");

        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        logger.log(Level.INFO, "Events registered!");

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new PMListener(this));
        logger.log(Level.INFO, "Plugin messaging channels registered!");

        loadFiles();
        loadPortalsData();

        if (UseMetrics) {
            try {
                Metrics metrics = new Metrics(this);
                metrics.start();
                logger.log(Level.INFO, "Metrics successfully registered!");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error! Metrics can not be registered!");
            }
        } else {
            logger.log(Level.INFO, "Skiping metrics enabling.");
        }

        logger.log(Level.INFO, "Version " + getDescription().getVersion() + " has been enabled. ("
                + (System.currentTimeMillis() - time) + "ms)");
    }

    @Override
    public void onDisable() {
        long time = System.currentTimeMillis();
        savePortalsData();
        logger.log(Level.INFO, "Version " + getDescription().getVersion() + " has been disabled. ("
                + (System.currentTimeMillis() - time) + "ms)");
    }

    public void TransferPlayer(Player player, String server, String command) {
        try {
            String playerName = player.getName();
            logger.log(Level.INFO, "Checking if " + playerName + " can use portals...");
            if (player.hasPermission("cuteportals.use")) {

                // Connection to a server.
                if (!server.equalsIgnoreCase("{none}")) {

                    // Here portals work like PM sender/receiver: they send messages to each other
                    // and then decide, what player should do after he arrives somewhere.
                    command = command.replace("{PLAYER}", playerName) + "#" + playerName;
                    logger.log(Level.INFO, "Sending connect PM to BungeeCord...");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeUTF("Connect");
                    dos.writeUTF(server); // TARGET SERVER
                    player.sendPluginMessage(this, "BungeeCord", baos.toByteArray());
                    baos.close();
                    dos.close();

                    // Command that a server should execute.
                    if (!command.equalsIgnoreCase("{none}")) {
                        logger.log(Level.INFO, "Sending forward PM to BungeeCord...");
                        baos = new ByteArrayOutputStream();
                        dos = new DataOutputStream(baos);
                        dos.writeUTF("Forward");
                        dos.writeUTF(server); // TARGET SERVER

                        dos.writeUTF("MCraft");
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        DataOutputStream out = new DataOutputStream(bytes);
                        out.writeUTF(command); // COMMAND
                        dos.writeShort(bytes.toByteArray().length);
                        dos.write(bytes.toByteArray());

                        player.sendPluginMessage(this, "BungeeCord", baos.toByteArray());
                        bytes.close();
                        out.close();

                        baos.close();
                        dos.close();
                    }
                } else {
                    // Here portals ignore PM part. They only execute commands for players, when they enter portals.
                    Server local = this.getServer();
                    logger.log(Level.INFO, "Executing command for " + playerName + "...");
                    if (command.charAt(command.length() - 2) == '@') {
                        command = command.substring(0, command.length() - 2);
                        player.performCommand(command);
                    } else {
                        local.dispatchCommand(local.getConsoleSender(), command.replace("{PLAYER}", playerName));
                    }
                }

            } else {
                player.sendMessage(ChatColor.DARK_RED + "You don't have permission for using portals.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void createConfigFile(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void loadFiles() {

        // Create config file if not exists
        File cFile = new File(getDataFolder(), "config.yml");
        if (!cFile.exists()) {
            if (cFile.getParentFile().mkdirs()) {
                createConfigFile(getResource("config.yml"), cFile);
                logger.log(Level.INFO, "Configuration file created!");
            }
        }

        // Check config for right parameters existance
        this.reloadConfig();
        Configuration config = this.getConfig();
        UseQuickPortals = Boolean.parseBoolean(config.getString("UseQuickPortals"));
        config.set("UseQuickPortals", UseQuickPortals);
        UseMetrics = Boolean.parseBoolean(config.getString("UseMetrics"));
        config.set("UseMetrics", UseMetrics);
        this.saveConfig();

        logger.log(Level.INFO, "Configuration file reloaded! " +
                "Using quick portals: " + UseQuickPortals + "; " +
                "Using metrics: " + UseMetrics);

        // Now let's check portal data.
        File pFile = new File(getDataFolder(), "portals.yml");
        if (!pFile.exists()) {
            if (pFile.getParentFile().mkdirs()) {
                createConfigFile(getResource("portals.yml"), pFile);
                logger.log(Level.INFO, "Portals file created!");
            }
        }

        portalsFile = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "portals.yml"));
        logger.log(Level.INFO, "Portal data file loaded!");
    }

    void loadPortalsData() {
        try {
            long time = System.currentTimeMillis();
            for (String key : portalsFile.getKeys(false)) {
                String value = portalsFile.getString(key);
                portalData.put(key, value);
            }
            logger.log(Level.INFO, "Portal data loaded from disk! (" + (System.currentTimeMillis() - time) + "ms)");
        } catch (NullPointerException e) {
            logger.log(Level.WARNING, "Failed to load portal data!");
            e.printStackTrace();
        }
    }

    void savePortalsData() {
        long time = System.currentTimeMillis();
        YamlConfiguration portalFile = new YamlConfiguration();
        for (Entry<String, String> entry : portalData.entrySet()) {
            portalFile.set(entry.getKey(), entry.getValue());
        }
        try {
            portalFile.save(new File(getDataFolder(), "portals.yml"));
            logger.log(Level.INFO, "Portal data saved to disk! (" + (System.currentTimeMillis() - time) + "ms)");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to save portal data!");
            e.printStackTrace();
        }
    }

}
