package xiacq.chunkcleaner;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import xiacq.chunkcleaner.core.commands.ChunkInfoCommand;
import xiacq.chunkcleaner.core.commands.DeleteChunksCommand;

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
        Bukkit.getLogger().info("> All set!");
        Bukkit.getLogger().info("--------------------------------------------");
    }

    @Override
    public void onDisable() {

    }


    private void registerCommands() {
        instance.getCommand("ChunkInfo").setExecutor(new ChunkInfoCommand());
        instance.getCommand("DeleteChunks").setExecutor(new DeleteChunksCommand());
    }

    private void setDefaultConfig() {
        FileConfiguration fileConfiguration = instance.getConfig();
        fileConfiguration.addDefault("prefix", "§5CC §8| §f");
        fileConfiguration.options().copyDefaults(true);
        instance.saveConfig();
    }

    private void loadValues() {
        PREFIX = instance.getConfig().getString("prefix");
    }

    public static ChunkCleaner getInstance() {return instance;}
}
