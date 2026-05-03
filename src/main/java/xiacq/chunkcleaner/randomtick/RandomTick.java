package xiacq.chunkcleaner.randomtick;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.ItemsAdder;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xiacq.chunkcleaner.ChunkCleaner;
import xiacq.chunkcleaner.core.utility.Pair;

import java.util.*;

public class RandomTick {

    private static boolean shutDown = false;
    private final int INTERVAL = RandomTickFile.getRandomTickConfiguration().getInt("interval");
    private final int SLOW_INTERVAL = RandomTickFile.getRandomTickConfiguration().getInt("slowInterval");
    private final double SLOW_DOWN_POINT = RandomTickFile.getRandomTickConfiguration().getDouble("pointOfSlowDown");


    public RandomTick() {
            if(RandomTickFile.getRandomTickConfiguration() == null) {
                ChunkCleaner.getInstance().getLogger().severe(ChunkCleaner.PREFIX + "Init of the RandomTick has failed due to the RandomTickFile.yml!");
                return;
            }

            if(RandomTickProfiles.PROFILES.isEmpty()) {
                ChunkCleaner.getInstance().getLogger().severe(ChunkCleaner.PREFIX + "Init of the RandomTick has failed, no profiles could be loaded!");
                return;
            }

            ChunkCleaner.getInstance().getLogger().info("> Init of the RandomTick is done! Loaded a total of " + RandomTickProfiles.PROFILES.size() + " profiles.");

            startRandomTicks();
    }

    double returnRecentTps() {
        try {
            return MinecraftServer.getServer().recentTps[1];
        } catch (NoSuchFieldError error) {
            return 20.0;
        }
    }

    private void startRandomTicks() {
        int preferredInterval;
        if(!RandomTickFile.getRandomTickConfiguration().getBoolean("forceMax")) {
            if(returnRecentTps() <= SLOW_DOWN_POINT)
                preferredInterval = SLOW_INTERVAL;
            else
                preferredInterval = INTERVAL;
        } else
            preferredInterval = INTERVAL;



        new BukkitRunnable() {


            @Override
            public void run() {

                //Firstly We take our World for this check;
                World checking = (World) RandomTickProfiles.PROFILES.keySet().toArray()[new Random().nextInt(0, RandomTickProfiles.PROFILES.size())];
                //Collect the chunks;
                Chunk[] chunks = checking.getLoadedChunks();

                if (chunks.length > 0) {
                   // for (int i = 0; i < 1; i++) {
                        for (Player player : Bukkit.getOnlinePlayers())
                            Bukkit.getScheduler().runTaskAsynchronously(ChunkCleaner.getInstance(), () -> {
                                checkChunk(player.getLocation().getChunk());
                            });
                        //checkChunk(chunks[new Random().nextInt(0, chunks.length-1)]);
                   // }
                }


                if((INTERVAL == preferredInterval && returnRecentTps() < SLOW_DOWN_POINT)
                        || (SLOW_INTERVAL == preferredInterval && returnRecentTps() > SLOW_DOWN_POINT)) {
                    ChunkCleaner.getInstance().getLogger().info(ChunkCleaner.PREFIX + "Restarting Random Tick!");
                    startRandomTicks();
                    cancel();
                }

                if(shutDown)
                    cancel();

            }
        }.runTaskTimerAsynchronously(ChunkCleaner.getInstance(), 0L, preferredInterval);

    }

