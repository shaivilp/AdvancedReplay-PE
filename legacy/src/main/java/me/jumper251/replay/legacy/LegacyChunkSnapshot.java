package me.jumper251.replay.legacy;

import org.bukkit.ChunkSnapshot;
import org.bukkit.World;

/**
 * 1.8-only ChunkSnapshot accessors. {@code getBlockTypeId}/{@code getBlockData} are numeric-id APIs
 * that don't exist (or differ) in the 1.21 API the main module compiles against, so they live here.
 */
public class LegacyChunkSnapshot {

    public static int getBlockTypeId(ChunkSnapshot snapshot, int x, int y, int z) {
        return snapshot.getBlockTypeId(x, y, z);
    }

    public static int getBlockData(ChunkSnapshot snapshot, int x, int y, int z) {
        return snapshot.getBlockData(x, y, z);
    }

    public static int getMaxHeight(World world) {
        return world.getMaxHeight();
    }
}
