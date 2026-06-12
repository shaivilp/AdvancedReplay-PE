package me.jumper251.replay.replaysystem.replaying.session;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Snapshot of a viewer's state before they entered a replay, so it can be fully restored when they
 * leave (or the replay ends). One per viewer of a synced session.
 */
public class ViewerState {

    private final ItemStack[] inventory;
    private final int level;
    private final float xp;
    private final Location start;
    private final GameMode gameMode;
    private final boolean allowFlight;
    private final boolean flying;

    public ViewerState(Player player) {
        this.inventory = player.getInventory().getContents();
        this.level = player.getLevel();
        this.xp = player.getExp();
        this.start = player.getLocation();
        this.gameMode = player.getGameMode();
        this.allowFlight = player.getAllowFlight();
        this.flying = player.isFlying();
    }

    public Location getStart() {
        return start;
    }

    public void restore(Player player) {
        player.getInventory().clear();
        player.getInventory().setContents(inventory);
        player.setLevel(level);
        player.setExp(xp);
        player.teleport(start);
        player.setGameMode(gameMode);
        player.setAllowFlight(allowFlight);
        player.setFlying(flying);
    }
}
