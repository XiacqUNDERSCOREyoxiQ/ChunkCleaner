package xiacq.chunkcleaner.core.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;
import xiacq.chunkcleaner.ChunkCleaner;
import xiacq.chunkcleaner.core.utility.Deletion;

public class DeleteChunksCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if(sender.hasPermission("CC.DeleteChunks")) {
            if (args.length == 1 || args.length == 2) {
                if(!containsWorld(args[0])) {
                    sender.sendMessage(ChunkCleaner.PREFIX + "The world §d§l" + args[0] + "§f does not seem to exist.");
                    return true;
                }

                World world = Bukkit.getWorld(args[0]);

                if(args.length == 1)
                    startDeletion(sender, world, false);
                else
                    if(args[1].equalsIgnoreCase("new"))
                        startDeletion(sender, world, false);
                    else if(args[1].equalsIgnoreCase("resume"))
                        startDeletion(sender, world, true);
                    else
                        sender.sendMessage(ChunkCleaner.PREFIX + "The argument you've provided is not available | Please chose either new or resume");
            } else
                sender.sendMessage(ChunkCleaner.PREFIX + "The number of provided arguments is incorrect. | Usage : /DeleteChunks {WorldName} {New or Resume}");
        } else
            sender.sendMessage(ChunkCleaner.PREFIX + "You don't have the required permission to execute this command!");
        return false;
    }
    private boolean containsWorld(String worldName) {
        for(World worlds : Bukkit.getWorlds())
            if(worlds.getName().equals(worldName))
                return true;
        return false;
    }
    private void startDeletion(CommandSender sender, World world, boolean resume) {new Deletion(sender, world).startDeletion(resume);}
}
