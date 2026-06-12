package me.jumper251.replay.replaysystem.replaying.world;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.jumper251.replay.ReplaySystem;
import me.jumper251.replay.filesystem.ConfigManager;
import me.jumper251.replay.legacy.LegacyBlock;
import me.jumper251.replay.legacy.LegacyVoidGenerator;
import me.jumper251.replay.legacy.LegacyWorld;
import me.jumper251.replay.replaysystem.data.snapshot.CapturedChunk;
import me.jumper251.replay.replaysystem.data.snapshot.WorldSnapshot;
import me.jumper251.replay.utils.LogUtils;

/**
 * Owns the lifecycle of the per-replay dedicated worlds: creating an empty void world, pasting the
 * captured 7x7 chunk region into it, and tearing it down (unload + delete) when the replay ends.
 * One world per active replay; the registry supports several concurrent replays.
 */
public class ReplayWorldManager {

    public static final String WORLD_PREFIX = "ar_replay_";

    private static final Map<String, ReplayWorldHandle> activeWorlds = new ConcurrentHashMap<>();
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private static final int CHUNKS_PER_TICK = 3;

    /** Mints a unique, filesystem-safe session id derived from the replay id. */
    public static String nextSessionId(String replayId) {
        String sanitized = replayId == null ? "replay" : replayId.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        return sanitized + "_" + COUNTER.incrementAndGet();
    }

    public static boolean isAtCapacity() {
        return activeWorlds.size() >= ConfigManager.MAX_REPLAY_WORLDS;
    }

    /**
     * Creates and registers an empty void world for the session. Must run on the main thread.
     * Returns null if at capacity or world creation failed.
     */
    public static ReplayWorldHandle createWorld(String sessionId, WorldSnapshot snapshot) {
        if (isAtCapacity()) {
            return null;
        }
        String worldName = WORLD_PREFIX + sessionId;
        try {
            World existing = Bukkit.getWorld(worldName);
            if (existing != null) {
                Bukkit.unloadWorld(existing, false);
            }

            WorldCreator creator = new WorldCreator(worldName);
            creator.generator(LegacyVoidGenerator.create());
            creator.generateStructures(false);
            World world = creator.createWorld();
            if (world == null) {
                return null;
            }
            LegacyWorld.applyReplaySettings(world);

            ReplayWorldHandle handle = new ReplayWorldHandle(sessionId, worldName, world, snapshot);
            activeWorlds.put(sessionId, handle);
            return handle;
        } catch (Throwable t) {
            LogUtils.log("Failed to create replay world " + worldName + ": " + t.getMessage());
            return null;
        }
    }

    /**
     * Pastes the captured chunks into the world over several ticks (skipping air), then runs
     * onComplete on the main thread. Playback must not start until this finishes.
     */
    public static void pasteChunks(ReplayWorldHandle handle, Runnable onComplete) {
        final World world = handle.getWorld();
        final List<CapturedChunk> chunks = handle.getSnapshot().getChunks();

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int processed = 0;
                while (index < chunks.size() && processed < CHUNKS_PER_TICK) {
                    pasteChunk(world, chunks.get(index));
                    index++;
                    processed++;
                }
                if (index >= chunks.size()) {
                    cancel();
                    onComplete.run();
                }
            }
        }.runTaskTimer(ReplaySystem.getInstance(), 1L, 1L);
    }

    private static void pasteChunk(final World world, CapturedChunk chunk) {
        final int baseX = chunk.getChunkX() << 4;
        final int baseZ = chunk.getChunkZ() << 4;
        chunk.decode(new CapturedChunk.BlockVisitor() {
            @Override
            public void visit(int localX, int y, int localZ, int blockId, int blockData) {
                Block block = world.getBlockAt(baseX + localX, y, baseZ + localZ);
                LegacyBlock.setTypeIdAndData(block, blockId, (byte) blockData, false);
            }
        });
    }

    public static ReplayWorldHandle getHandle(String sessionId) {
        return activeWorlds.get(sessionId);
    }

    /** Unloads and deletes the world for the session (folder delete runs async). */
    public static void teardown(String sessionId) {
        ReplayWorldHandle handle = activeWorlds.remove(sessionId);
        if (handle == null) return;
        World world = handle.getWorld();
        if (world == null) return;

        evacuate(world);
        final File folder = world.getWorldFolder();
        Bukkit.unloadWorld(world, false);

        new BukkitRunnable() {
            @Override
            public void run() {
                deleteWithRetries(folder, 5);
            }
        }.runTaskAsynchronously(ReplaySystem.getInstance());
    }

    /** Synchronous teardown for server shutdown (async tasks won't run during disable). */
    public static void teardownAll() {
        for (String id : new ArrayList<>(activeWorlds.keySet())) {
            ReplayWorldHandle handle = activeWorlds.remove(id);
            if (handle == null || handle.getWorld() == null) continue;
            evacuate(handle.getWorld());
            File folder = handle.getWorld().getWorldFolder();
            Bukkit.unloadWorld(handle.getWorld(), false);
            deleteWithRetries(folder, 1);
        }
    }

    /** Removes leftover replay worlds from a previous run (e.g. after a crash). Call on enable. */
    public static void cleanupOrphanedWorlds() {
        File container = Bukkit.getWorldContainer();
        File[] files = container.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory() && f.getName().startsWith(WORLD_PREFIX) && Bukkit.getWorld(f.getName()) == null) {
                deleteWithRetries(f, 1);
            }
        }
    }

    private static void evacuate(World world) {
        World fallback = fallbackWorld(world);
        if (fallback == null) return;
        for (Player p : new ArrayList<>(world.getPlayers())) {
            p.teleport(fallback.getSpawnLocation());
        }
    }

    private static World fallbackWorld(World exclude) {
        for (World w : Bukkit.getWorlds()) {
            if (w != exclude && !w.getName().startsWith(WORLD_PREFIX)) {
                return w;
            }
        }
        return null;
    }

    private static void deleteWithRetries(File folder, int retries) {
        for (int attempt = 0; attempt <= retries; attempt++) {
            if (deleteRecursively(folder)) {
                return;
            }
            try {
                Thread.sleep(200L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        LogUtils.log("Could not fully delete replay world folder: " + folder.getName());
    }

    private static boolean deleteRecursively(File file) {
        if (!file.exists()) return true;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        return file.delete();
    }
}
