package me.jumper251.replay.replaysystem.recording.snapshot;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;

import me.jumper251.replay.legacy.LegacyChunkSnapshot;
import me.jumper251.replay.replaysystem.data.snapshot.CapturedChunk;
import me.jumper251.replay.replaysystem.data.snapshot.WorldSnapshot;

/**
 * Captures a square region of chunks (default 7x7) at record start into a {@link WorldSnapshot}.
 * Must be called on the main thread (chunk access is not thread-safe). Block ids/data are read via
 * the legacy 1.8 wrapper.
 */
public class ChunkCapture {

    public static WorldSnapshot capture(World world, int centerChunkX, int centerChunkZ, int radius) {
        int maxHeight = LegacyChunkSnapshot.getMaxHeight(world);
        int minY = 0;
        int maxY = maxHeight - 1;

        List<CapturedChunk> chunks = new ArrayList<>();

        for (int cx = centerChunkX - radius; cx <= centerChunkX + radius; cx++) {
            for (int cz = centerChunkZ - radius; cz <= centerChunkZ + radius; cz++) {
                Chunk chunk = world.getChunkAt(cx, cz);
                ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);

                CapturedChunk.Encoder encoder = new CapturedChunk.Encoder();
                // Canonical order: y outer, then x, then z (must match CapturedChunk.decode).
                for (int y = minY; y <= maxY; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            int id = LegacyChunkSnapshot.getBlockTypeId(snapshot, x, y, z);
                            int data = LegacyChunkSnapshot.getBlockData(snapshot, x, y, z);
                            encoder.add(id, data);
                        }
                    }
                }

                chunks.add(encoder.build(cx, cz, minY, maxY));
            }
        }

        int baseBlockX = (centerChunkX - radius) * 16;
        int baseBlockZ = (centerChunkZ - radius) * 16;

        return new WorldSnapshot(world.getName(), centerChunkX, centerChunkZ, radius,
                baseBlockX, baseBlockZ, minY, maxY, chunks);
    }
}