    public void checkChunk(Chunk chunk) {
        World world = chunk.getWorld();
        //Check Suitable Blocks
        Set<String> usableBlocks = new HashSet<>();
        int topY = world.getMinHeight();
        int floorY = world.getMaxHeight();
        for(RandomTickProfiles profile : RandomTickProfiles.PROFILES.get(world)) {
            String possibleCheck = profile.getProperties().returnFromToBlocks().getKey();
            if (usableBlocks.isEmpty() || !usableBlocks.contains(possibleCheck))
                usableBlocks.add(possibleCheck);

            int thisFloorY = profile.getProperties().returnYLevels().getKey();

            if(floorY > thisFloorY)
                floorY = thisFloorY;

            int thisTopY = profile.getProperties().returnYLevels().getValue();

            if(topY < thisTopY)
                topY = thisTopY;
        }
        //Check The Chunk for that block

        for(int y = floorY; y < topY; y++)
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++) {
                    Block chosenOne = world.getBlockAt((chunk.getX() << 4) + x, y, (chunk.getZ() << 4) + z);
                    if (usableBlocks.contains(kindOfBlock(chosenOne)))
                        for(RandomTickProfiles profiles : RandomTickProfiles.PROFILES.get(world))
                            if(profiles.getProperties().returnFromToBlocks().getKey().equalsIgnoreCase(kindOfBlock(chosenOne))
                                    && hasCorrectPositions(profiles, getCorrectPositions(profiles, chosenOne))
                                    && hasCorrectProperties(profiles, chosenOne)) {
                                resultProfile(profiles, chosenOne);
                            }

                }
    }

    private String kindOfBlock(Block block) {
        CustomBlock possibleCustom = CustomBlock.byAlreadyPlaced(block);
        return possibleCustom == null ? block.getType().name() : possibleCustom.getNamespacedID().toUpperCase();
    }

    private boolean hasCorrectProperties(RandomTickProfiles profile, Block chosenBlock) {
        byte lightLevel = chosenBlock.getRelative(BlockFace.UP).getLightLevel();
        if((lightLevel <= profile.getProperties().returnLightLevels().getValue() || profile.getProperties().returnLightLevels().getValue() == -1)
                && (lightLevel >= profile.getProperties().returnLightLevels().getKey() || profile.getProperties().returnLightLevels().getKey() == -1)) {
            long timeInWorld = chosenBlock.getWorld().getTime();
            return ((timeInWorld <= profile.getProperties().returnTime().getValue() || profile.getProperties().returnTime().getValue() == -1)
                    && timeInWorld >= profile.getProperties().returnTime().getKey() || profile.getProperties().returnTime().getKey() == -1);
        }
        return false;
    }


    private void replace(Pair<String> fromTo, Block chosenOne) {
        CustomBlock placedCustom = CustomBlock.byAlreadyPlaced(chosenOne);
        if(placedCustom != null)
            placedCustom.remove();

        if(fromTo.getValue().contains("ITEMSADDER:")) {
            chosenOne.setType(Material.AIR);
            CustomBlock.place(fromTo.getValue().toLowerCase(), chosenOne.getLocation());
        } else
            chosenOne.setType(
                    Material.getMaterial(
                            fromTo.getValue()
                    )
            );
    }

    private void resultProfile(RandomTickProfiles randomTickProfiles, Block chosenBlock) {
        Bukkit.getScheduler().runTask(ChunkCleaner.getInstance(), () -> {
            //is it a rng win?
            if(randomTickProfiles.getProperties().returnChance() < new Random().nextDouble(0.0, 100.0))
                return; //guess it's not
            //Replace the block II
            // and check if IA :(
            replace(randomTickProfiles.getProperties().returnFromToBlocks(), chosenBlock);


            //Use  Results III
            for(Result result : randomTickProfiles.getResult()) {
                    Block[] inFill = returnComposition(chosenBlock, transformFromString(result.getPosition()));
                    if(result.getFillingRate() == 100)
                        for(Block toBeFilled : inFill)
                            replace(new Pair<>(kindOfBlock(toBeFilled), result.getResultMaterial()), toBeFilled);

                    else {
                        //We gonna fill the percentage otherwise.
                        if(result.getFillingRate() > 0) {
                            List<Block> blocks = new ArrayList<>(Arrays.stream(returnComposition(chosenBlock, transformFromString(result.getPosition()))).toList());
                            int toChange = returnFillToBlocks(blocks.size(), result.getFillingRate());

                            while (toChange > 0) {
                                Block random;
                                if (toChange > 0) {
                                    random = blocks.get(new Random().nextInt(blocks.size() - 1));
                                } else
                                    random = blocks.getFirst();
                                blocks.remove(random);
                                // random.setType(Material.getMaterial(result.getResultMaterial()));
                                replace(new Pair<>(kindOfBlock(random), result.getResultMaterial()), random);
                                toChange--;
                            }
                        }
                    }
            }


            //Use commands Provided.

            if(!randomTickProfiles.getCommands().isEmpty() && !randomTickProfiles.getCommands().getFirst().equalsIgnoreCase("none")) {
                for(String command : randomTickProfiles.getCommands()) {
                    StringBuilder stringBuilder = new StringBuilder();

                    if(command.contains("_x") || command.contains("_y") || command.contains("_z")) {
                        String[] split = command.split(" ");
                        stringBuilder.append(split[0]).append(" ");
                        for(int i = 1; i < split.length; i++) {
                            String currentSplit = split[i];
                            char contains;

                            if(currentSplit.contains("_x")) {
                                contains = 'x';
                            } else if(currentSplit.contains("_y")) {
                                contains = 'y';
                            } else if(currentSplit.contains("_z")) {
                                contains = 'z';
                            } else {
                                stringBuilder.append(currentSplit).append(" ");
                                continue;
                            }

                            int val = switch (contains) {
                                case 'x' -> chosenBlock.getX();
                                case 'y' -> chosenBlock.getY();
                                case 'z' -> chosenBlock.getZ();
                                default -> throw new IllegalStateException("Unexpected value: " + contains);
                            };

                            StringBuilder number = new StringBuilder();
                            int startPoint;
                            if(currentSplit.contains("+")) {
                                startPoint = currentSplit.indexOf('+');
                            } else if(currentSplit.contains("-")) {
                                startPoint = currentSplit.indexOf('-');
                            } else {
                                stringBuilder.append(val).append(" ");
                                continue;

                            }
                            startPoint++; // skipping the + or -
                            while(startPoint < currentSplit.length()) {
                                number.append(currentSplit.charAt(startPoint));
                                startPoint++;
                            }

                            val = (currentSplit.contains("+")) ?
                                    val + Integer.parseInt(number.toString()) : val - Integer.parseInt(number.toString());
                            stringBuilder.append(val).append(" ");
                        }
                    } else
                        stringBuilder.append(command);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.valueOf(stringBuilder));
                }
            }
        });

    }

    private int returnFillToBlocks(int blocks, int fill) {
        return  blocks > 1 ? switch (fill) {
            case 100 -> 4;
            case 75 -> 3;
            case 50 -> 2;
            case 25 -> 1;
            default -> 0;
        } : 1;
    }

    public void setShutDown(boolean state) {
        shutDown = state;
    }

    private List<String> getCorrectPositions(RandomTickProfiles randomTickProfile, Block checkingBlock) {
        List<String> correctPositionList = new ArrayList<>();
        for(Goal currentGoal : randomTickProfile.getGoal()) {
            for(String position : currentGoal.returnPositions()) {
                int correctBlocks = 0;
                Block[] blockPositionsToCheck = returnComposition(checkingBlock, transformFromString(position));
                for(Block block : blockPositionsToCheck)
                    if(block.getType().name().equals(currentGoal.returnReplacement()))
                        correctBlocks++;
                if(correctBlocks == blockPositionsToCheck.length)
                    correctPositionList.add(position);
            }
        }
        return correctPositionList;
    }
    private boolean hasCorrectPositions(RandomTickProfiles randomTickProfile, List<String> correctPositons) {
        int correctGoals = 0;

        for(Goal goal : randomTickProfile.getGoal()) {
            int correctPositionss = 0;
            for (String positions : goal.returnPositions())
                if (correctPositons.contains(positions) && goal.returnRequired())
                    correctPositionss++;

            if(correctPositionss == goal.returnPositions().size() && goal.returnRequiredAll())
                correctGoals++;
            else if(correctPositionss > 0 && !goal.returnRequiredAll())
                correctGoals++;
        }                      //Required Goals;
        return correctGoals >= randomTickProfile.getGoal().size();
    }
    private int transformFromString(String key) {
        return switch (key.toUpperCase()) {
            case "A" -> 1;
            case "B" -> 2;
            case "C" -> 3;
            case "D" -> 4;
            case "E" -> 5;
            case "F" -> 6;
            case "G" -> 7;
            case "H" -> 8;
            default -> 0;
        };
    }
    private Block[] returnComposition(Block mainBlock, int position) {
        return switch (position) {
            case 1 -> new Block[] {
                    mainBlock.getRelative(1, 0, 0),  // East
                    mainBlock.getRelative(-1, 0, 0), // West
                    mainBlock.getRelative(0, 0, 1),  // South
                    mainBlock.getRelative(0, 0, -1)  // North
            };
            case 2 -> new Block[] {mainBlock.getRelative(0, 1, 0)};  // Above
            case 3 -> new Block[] {mainBlock.getRelative(0, -1, 0)}; // Below
            case 4 -> new Block[] {
                    mainBlock.getRelative(1, 0, 1),   // Southeast
                    mainBlock.getRelative(-1, 0, 1),  // Southwest
                    mainBlock.getRelative(-1, 0, -1), // Northwest
                    mainBlock.getRelative(1, 0, -1),  // Northeast
            };
            case 5 -> new Block[] {
                    mainBlock.getRelative(1, 1, 0),  // Above East
                    mainBlock.getRelative(-1, 1, 0), // Above West
                    mainBlock.getRelative(0, 1, 1),  // Above South
                    mainBlock.getRelative(0, 1, -1)  // Above North
            };
            case 6 -> new Block[] {
                    mainBlock.getRelative(1, -1, 0),  // Below East
                    mainBlock.getRelative(-1, -1, 0), // Below West
                    mainBlock.getRelative(0, -1, 1),  // Below South
                    mainBlock.getRelative(0, -1, -1)  // Below North
            };
            case 7 -> new Block[] {
                    mainBlock.getRelative(1, 1, 1),   // Above Southeast
                    mainBlock.getRelative(-1, 1, 1),  // Above Southwest
                    mainBlock.getRelative(-1, 1, -1), // Above Northwest
                    mainBlock.getRelative(1, 1, -1),  // Above Northeast
            };
            case 8 -> new Block[] {
                    mainBlock.getRelative(1, -1, 1),   // Below Southeast
                    mainBlock.getRelative(-1, -1, 1),  // Below Southwest
                    mainBlock.getRelative(-1, -1, -1), // Below Northwest
                    mainBlock.getRelative(1, -1, -1),  // Below Northeast
            };
            default -> throw new IllegalStateException("Unexpected value: " + position);
        };
    }
}
