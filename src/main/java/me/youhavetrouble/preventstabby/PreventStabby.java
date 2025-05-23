package me.youhavetrouble.preventstabby;

import me.youhavetrouble.preventstabby.commands.MainCommand;
import me.youhavetrouble.preventstabby.config.ConfigCache;
import me.youhavetrouble.preventstabby.hooks.PlaceholderApiHook;
import me.youhavetrouble.preventstabby.hooks.WorldGuardHook;
import me.youhavetrouble.preventstabby.data.PlayerListener;
import me.youhavetrouble.preventstabby.data.PlayerManager;
import me.youhavetrouble.preventstabby.listeners.EnvironmentalListener;
import me.youhavetrouble.preventstabby.listeners.PvpListener;
import me.youhavetrouble.preventstabby.listeners.UtilListener;
import me.youhavetrouble.preventstabby.util.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public final class PreventStabby extends JavaPlugin {

    private static PreventStabby plugin;
    private ConfigCache configCache;
    private PlayerManager playerManager;
    private DatabaseSQLite sqLite;
    private static boolean worldGuardHook;

    @Override
    public void onEnable() {
        plugin = this;
        reloadPluginConfig();
        File dbFile = new File("plugins/PreventStabby");
        sqLite = new DatabaseSQLite("jdbc:sqlite:plugins/PreventStabby/database.db", dbFile, getLogger());
        playerManager = new PlayerManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new UtilListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new EnvironmentalListener(this), this);

        getServer().getPluginManager().registerEvents(new PvpListener(this), this);

        // Register command
        PluginCommand pvpCommand = getCommand("pvp");
        if (pvpCommand == null) {
            getLogger().severe("Error with registering commands.");
            getLogger().severe("Plugin will now disable.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        MainCommand mainCommand = new MainCommand();
        pvpCommand.setExecutor(mainCommand);
        pvpCommand.setTabCompleter(mainCommand);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderApiHook(this).register();
        }

        Metrics metrics = new Metrics(this, 14074);

        getServer().getOnlinePlayers().forEach(player -> playerManager.getPlayerData(player.getUniqueId()));
        getServer().getWorlds().forEach(world -> {
            for (Chunk chunk : world.getLoadedChunks()) {
                if (!chunk.isEntitiesLoaded()) continue;
                Bukkit.getRegionScheduler().run(plugin, chunk.getWorld(), chunk.getX(), chunk.getZ(), (task)  -> {
                    for (Entity entity : chunk.getEntities()) {
                        if (!(entity instanceof Tameable tameable)) continue;
                        UUID ownerId = tameable.getOwnerUniqueId();
                        if (ownerId == null) continue;
                        getPlayerManager().getPlayerData(ownerId).thenAccept(playerData -> {
                            if (playerData == null) return;
                            playerData.addRelatedEntity(entity.getUniqueId());
                        });
                    }
                });
            }
        });
    }

    @Override
    public void onLoad() {
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            try {
                WorldGuardHook.init(this.getLogger());
                worldGuardHook = true;
            } catch (NoClassDefFoundError e) {
                worldGuardHook = false;
            }
        } else {
            worldGuardHook = false;
        }
    }

    public static boolean worldGuardHookEnabled() {
        return worldGuardHook;
    }

    public void reloadPluginConfig() {
        configCache = new ConfigCache(this);
    }

    public void reloadPluginConfig(CommandSender commandSender) {
        getServer().getAsyncScheduler().runNow(this, (task) -> {
            reloadPluginConfig();
            PluginMessages.sendMessage(commandSender, "PreventStabby configuration reloaded.");
        });
    }

    public ConfigCache getConfigCache() {
        return configCache;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public DatabaseSQLite getSqLite() {return sqLite;}

    public static PreventStabby getPlugin() {
        return plugin;
    }

}
