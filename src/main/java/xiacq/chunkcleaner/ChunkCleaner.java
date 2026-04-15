package xiacq.chunkcleaner;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import xiacq.chunkcleaner.core.commands.ChunkInfoCommand;
import xiacq.chunkcleaner.core.commands.DeleteChunksCommand;
import xiacq.chunkcleaner.core.utility.Deletion;
import xiacq.chunkcleaner.listener.ChunkListener;

public class ChunkCleaner extends JavaPlugin {
    private static ChunkCleaner instance;
    public static String PREFIX;

    @Override
    public void onEnable() {
        instance = this;
        Bukkit.getLogger().info("---------------{ChunkCleaner}---------------");
        Bukkit.getLogger().info("> Starting please wait!");
        registerCommands();
        setDefaultConfig();
        loadValues();
        loadListeners();
        Bukkit.getLogger().info("> All set!");
        Bukkit.getLogger().info("--------------------------------------------");
    }

    @Override
    public void onDisable() {
        //REMOVING captured new chunks.
        Bukkit.getLogger().info("---------------{ChunkCleaner}---------------");
        Bukkit.getLogger().info("> Shutting down please wait!");
        new Deletion(null, null).startDeletionSinge();
        Bukkit.getLogger().info("> All done!");
        Bukkit.getLogger().info("--------------------------------------------");

    }


    private void registerCommands() {
        instance.getCommand("ChunkInfo").setExecutor(new ChunkInfoCommand());
        instance.getCommand("DeleteChunks").setExecutor(new DeleteChunksCommand());
    }

    private void loadListeners() {
        if(instance.getConfig().getBoolean("chunkLoadDeletion")) {
            Bukkit.getPluginManager().registerEvents(new ChunkListener(), instance);
            Bukkit.getLogger().info("> Chunk deletion via load events active!");
        }
    }

    private void setDefaultConfig() {
        FileConfiguration fileConfiguration = instance.getConfig();
        fileConfiguration.addDefault("prefix", "§5CC §8| §f");
        fileConfiguration.addDefault("inhibitedTime", 60);
        fileConfiguration.addDefault("chunkLoadDeletion", true);
        fileConfiguration.addDefault("extraEvents", true);

        fileConfiguration.options().copyDefaults(true);
        instance.saveConfig();
    }

    private void loadValues() {
        PREFIX = instance.getConfig().getString("prefix");
    }

    public static ChunkCleaner getInstance() {return instance;}
}
