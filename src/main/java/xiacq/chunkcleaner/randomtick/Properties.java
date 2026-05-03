package xiacq.chunkcleaner.randomtick;

import org.bukkit.World;
import xiacq.chunkcleaner.core.utility.Pair;

import java.util.Set;

public class Properties {

    private final Pair<String> FROM_TO_BLOCKS;
    private final Set<World> APPLICABLE_WORLDS;
    private final double CHANCE;
    private final Pair<Integer> LIGHT_LEVELS;
    private final Pair<Integer> TIME;
    private final Pair<Integer> Y_LEVELS;




    public Properties(Pair<String> fromToBlocks,
                      Set<World> applicableWorlds,
                      double chance,
                      Pair<Integer> lightLevels,
                      Pair<Integer> time,
                      Pair<Integer> yLevels
    ) {
        this.FROM_TO_BLOCKS = fromToBlocks;
        this.APPLICABLE_WORLDS = applicableWorlds;
        this.CHANCE = chance;
        this.LIGHT_LEVELS = lightLevels;
        this.TIME = time;
        this.Y_LEVELS = yLevels;
    }

    public Pair<String> returnFromToBlocks() {return this.FROM_TO_BLOCKS;}
    public Set<World> returnApplicableWorlds() {return this.APPLICABLE_WORLDS;}
    public double returnChance() {return this.CHANCE;}
    public Pair<Integer> returnLightLevels() {return this.LIGHT_LEVELS;}
    public Pair<Integer> returnTime() {return this.TIME;}
    public Pair<Integer> returnYLevels() {return this.Y_LEVELS;}

}
