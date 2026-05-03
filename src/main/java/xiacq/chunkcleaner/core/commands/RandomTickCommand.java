package xiacq.chunkcleaner.core.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import xiacq.chunkcleaner.ChunkCleaner;
import xiacq.chunkcleaner.randomtick.RandomTick;
import xiacq.chunkcleaner.randomtick.RandomTickFile;
import xiacq.chunkcleaner.randomtick.RandomTickProfiles;

public class RandomTickCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {

        if(sender.isOp()) {
            if(args.length == 0) {
                sender.sendMessage("-------------§5{§dRandomTickConfig§5}-------------");
                sender.sendMessage("§d> §5Atleast Chunks per Interval: §d" + RandomTickFile.getRandomTickConfiguration().getInt("minChunkCheckPerInterval"));
                sender.sendMessage("§d> §5Max Chunks per Interval: §d" + RandomTickFile.getRandomTickConfiguration().getInt("maxChunkCheckPerInterval"));
                sender.sendMessage("§d> §5Enforce Max always: §d" + RandomTickFile.getRandomTickConfiguration().getBoolean("forceMax"));
                sender.sendMessage("§d> §5Interval: §d" + RandomTickFile.getRandomTickConfiguration().getInt("interval"));
                sender.sendMessage("§d> §5Slow interval: §d" + RandomTickFile.getRandomTickConfiguration().getInt("slowInterval"));
                sender.sendMessage("§d> §5Point of Slowdown: §d" + RandomTickFile.getRandomTickConfiguration().getDouble("pointOfSlowDown"));
                sender.sendMessage("§d> §5Is running? : §d" + RandomTickFile.getRandomTickConfiguration().getInt("isEnabled"));
                sender.sendMessage("---------------------------------------------");

            } else if(args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                ChunkCleaner.getInstance().returnRandomTick().setShutDown(true);
                RandomTickFile.reloadRandomTickConfiguration();
                RandomTickProfiles.PROFILES.clear();
                RandomTickFile.initRandomTickFile();
                ChunkCleaner.getInstance().updateRandomTick(new RandomTick());
                sender.sendMessage(ChunkCleaner.PREFIX + "Reloaded!");
            } else
                sender.sendMessage(ChunkCleaner.PREFIX+ "Usage unsupported!");

        } else
            sender.sendMessage(ChunkCleaner.PREFIX + "You don't have the required permission to execute this command!");

        return false;
    }
}
