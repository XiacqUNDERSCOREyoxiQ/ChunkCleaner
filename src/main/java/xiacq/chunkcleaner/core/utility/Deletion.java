package xiacq.chunkcleaner.core.utility;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import xiacq.chunkcleaner.ChunkCleaner;
import xiacq.chunkcleaner.listener.ChunkListener;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.InflaterInputStream;

public class Deletion {

    private final File MANIFEST_FILE;
    private final CommandSender USER;
    private final World WORLD;
    private final int TO_CHECK_FOR_TIME = ChunkCleaner.getInstance().getConfig().getInt("inhibited_time");


    public Deletion(CommandSender sender, World world) {
        this.MANIFEST_FILE = new File(ChunkCleaner.getInstance().getDataFolder(), "manifest_of_CC.txt");
        this.WORLD = world;
        this.USER = sender;
    }

    public void startDeletionSinge() {
        List<File> delete = new ArrayList<>();
        for(Chunk chunks : ChunkListener.CHUNKS_TO_LOCK_AT.keySet()) {

            if(ChunkListener.CHUNKS_TO_LOCK_AT.get(chunks))
                continue;

            for (Entity entities : chunks.getEntities())
                if (entities instanceof Player
                        || entities.getCustomName() != null
                        || entities instanceof ItemFrame
                        || (entities instanceof Tameable tameable && tameable.isTamed())
                        || entities instanceof Minecart)
                    continue; //Cancel it because of the
                else
                    entities.remove();

            int chunkX = chunks.getX();
            int chunkZ = chunks.getZ();

            File regionFolder = getRegionFolder(chunks.getWorld());
            int regionX = Math.floorDiv(chunkX, 32);
            int regionZ = Math.floorDiv(chunkZ, 32);

            File regionFile = new File(regionFolder, "r." + regionX + "." + regionZ + ".mca");

            if (!regionFile.exists()) {
                ChunkCleaner.getInstance().getLogger().severe("There was an error during automatic chunk deletion! | The file was not found.");
                continue;
            }

            if(delete.isEmpty() || !delete.contains(regionFile))
                delete.add(regionFile);
        }
        for(File file : delete)
            processFile(file);
    }


    public void startDeletion(boolean tryToResume) {
        List<File> regionFilesFromManifest = new ArrayList<>();
        if(tryToResume) { //Check for resume
            if (!MANIFEST_FILE.exists()) {
                USER.sendMessage(ChunkCleaner.PREFIX + "The file to resume from is missing meaning either deleted or never there in the first place.");
                return;
            } else {
                //Try reading it
                try (BufferedReader reader = new BufferedReader(new FileReader(MANIFEST_FILE))) {
                    String currentLine;
                    while ((currentLine = reader.readLine()) != null) {
                        if (currentLine.trim().isEmpty())
                            continue; //SKIP
                        File file = new File(currentLine);
                        if (file.exists())
                            regionFilesFromManifest.add(file);
                        else
                            ChunkCleaner.getInstance().getLogger().severe("Couldn't find the region file from: " + currentLine);
                    }
                } catch (IOException ioException) {
                    USER.sendMessage(ChunkCleaner.PREFIX + "There was an error while reading the manifest please see the log!");
                    ChunkCleaner.getInstance().getLogger().severe("Couldn't read the manifest -> " + ioException.getMessage());
                    return;
                }
            }

            if (regionFilesFromManifest.isEmpty()) {
                MANIFEST_FILE.delete();
                USER.sendMessage(ChunkCleaner.PREFIX + "No region files left to process from the manifest. Manifest file got deleted!");
            } else {
                USER.sendMessage(ChunkCleaner.PREFIX + "Starting deletion process for " + regionFilesFromManifest.size() + " region files from the manifest.");
                processFiles(regionFilesFromManifest, regionFilesFromManifest.size());
            }
        } else
            createNewManifestAndQueue();
    }




    private  File getRegionFolder(World world) {
       File worldFolder = world.getWorldFolder();
       return switch (world.getEnvironment()) {
           case NETHER -> new File(worldFolder, "DIM-1/region");
           case THE_END -> new File(worldFolder, "DIM1/region");
           default -> new File(worldFolder, "region");
        };
    }

