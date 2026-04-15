package xiacq.chunkcleaner.listener;

import org.bukkit.Chunk;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import xiacq.chunkcleaner.ChunkCleaner;

import java.util.HashMap;
import java.util.Map;

public class ChunkListener implements Listener {
    public static final Map<Chunk, Boolean> CHUNKS_TO_LOCK_AT = new HashMap<>();
    private final boolean EXTRA_EVENTS = ChunkCleaner.getInstance().getConfig().getBoolean("extraEvents");
    //CHUNK EVENTS
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if(event.isNewChunk()) //Save the chunk to be deleted if nothing interupts.
            CHUNKS_TO_LOCK_AT.put(event.getChunk(), false);
    }
    //Marking Chunk as plz do not dispose if not attacked by DeleteChunks && if enabled
    //handles
    private void handleSaved(Chunk chunk) {
        if(!CHUNKS_TO_LOCK_AT.isEmpty() && CHUNKS_TO_LOCK_AT.containsKey(chunk))
            CHUNKS_TO_LOCK_AT.put(chunk, true);
    }
    //HANDLE EVENTS
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
         handleSaved(event.getBlock().getChunk());
    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
          handleSaved(event.getBlock().getChunk());
    }
    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if(EXTRA_EVENTS)
            handleSaved(event.getEntity().getLocation().getChunk());
    }
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if(EXTRA_EVENTS
                && !event.getAction().equals(Action.LEFT_CLICK_AIR)
                && !event.getAction().equals(Action.RIGHT_CLICK_AIR))
            handleSaved(event.getPlayer().getLocation().getChunk());
    }
}
