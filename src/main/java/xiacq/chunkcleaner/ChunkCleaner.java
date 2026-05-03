package xiacq.chunkcleaner;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import xiacq.chunkcleaner.core.commands.ChunkInfoCommand;
import xiacq.chunkcleaner.core.commands.DeleteChunksCommand;
import xiacq.chunkcleaner.core.commands.RandomTickCommand;
import xiacq.chunkcleaner.core.utility.Deletion;
import xiacq.chunkcleaner.listener.ChunkListener;
import xiacq.chunkcleaner.randomtick.RandomTick;
import xiacq.chunkcleaner.randomtick.RandomTickFile;

public class ChunkCleaner extends JavaPlugin {
    private static ChunkCleaner instance;
    public static String PREFIX;
    private RandomTick RANDOM_TICK;

    @Override
    public void onEnable() {
        instance = this;
        Bukkit.getLogger().info("---------------{ChunkCleaner}---------------");
        Bukkit.getLogger().info("> Starting please wait!");
        registerCommands();
        setDefaultConfig();
        RandomTickFile.initRandomTickFile(); //Random Tick Init
        loadValues();
        loadListeners();
        this.RANDOM_TICK = new RandomTick();

        if(!this.getServer().getPluginManager().getPlugin("ItemsAdder").isEnabled()) {
            Bukkit.getLogger().severe("> Items adder not found stopping!");
            Bukkit.getPluginManager().disablePlugin(this);
        } else
            Bukkit.getLogger().info("> Items adder Found!");


        Bukkit.getLogger().info("> All set!");
        Bukkit.getLogger().info("--------------------------------------------");
    }

    @Override
    public void onDisable() {
        //REMOVING captured new chunks.
        Bukkit.getLogger().info("---------------{ChunkCleaner}---------------");
        Bukkit.getLogger().info("> Shutting down please wait!");
        new Deletion(null, null).startDeletionSinge();

        if(this.RANDOM_TICK != null)
            this.RANDOM_TICK.setShutDown(true);

        Bukkit.getLogger().info("> All done!");
        Bukkit.getLogger().info("--------------------------------------------");

    }


    private void registerCommands() {
        instance.getCommand("ChunkInfo").setExecutor(new ChunkInfoCommand());
        instance.getCommand("DeleteChunks").setExecutor(new DeleteChunksCommand());
        instance.getCommand("Randomtick").setExecutor(new RandomTickCommand());
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

    public RandomTick returnRandomTick() {return this.RANDOM_TICK;}
    public void updateRandomTick(RandomTick randomTick) {this.RANDOM_TICK = randomTick;}
}