    private void createNewManifestAndQueue() {
        File regionFolder = getRegionFolder(WORLD);
        if(regionFolder.exists() && regionFolder.isDirectory()) {
            File[] regionFiles = regionFolder.listFiles((dir, name) -> name.endsWith(".mca"));
            if(regionFiles != null && regionFiles.length > 0) {
                ChunkCleaner.getInstance().getDataFolder().mkdirs();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(MANIFEST_FILE))) {
                    for(File file : regionFiles) {
                        writer.write(file.getAbsolutePath());
                        writer.newLine();
                    }
                } catch (IOException ioException) {
                    ChunkCleaner.getInstance().getLogger().severe("Failed to write manifest file. | " + ioException.getMessage());
                    USER.sendMessage(ChunkCleaner.PREFIX + "There was an error while creating the manifest please check the console for more information.");
                    return;
                }
                USER.sendMessage(ChunkCleaner.PREFIX + "Created a manifest file with §l" + regionFiles.length + "§r regions to process.");
                startDeletion(true);
            } else
                USER.sendMessage(ChunkCleaner.PREFIX + "Couldn't find region files within the world folder of " + WORLD.getName());
        } else
            USER.sendMessage(ChunkCleaner.PREFIX + "Region folder not found or there it's one dir down. §cAborting...");
    }


    private void processFiles(List<File> regionFiles, int totalFiles) {
        if (regionFiles.isEmpty()) {
            USER.sendMessage(ChunkCleaner.PREFIX + "All region files have been processed. Deleting the manifest.");
            MANIFEST_FILE.delete();
            return;
        }

        File regionFile = regionFiles.remove(0);
        int processed = totalFiles - regionFiles.size();

        Bukkit.getScheduler().runTaskAsynchronously(ChunkCleaner.getInstance(), () -> {
            processFile(regionFile);
            updateManifest(regionFiles);
            updateActionBar(processed, totalFiles);
            processFiles(regionFiles, totalFiles);
        });
    }

    private void processFile(File regionFile) {
        Set<ChunkCoordinates> chunksToDelete = new HashSet<>();
        int totalChunksInFile = 0;
        try (FileChannel channel = FileChannel.open(regionFile.toPath(), StandardOpenOption.READ)){
            if(channel.size() < 8192) {
                ChunkCleaner.getInstance().getLogger().warning("Skipping empty or invalid region file | " + regionFile.getName());
                return;
            }
            //REBUILDING HEADER TO PREVENT DAMAGE
            ByteBuffer header = ByteBuffer.allocate(4096);
            channel.read(header);
            header.flip();
            for(int i = 0; i < 1024; i++) {
                int entry = header.getInt(i*4);//HEXA Steps
                if(entry == 0)
                    continue;
                totalChunksInFile++;
                int locationOffset = (entry >> 8) * 4096;
                ByteBuffer chunkHeader = ByteBuffer.allocate(5);
                channel.read(chunkHeader, locationOffset);
                chunkHeader.flip();
                int chunkLength = chunkHeader.getInt() -1;
                byte compressionType = chunkHeader.get();
                if(chunkLength <= 0)
                    continue;
                if(compressionType != 2) {
                    ChunkCleaner.getInstance().getLogger().warning("Other compression type at index " + i + " in region file " + regionFile.getName() + " | The other compression type: " + compressionType);
                    continue;
                }
                ByteBuffer chunkDataBuffer = ByteBuffer.allocate(chunkLength);
                channel.read(chunkDataBuffer, locationOffset + 5);
                chunkDataBuffer.flip();

                if(shouldDeleteChunk(chunkDataBuffer)) {
                    int regionX = getRegion(regionFile, 1);
                    int regionZ = getRegion(regionFile, 2);
                    int localX = i % 32;
                    int localZ = i / 32;
                    int chunkX = regionX  * 32 + localX;
                    int chunkZ  = regionZ * 32 + localZ;

                    if(Bukkit.getOnlinePlayers()
                            .stream().noneMatch(p ->
                                    p.getLocation().getChunk().getX() == chunkX && p.getLocation().getChunk().getZ() == chunkZ))
                        chunksToDelete.add(new ChunkCoordinates(chunkX, chunkZ));
                }
            }
            handleFileModification(regionFile, chunksToDelete, totalChunksInFile);
        } catch (IOException ioException) {
            USER.sendMessage(ChunkCleaner.PREFIX + "Failed to process region file " +regionFile.getName() + " see the console for more information!");
            ChunkCleaner.getInstance().getLogger().severe("Failed to process region file "+regionFile.getName()  + " due to | " + ioException.getMessage());
        }
    }



    private void handleFileModification(File regionFile, Set<ChunkCoordinates> chunksToDelete, int totalChunks) {
        boolean allChunksDeleted = totalChunks > 0 && chunksToDelete.size() == totalChunks;
        if(allChunksDeleted) {
            ChunkCleaner.getInstance().getLogger().info("All " + totalChunks + " deleted from " + regionFile.getName() + ". | Deleting file!");
            regionFile.delete();
        } else if(!chunksToDelete.isEmpty())
            MCARewriter.rewriteRegionFile(regionFile, chunksToDelete);
        else
            ChunkCleaner.getInstance().getLogger().info("No chunks found to delete in" + regionFile.getName() + "! | Skipping file.");

    }
    private boolean shouldDeleteChunk(ByteBuffer compressedChunk) {
        try (InputStream is = new InflaterInputStream(new ByteArrayInputStream(
                compressedChunk.array(),
                compressedChunk.position(),
                compressedChunk.remaining()));
             DataInputStream dataInputStream = new DataInputStream(is)) {
            byte[] rawBytes = dataInputStream.readAllBytes();
            if(rawBytes.length == 0) //Empty or invalid
                return false;
            ChunkData chunkData = parseChunkData(new DataInputStream(new ByteArrayInputStream(rawBytes, 3, rawBytes.length -3)));
            if(!chunkData.foundInhabitedTime) { // We look for legacy format then -> root -> level -> chunk
                ChunkData legacyChunkData = parseChunkDataLegacy(new DataInputStream(new ByteArrayInputStream(rawBytes, 3 , rawBytes.length-3)));
                if(legacyChunkData.foundInhabitedTime)
                    chunkData = legacyChunkData;
            }

            if(chunkData.hasBlockEntities)
                return false;

            if(!chunkData.foundInhabitedTime) {
                ChunkCleaner.getInstance().getLogger().severe("There was a failure to read the tag due to corruption or invalid world generation. Skipping that chunk just in case.");
                return false;
            }

            return chunkData.inhabitedTime <= TO_CHECK_FOR_TIME*20L;
        } catch (IOException ioException) {
            ChunkCleaner.getInstance().getLogger().severe("Failed to read or parse chunk NBT | " + ioException.getMessage());
            return false;
        }
    }
    private ChunkData parseChunkData(DataInputStream dataInputStream) throws IOException {
        ChunkData chunkData = new ChunkData();
        while (dataInputStream.available() > 0) {
            byte tagType = dataInputStream.readByte();
            if(tagType == 0)
                break;
            byte[] nameBytes = new byte[dataInputStream.readUnsignedShort()];
            dataInputStream.readFully(nameBytes);
            String name = new String(nameBytes);

            if(tagType == 9 && name.equals("block_entities")) {
                byte listType = dataInputStream.readByte();
                int listSize = dataInputStream.readInt();
                if(listSize > 0) chunkData.hasBlockEntities = true;
                for(int i = 0; i < listSize; i++)
                    skipTagPayload(dataInputStream, listType);
            } else if(tagType == 4 && name.equals("InhabitedTime")) {
                chunkData.inhabitedTime = dataInputStream.readLong();
                chunkData.foundInhabitedTime = true;
            } else
                skipTagPayload(dataInputStream, tagType);

            if(chunkData.foundInhabitedTime && chunkData.hasBlockEntities)
                break;
        }
        return chunkData;
    }
    private ChunkData parseChunkDataLegacy(DataInputStream dataInputStream) throws IOException {
        while(dataInputStream.available() > 0) {
            byte tagType = dataInputStream.readByte();
            if(tagType == 0)
                break;
            byte[] nameBytes = new byte[dataInputStream.readUnsignedShort()];
            dataInputStream.readFully(nameBytes);
            String name = new String(nameBytes);
            if(tagType == 10 && name.equals("Level"))
                return parseChunkData(dataInputStream);
            else
                skipTagPayload(dataInputStream, tagType);
        }
        return new ChunkData();
    }

    private void skipTagPayload(DataInputStream data, byte tagType) throws IOException {
        switch (tagType) {
            case 1 -> data.skipBytes(1);  // TAG_Byte
            case 2 -> data.skipBytes(2);  // TAG_Short
            case 3 -> data.skipBytes(4);  // TAG_Int
            case 4 -> data.skipBytes(8);  // TAG_Long
            case 5 -> data.skipBytes(4);  // TAG_Float
            case 6 -> data.skipBytes(8);  // TAG_Double
            case 7 -> data.skipBytes(data.readInt()); // TAG_Byte_Array
            case 8 -> data.skipBytes(data.readUnsignedShort()); // TAG_String
            case 9 -> { // TAG_List
              byte listType = data.readByte();
              int size = data.readInt();
                for (int i = 0; i < size; i++) skipTagPayload(data, listType);
            }
            case 10 -> { // TAG_Compound
               while (true) {
                   byte type = data.readByte();
                   if (type == 0)
                       break;
                   data.skipBytes(data.readUnsignedShort()); // skip name
                   skipTagPayload(data, type);
               }}
            case 11 -> data.skipBytes((int) (data.readInt() * 4L)); // TAG_Int_Array
            case 12 -> data.skipBytes((int) (data.readInt() * 8L)); // TAG_Long_Array
       }
   }
    //if not noticed x = 1 or z = 2
    private int getRegion(File f, int xOrz) {
        String name = f.getName()
                .replace(".mca", "")
                .replaceFirst("^r[_.]", ""); // strip leading "r." or "r_"
        String[] split = name.split("[_.]"); // split on either dot or underscore
        return Integer.parseInt(split[xOrz - 1]);
   }
    private void updateActionBar(int done, int total) {
        for(Player player : Bukkit.getOnlinePlayers())
            if(player.isOp())
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(String.format("§5> §dJobs §f%d§d | §f%d §5<", done, total)));
    }
    private void updateManifest(List<File> regionFiles) {
        List<File> filesToWrite;
        synchronized (regionFiles) {filesToWrite = new ArrayList<>(regionFiles);}
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(MANIFEST_FILE, false))) {
            for(File file : filesToWrite) {
                writer.write(file.getAbsolutePath());
                writer.newLine();
            }
        } catch (IOException ioException) {ChunkCleaner.getInstance().getLogger().severe("Couldn't update the Manifest! | " + ioException.getMessage());}
    }
}
