package me.jumper251.replay.replaysystem.replaying.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.jumper251.replay.replaysystem.replaying.ReplayHelper;
import me.jumper251.replay.replaysystem.replaying.ReplayPacketListener;
import me.jumper251.replay.replaysystem.replaying.Replayer;
import me.jumper251.replay.replaysystem.replaying.world.ReplayWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import me.jumper251.replay.ReplaySystem;
import me.jumper251.replay.api.ReplaySessionFinishEvent;
import me.jumper251.replay.filesystem.ConfigManager;
import me.jumper251.replay.filesystem.ItemConfig;
import me.jumper251.replay.filesystem.ItemConfigOption;
import me.jumper251.replay.filesystem.ItemConfigType;

/**
 * Owns the per-session viewer state for a synced replay: it preps each viewer when they join,
 * restores them when they leave, and runs the packet listener once for the whole session.
 */
public class ReplaySession {

	private final Replayer replayer;

	private final Player owner;

	private final ReplayPacketListener packetListener;

	private final Map<String, ViewerState> viewerStates = new HashMap<>();

	public ReplaySession(Replayer replayer) {
		this.replayer = replayer;
		this.owner = this.replayer.getWatchingPlayer();
		this.packetListener = new ReplayPacketListener(replayer);
	}

	public void startSession() {
		this.packetListener.register();
		addViewer(this.owner, true);
	}

	/** Saves a viewer's state and puts them into spectating-ready replay mode. Call before teleporting them into the replay world. */
	public void addViewer(Player player, boolean isOwner) {
		if (viewerStates.containsKey(player.getName())) {
			return;
		}
		viewerStates.put(player.getName(), new ViewerState(player));

		player.setHealth(player.getMaxHealth());
		player.setFoodLevel(20);
		player.getInventory().clear();

		giveControlItems(player, isOwner);

		player.setAllowFlight(true);
		player.setFlying(true);

		if (ConfigManager.HIDE_PLAYERS) {
			for (Player all : Bukkit.getOnlinePlayers()) {
				if (all == player) continue;
				player.hidePlayer(all);
			}
		}
	}

	private void giveControlItems(Player player, boolean isOwner) {
		List<ItemConfigOption> configItems = new ArrayList<>();
		// Everyone gets a leave button; only the owner drives the timeline.
		configItems.add(ItemConfig.getItem(ItemConfigType.LEAVE));
		if (isOwner) {
			configItems.add(ItemConfig.getItem(ItemConfigType.TELEPORT));
			configItems.add(ItemConfig.getItem(ItemConfigType.SPEED));
			configItems.add(ItemConfig.getItem(ItemConfigType.BACKWARD));
			configItems.add(ItemConfig.getItem(ItemConfigType.FORWARD));
			configItems.add(ItemConfig.getItem(ItemConfigType.PAUSE));
		}

		for (ItemConfigOption item : configItems) {
			if (item != null && item.isEnabled()) {
				player.getInventory().setItem(item.getSlot(), ReplayHelper.createItem(item));
			}
		}
	}

	/** Restores a single non-owner viewer that is leaving an ongoing session. */
	public void removeViewer(Player player) {
		ViewerState state = viewerStates.remove(player.getName());
		if (state == null) return;

		packetListener.revertCamera(player);
		state.restore(player);
		unhide(player);
	}

	public void stopSession() {
		this.packetListener.unregister();

		final List<String> names = new ArrayList<>(viewerStates.keySet());

		new BukkitRunnable() {

			@Override
			public void run() {
				// Take everyone out of spectator camera before teleporting them back (avoids void world).
				packetListener.revertCameras();

				for (String name : names) {
					Player p = Bukkit.getPlayer(name);
					ViewerState state = viewerStates.get(name);
					if (p != null && state != null) {
						state.restore(p);
						unhide(p);
					}
					ReplayHelper.replaySessions.remove(name);
				}
				viewerStates.clear();

				// Viewers are out of the replay world now, so it is safe to unload/delete it.
				if (replayer.getReplayWorld() != null) {
					ReplayWorldManager.teardown(replayer.getSessionId());
				}

				ReplaySessionFinishEvent finishEvent = new ReplaySessionFinishEvent(replayer.getReplay(), owner);
				Bukkit.getPluginManager().callEvent(finishEvent);
			}
		}.runTask(ReplaySystem.getInstance());
	}

	private void unhide(Player player) {
		if (ConfigManager.HIDE_PLAYERS) {
			for (Player all : Bukkit.getOnlinePlayers()) {
				if (all == player) continue;
				player.showPlayer(all);
			}
		}
	}

	public ReplayPacketListener getPacketListener() {
		return packetListener;
	}
}
