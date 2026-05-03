package xiacq.chunkcleaner.randomtick;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xiacq.chunkcleaner.ChunkCleaner;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RandomTickFile {
    private static File file;
    private static FileConfiguration fileConfiguration;

    public static void initRandomTickFile() {
        try {
            file = new File(ChunkCleaner.getInstance().getDataFolder(), "RandomTickFile.yml");
            if(!file.exists())
                file.createNewFile();
            fileConfiguration = YamlConfiguration.loadConfiguration(file);
            setDefaults();
        } catch (IOException ioException) {ChunkCleaner.getInstance().getLogger().severe(ChunkCleaner.PREFIX + "Could not create the RandomTickFile.yml!");}
        RandomTickProfiles.loadProfiles();

    }

    private static void setDefaults() {
        String prefixForExample = "Random Ticks.example";

        fileConfiguration.addDefault("isEnabled", true);
        fileConfiguration.addDefault("minChunkCheckPerInterval", 1);
        fileConfiguration.addDefault("maxChunkCheckPerInterval", 20);
        fileConfiguration.addDefault("forceMax", true);
        fileConfiguration.addDefault("interval", 20);
        fileConfiguration.addDefault("slowInterval", 40);
        fileConfiguration.addDefault("pointOfSlowDown", 10.0);


        fileConfiguration.addDefault(prefixForExample + ".isEnabled", true);
        fileConfiguration.addDefault(prefixForExample + ".searchForBlock", Material.IRON_BLOCK.toString());
        fileConfiguration.addDefault(prefixForExample + ".replaceWithBlock", Material.NETHERITE_BLOCK.toString());
        fileConfiguration.addDefault(prefixForExample + ".applicableWorlds", List.of("world", "world_nether", "world_the_end"));
        fileConfiguration.addDefault(prefixForExample + ".chance", 100.0);
        fileConfiguration.addDefault(prefixForExample + ".minY", 10);
        fileConfiguration.addDefault(prefixForExample + ".maxY", 100);
        fileConfiguration.addDefault(prefixForExample + ".fromTime", -1);
        fileConfiguration.addDefault(prefixForExample + ".toTime", -1);
        fileConfiguration.addDefault(prefixForExample + ".fromLight", -1);
        fileConfiguration.addDefault(prefixForExample + ".toLight", 15);
        fileConfiguration.addDefault(prefixForExample + ".executionOfCommands", List.of("say a", "say +x +y +z"));
        fileConfiguration.addDefault(prefixForExample + ".exampleGoal.positions", List.of("A", "B"));
        fileConfiguration.addDefault(prefixForExample + ".exampleGoal.required", true);
        fileConfiguration.addDefault(prefixForExample + ".exampleGoal.requiredAll", false);
        fileConfiguration.addDefault(prefixForExample + ".exampleGoal.replacement", Material.AIR.toString());
        fileConfiguration.addDefault(prefixForExample + ".Result.B.supply", Material.AIR.toString());
        fileConfiguration.addDefault(prefixForExample + ".Result.B.fill", 100);


        fileConfiguration.options().copyDefaults(false);
        saveRandomTickFile();
    }

    private static void saveRandomTickFile() {
        try {
           fileConfiguration.save(file);
        } catch (IOException exception) {ChunkCleaner.getInstance().getLogger().severe(ChunkCleaner.PREFIX + "Could not save the RandomTickFile.yml!");}
        reloadRandomTickConfiguration();
    }

    public static FileConfiguration getRandomTickConfiguration() {return fileConfiguration;}
    public static void reloadRandomTickConfiguration() {fileConfiguration = YamlConfiguration.loadConfiguration(file);}

}
