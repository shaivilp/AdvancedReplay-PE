package me.jumper251.replay.replaysystem.data.snapshot;

import java.io.Serializable;
import java.util.List;

/**
 * A snapshot of a square region of chunks (default 7x7) captured at {@code /rp start}, used to
 * rebuild the world's state inside a dedicated replay world. Stored inside {@code ReplayData} and
 * therefore serialized into the .replay file. Null on replays recorded before this feature.
 */
public class WorldSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String originalWorld;

    private final int centerChunkX;
    private final int centerChunkZ;
    private final int radiusChunks;

    // World-space minimum corner of the captured region (block coords), useful for region bounds checks.
    private final int baseBlockX;
    private final int baseBlockZ;

    private final int minY;
    private final int maxY;

    private final List<CapturedChunk> chunks;

    private final byte schemaVersion;

    public WorldSnapshot(String originalWorld, int centerChunkX, int centerChunkZ, int radiusChunks,
                         int baseBlockX, int baseBlockZ, int minY, int maxY, List<CapturedChunk> chunks) {
        this.originalWorld = originalWorld;
        this.centerChunkX = centerChunkX;
        this.centerChunkZ = centerChunkZ;
        this.radiusChunks = radiusChunks;
        this.baseBlockX = baseBlockX;
        this.baseBlockZ = baseBlockZ;
        this.minY = minY;
        this.maxY = maxY;
        this.chunks = chunks;
        this.schemaVersion = 1;
    }

    public String getOriginalWorld() {
        return originalWorld;
    }

    public int getCenterChunkX() {
        return centerChunkX;
    }

    public int getCenterChunkZ() {
        return centerChunkZ;
    }

    public int getRadiusChunks() {
        return radiusChunks;
    }

    public int getBaseBlockX() {
        return baseBlockX;
    }

    public int getBaseBlockZ() {
        return baseBlockZ;
    }

    /** Exclusive max block X of the captured region. */
    public int getMaxBlockX() {
        return baseBlockX + (radiusChunks * 2 + 1) * 16;
    }

    /** Exclusive max block Z of the captured region. */
    public int getMaxBlockZ() {
        return baseBlockZ + (radiusChunks * 2 + 1) * 16;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public List<CapturedChunk> getChunks() {
        return chunks;
    }

    public byte getSchemaVersion() {
        return schemaVersion;
    }

    /** True when the given world-space block column lies inside the captured region (Y inclusive). */
    public boolean contains(int blockX, int y, int blockZ) {
        return blockX >= baseBlockX && blockX < getMaxBlockX()
                && blockZ >= baseBlockZ && blockZ < getMaxBlockZ()
                && y >= minY && y <= maxY;
    }
}
