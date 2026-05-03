package xiacq.chunkcleaner.randomtick;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import xiacq.chunkcleaner.core.utility.Pair;

import java.util.*;

public class RandomTickProfiles {

    private final Properties PROPERTIES;
    private final List<Goal> GOALS;
    private final List<Result> RESULTS;
    public final static HashMap<World, Set<RandomTickProfiles>> PROFILES = new HashMap<>();
    private final List<String> COMMANDS;


    public RandomTickProfiles(boolean isEnabled, Properties properties, List<Goal> goals, List<Result> results, List<String> commands) {
        this.PROPERTIES = properties;
        this.GOALS = goals;
        this.RESULTS = results;
        this.COMMANDS = commands;

        for(World worlds : this.PROPERTIES.returnApplicableWorlds()) {
            if(isEnabled)
                if(PROFILES.isEmpty() || !PROFILES.containsKey(worlds))
                    PROFILES.put(worlds, Set.of(this));
                else {
                    PROFILES.get(worlds).add(this);
                }
        }
    }

    public Properties getProperties() {return this.PROPERTIES;}
    public List<Goal> getGoal() {return this.GOALS;}
    public List<Result> getResult() {return this.RESULTS;}
    public List<String> getCommands() {return this.COMMANDS;}

    public static void loadProfiles() {
        FileConfiguration fileConfiguration = RandomTickFile.getRandomTickConfiguration();
        //Load Profiles
        for(String profileKeys : fileConfiguration.getConfigurationSection("Random Ticks").getKeys(false)) {
            new RandomTickProfiles(
                    fileConfiguration.getBoolean("Random Ticks." + profileKeys + ".isEnabled"),
                    loadPropertiesOfProfile("Random Ticks." +  profileKeys, fileConfiguration),
                    loadGoalsOfProfile("Random Ticks." + profileKeys, fileConfiguration),
                    loadResultsOfProfile("Random Ticks." + profileKeys, fileConfiguration),
                    fileConfiguration.getStringList("Random Ticks." + profileKeys + ".executionOfCommands")
            );
        }


    }

    private static List<Goal> loadGoalsOfProfile(String keyToGoals, FileConfiguration fileConfiguration) {
        List<Goal> goals = new ArrayList<>();
        for (String keys : Objects.requireNonNull(fileConfiguration.getConfigurationSection(keyToGoals)).getKeys(false))
            if(keys.toLowerCase().contains("goal")) {
                String tempPath = keyToGoals + "." + keys + ".";
                goals.add(
                        new Goal(
                                fileConfiguration.getStringList(tempPath + "positions"),
                                fileConfiguration.getBoolean(tempPath + "required"),
                                fileConfiguration.getBoolean(tempPath + "requiredAll"),
                                fileConfiguration.getString(tempPath + "replacement")
                        )
                    );
                }
        return goals;
    }

    private static Properties loadPropertiesOfProfile(String toProfiles, FileConfiguration fileConfiguration) {
            return new Properties(
                    new Pair<>(fileConfiguration.getString(toProfiles + ".searchForBlock"),fileConfiguration.getString(toProfiles + ".replaceWithBlock")),
                    loadDimensionsFromList(fileConfiguration.getStringList(toProfiles + ".applicableWorlds")),
                    fileConfiguration.getDouble(toProfiles + ".chance"),
                    new Pair<>(fileConfiguration.getInt(toProfiles + ".fromLight"), fileConfiguration.getInt(toProfiles + ".toLight")),
                    new Pair<>(fileConfiguration.getInt(toProfiles + ".fromTime"), fileConfiguration.getInt(toProfiles + ".toTime")),
                    new Pair<>(fileConfiguration.getInt(toProfiles + ".minY"), fileConfiguration.getInt(toProfiles + ".maxY"))
            );
    }

    private static Set<World> loadDimensionsFromList(List<String> stringList) {
        Set<World> returningSet = new HashSet<>();
        for(String dimName : stringList)
            returningSet.add(Bukkit.getWorld(dimName));
        return returningSet;
    }

    private static List<Result> loadResultsOfProfile(String toProfile, FileConfiguration fileConfiguration) {
        List<Result> results = new ArrayList<>();
        for(String subKeys : fileConfiguration.getConfigurationSection(toProfile + ".Result").getKeys(false))
            results.add(new Result(
                    subKeys.toUpperCase(),
                    fileConfiguration.getString(toProfile + ".Result." + subKeys + ".supply"),
                    fileConfiguration.getInt(toProfile + ".Result." + subKeys + ".fill")
            ));
        return results;
    }
}
