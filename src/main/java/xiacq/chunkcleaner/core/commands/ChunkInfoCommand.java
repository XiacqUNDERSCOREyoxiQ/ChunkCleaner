package xiacq.chunkcleaner.core.commands;


import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import xiacq.chunkcleaner.ChunkCleaner;

public class ChunkInfoCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull[] args) {
        if(sender instanceof Player player && player.hasPermission("CC.ChunkInfo"))
            if(args.length == 0)
                printChunkInfo(player);
             else
                player.sendMessage(ChunkCleaner.PREFIX + "This command does not support arguments!");
         else
            sender.sendMessage(ChunkCleaner.PREFIX + "You don't have the required permissions to execute this command!");
        return false;
    }
    private void printChunkInfo(Player player) {
        player.sendMessage("----------------{§5ChunkCleaner§f}---------------");
        player.sendMessage("§5>§r Chunk InhabitedTime " + player.getLocation().getChunk().getInhabitedTime()/20 + " §8| §7§o(Side note this time is only a rough estimate not the actual nbt data that is used for removal)") ;
        player.sendMessage("§5>§r HasTileEntities " + (player.getLocation().getChunk().getTileEntities().length > 0));
        player.sendMessage("§5>§r Keep ? " + (player.getLocation().getChunk().getTileEntities().length > 0 || player.getLocation().getChunk().getInhabitedTime()/20 >= ChunkCleaner.getInstance().getConfig().getInt("inhibitedTime")));
        player.sendMessage("---------------------------------------------");
    }
}
