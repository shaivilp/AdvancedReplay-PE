package me.jumper251.replay.replaysystem.data.snapshot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A run-length-encoded snapshot of a single 16 x (maxY-minY+1) x 16 chunk column.
 *
 * Blocks are encoded in a canonical order (y outer, then x, then z) as numeric block id + data
 * value, which is the native 1.8 representation. RLE collapses the large air/stone runs so a
 * full-height chunk serializes to a few KB; combined with the GZIP layer in the replay savers the
 * whole 7x7 region stays small.
 *
 * This class intentionally has no Bukkit dependency so it can live in the serialized {@code ReplayData}.
 */
public class CapturedChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int chunkX;
    private final int chunkZ;
    private final int minY;
    private final int maxY;

    // Parallel RLE arrays. runBlockId[i]/runBlockData[i] repeated runLengths[i] times.
    private final int[] runLengths;
    private final int[] runBlockId;
    private final byte[] runBlockData;

    public CapturedChunk(int chunkX, int chunkZ, int minY, int maxY,
                         int[] runLengths, int[] runBlockId, byte[] runBlockData) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.minY = minY;
        this.maxY = maxY;
        this.runLengths = runLengths;
        this.runBlockId = runBlockId;
        this.runBlockData = runBlockData;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    /**
     * Walks every non-air block in the chunk, in capture order, invoking the visitor with the
     * block's local x/z (0-15), absolute y, and id/data. Air runs (id == 0) are skipped so callers
     * never pay for the empty parts of the world.
     */
    public void decode(BlockVisitor visitor) {
        int idx = 0;
        for (int r = 0; r < runLengths.length; r++) {
            int len = runLengths[r];
            int id = runBlockId[r];
            int data = runBlockData[r] & 0xFF;
            if (id == 0) {
                idx += len;
                continue;
            }
            for (int k = 0; k < len; k++) {
                int li = idx + k;
                int y = minY + (li >> 8);       // li / 256
                int rem = li & 0xFF;            // li % 256
                int x = rem >> 4;               // rem / 16
                int z = rem & 0xF;              // rem % 16
                visitor.visit(x, y, z, id, data);
            }
            idx += len;
        }
    }

    public interface BlockVisitor {
        void visit(int localX, int y, int localZ, int blockId, int blockData);
    }

    /**
     * Incremental RLE encoder. Feed every block in canonical order (y outer, then x 0-15, then
     * z 0-15) via {@link #add(int, int)}, then call {@link #build(int, int, int, int)}.
     */
    public static final class Encoder {

        private final List<int[]> runs = new ArrayList<>(); // {length, id, data}
        private int curId = Integer.MIN_VALUE;
        private int curData;
        private int curLen;

        public void add(int blockId, int blockData) {
            if (curLen > 0 && blockId == curId && blockData == curData) {
                curLen++;
                return;
            }
            flush();
            curId = blockId;
            curData = blockData;
            curLen = 1;
        }

        private void flush() {
            if (curLen > 0) {
                runs.add(new int[]{curLen, curId, curData});
                curLen = 0;
            }
        }

        public CapturedChunk build(int chunkX, int chunkZ, int minY, int maxY) {
            flush();
            int n = runs.size();
            int[] lengths = new int[n];
            int[] ids = new int[n];
            byte[] datas = new byte[n];
            for (int i = 0; i < n; i++) {
                int[] run = runs.get(i);
                lengths[i] = run[0];
                ids[i] = run[1];
                datas[i] = (byte) run[2];
            }
            return new CapturedChunk(chunkX, chunkZ, minY, maxY, lengths, ids, datas);
        }
    }
}
