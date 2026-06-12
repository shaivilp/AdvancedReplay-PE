package me.jumper251.replay.replaysystem.replaying.world;

import org.bukkit.World;

import me.jumper251.replay.replaysystem.data.snapshot.WorldSnapshot;

/**
 * A live dedicated replay world plus the snapshot it was built from. Coordinates are kept identical
 * to the original world, so recorded LocationData lines up without remapping.
 */
public class ReplayWorldHandle {

    private final String sessionId;
    private final String worldName;
    private final World world;
    private final WorldSnapshot snapshot;

    public ReplayWorldHandle(String sessionId, String worldName, World world, WorldSnapshot snapshot) {
        this.sessionId = sessionId;
        this.worldName = worldName;
        this.world = world;
        this.snapshot = snapshot;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getWorldName() {
        return worldName;
    }

    public World getWorld() {
        return world;
    }

    public WorldSnapshot getSnapshot() {
        return snapshot;
    }
}
