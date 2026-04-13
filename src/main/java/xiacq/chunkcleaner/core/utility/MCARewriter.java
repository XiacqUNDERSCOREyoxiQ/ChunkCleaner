package xiacq.chunkcleaner.core.utility;

import org.bukkit.Bukkit;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.logging.Level;

public class MCARewriter {
    private static final int SECTOR_BYTES = 4096;
    private static final int HEADER_SIZE = SECTOR_BYTES * 2;
    private static final int MAX_CHUNK_SIZE = 1024 * 1024;

    public static void rewriteRegionFile(File regionFile, Set<ChunkCoordinates> chunksToDelete) {
        File tmpFile = new File(regionFile.getParentFile(), regionFile.getName() + ".tmp");
        try (FileChannel inputChannel = FileChannel.open(regionFile.toPath(), StandardOpenOption.READ);
             FileChannel outputChannel = FileChannel.open(tmpFile.toPath(),
                     StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            ByteBuffer headerBuffer = ByteBuffer.allocate(HEADER_SIZE);
            inputChannel.read(headerBuffer, 0);
            headerBuffer.flip();
            ByteBuffer newHeaderBuffer = ByteBuffer.allocate(HEADER_SIZE);
            outputChannel.write(newHeaderBuffer, 0);
            int nextSector = 2;

            for (int z = 0; z < 32; z++)
                for (int x = 0; x < 32; x++) {
                    int chunkIndex = x + z * 32;
                    int locationOffset = (headerBuffer.get(chunkIndex * 4) & 0xFF) << 16 |
                            (headerBuffer.get(chunkIndex * 4 + 1) & 0xFF) << 8 |
                            (headerBuffer.get(chunkIndex * 4 + 2) & 0xFF);
                    int sectors = headerBuffer.get(chunkIndex * 4 + 3) & 0xFF;
                    if (locationOffset == 0 || sectors == 0) continue;

                    int globalX = (getRegion(regionFile, true) << 5) + x;
                    int globalZ = (getRegion(regionFile, false) << 5) + z;

                    if (chunksToDelete.contains(new ChunkCoordinates(globalX, globalZ))) {
                        newHeaderBuffer.putInt(chunkIndex * 4, 0);
                        newHeaderBuffer.putInt(SECTOR_BYTES + chunkIndex * 4, 0);
                        continue;
                    }

                    try {
                        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
                        int bytesRead = inputChannel.read(lengthBuffer, (long) locationOffset * SECTOR_BYTES);
                        if (bytesRead != 4) {
                            Bukkit.getLogger().warning("Failed to read chunk length for chunk at " + globalX + "," + globalZ +
                                    " in region " + regionFile.getName() + " | Skipping chunk.");
                            newHeaderBuffer.putInt(chunkIndex * 4, 0);
                            newHeaderBuffer.putInt(SECTOR_BYTES + chunkIndex * 4, 0);
                            continue;
                        }

                        lengthBuffer.flip();
                        int length = lengthBuffer.getInt();

                        if (length < 5 || length > MAX_CHUNK_SIZE) {
                            Bukkit.getLogger().warning("Invalid chunk size detected: " + length + " bytes at " +
                                    globalX + "," + globalZ + " in region " + regionFile.getName() + ". | Skipping chunk.");
                            newHeaderBuffer.putInt(chunkIndex * 4, 0);
                            newHeaderBuffer.putInt(SECTOR_BYTES + chunkIndex * 4, 0);
                            continue;
                        }

                        ByteBuffer chunkDataBuffer = ByteBuffer.allocate(length);
                        int chunkBytesRead = inputChannel.read(chunkDataBuffer, (long) locationOffset * SECTOR_BYTES + 4);
                        if (chunkBytesRead != length) {
                            Bukkit.getLogger().warning("Incomplete chunk read (" + chunkBytesRead + "/" + length + " bytes) at " +
                                    globalX + "," + globalZ + " in region " + regionFile.getName() + ". | Skipping chunk.");
                            newHeaderBuffer.putInt(chunkIndex * 4, 0);
                            newHeaderBuffer.putInt(SECTOR_BYTES + chunkIndex * 4, 0);
                            continue;
                        }
                        chunkDataBuffer.flip();

                        int totalChunkBytesOnDisk = 4 + length;
                        int requiredSectors = (totalChunkBytesOnDisk + SECTOR_BYTES - 1) / SECTOR_BYTES;
                        if (requiredSectors > 0xFF) {
                            Bukkit.getLogger().warning("Chunk at " + globalX + "," + globalZ + " in region " + regionFile.getName() +
                                    " requires too many sectors (" + requiredSectors + "). | Skipping chunk.");
                            newHeaderBuffer.putInt(chunkIndex * 4, 0);
                            newHeaderBuffer.putInt(SECTOR_BYTES + chunkIndex * 4, 0);
                            continue;
                        }

                        newHeaderBuffer.put(chunkIndex * 4, (byte)((nextSector >> 16) & 0xFF));
                        newHeaderBuffer.put(chunkIndex * 4 + 1, (byte)((nextSector >> 8) & 0xFF));
                        newHeaderBuffer.put(chunkIndex * 4 + 2, (byte)(nextSector & 0xFF));
                        newHeaderBuffer.put(chunkIndex * 4 + 3, (byte)(requiredSectors & 0xFF));

                        int timestamp = (int)(System.currentTimeMillis() / 1000L);
                        int timeOffset = SECTOR_BYTES + chunkIndex * 4;
                        newHeaderBuffer.put(timeOffset, (byte)((timestamp >> 24) & 0xFF));
                        newHeaderBuffer.put(timeOffset + 1, (byte)((timestamp >> 16) & 0xFF));
                        newHeaderBuffer.put(timeOffset + 2, (byte)((timestamp >> 8) & 0xFF));
                        newHeaderBuffer.put(timeOffset + 3, (byte)(timestamp & 0xFF));

                        outputChannel.write(lengthBuffer.rewind(), (long) nextSector * SECTOR_BYTES);
                        outputChannel.write(chunkDataBuffer, (long) nextSector * SECTOR_BYTES + 4);

                        int paddingBytes = requiredSectors * SECTOR_BYTES - totalChunkBytesOnDisk;
                        if (paddingBytes > 0) {
                            ByteBuffer paddingBuffer = ByteBuffer.allocate(paddingBytes);
                            outputChannel.write(paddingBuffer, (long) nextSector * SECTOR_BYTES + totalChunkBytesOnDisk);
                        }

                        nextSector += requiredSectors;
                    } catch (Exception exception) {
                        Bukkit.getLogger().log(Level.WARNING, "Failed to process chunk at " + globalX + "," + globalZ +
                                " in region " + regionFile.getName(), exception);
                        newHeaderBuffer.putInt(chunkIndex * 4, 0);
                        newHeaderBuffer.putInt(SECTOR_BYTES + chunkIndex * 4, 0);
                    }
                }


            newHeaderBuffer.flip();
            outputChannel.write(newHeaderBuffer, 0);
            outputChannel.truncate((long) nextSector * SECTOR_BYTES);

        } catch (IOException ioException) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to rewrite region file " + regionFile.getName(), ioException);
            try {
                Files.deleteIfExists(tmpFile.toPath());
            } catch (IOException deleteEx) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to delete temporary file: " + tmpFile.getName(), deleteEx);
            }
            throw new RuntimeException("Region file rewrite failed", ioException);
        }

        try {
            Files.delete(regionFile.toPath());
            Files.move(tmpFile.toPath(), regionFile.toPath());
        } catch (IOException ioException) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to replace original region file " + regionFile.getName(), ioException);
            throw new RuntimeException("Failed to replace original region file", ioException);
        }
    }


    private static int getRegion(File file, boolean x) {
        String[] split = file.getName().split("\\.");
        return x ? Integer.parseInt(split[1]) : Integer.parseInt(split[2]);
    }
}
