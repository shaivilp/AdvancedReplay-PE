package me.jumper251.replay.replaysystem.replaying;

import java.util.ArrayList;



import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;




import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import me.jumper251.replay.filesystem.MessageBuilder;
import me.jumper251.replay.filesystem.Messages;
import me.jumper251.replay.replaysystem.data.snapshot.WorldSnapshot;
import me.jumper251.replay.replaysystem.replaying.session.ReplaySession;
import me.jumper251.replay.replaysystem.replaying.world.ReplayWorldHandle;
import me.jumper251.replay.replaysystem.replaying.world.ReplayWorldManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;



import me.jumper251.replay.ReplaySystem;
import me.jumper251.replay.api.IReplayHook;
import me.jumper251.replay.api.ReplayAPI;
import me.jumper251.replay.filesystem.ConfigManager;
import me.jumper251.replay.replaysystem.Replay;
import me.jumper251.replay.replaysystem.data.ActionData;
import me.jumper251.replay.replaysystem.data.ActionType;
import me.jumper251.replay.replaysystem.data.ReplayData;
import me.jumper251.replay.replaysystem.data.types.ItemData;
import me.jumper251.replay.replaysystem.data.types.LocationData;
import me.jumper251.replay.replaysystem.data.types.SpawnData;
import me.jumper251.replay.replaysystem.utils.entities.IEntity;
import me.jumper251.replay.replaysystem.utils.entities.INPC;



public class Replayer {

	private HashMap<String, INPC> npcs;

	private HashMap<Integer, IEntity> entities;

	private Map<Location, ItemData> blockChanges;

	// The owner is the player who ran /rp play and controls the timeline. viewers includes the owner
	// plus any staff that joined via /rp join; all of them see the same synchronized playback.
	private Player watcher;
	private final Set<Player> viewers;
	private final String sessionId;
	private World replayWorld;

	private Replay replay;

	private BukkitRunnable run;

	private int currentTicks;
	private double speed, tmpTicks;

	private boolean paused, started;

	private ReplayingUtils utils;
	private ReplaySession session;

	public Replayer(Replay replay, Player watcher) {
		this.replay = replay;
		this.watcher = watcher;
		this.viewers = new LinkedHashSet<>();
		this.viewers.add(watcher);
		this.sessionId = ReplayWorldManager.nextSessionId(replay.getId());
		this.npcs = new HashMap<String, INPC>();
		this.entities = new HashMap<Integer, IEntity>();
		this.blockChanges = new HashMap<>();

		this.utils = new ReplayingUtils(this);
		this.session = new ReplaySession(this);
		this.paused = false;
	}


	public boolean start() {
		ReplayData data = this.replay.getData();
		SpawnData spawnData = findSpawn(data);

		WorldSnapshot snapshot = data.getWorldSnapshot();
		boolean useDedicated = snapshot != null && ConfigManager.USE_DEDICATED_WORLD;

		if (!useDedicated && spawnData != null && !spawnData.getLocation().isValidWorld()) {
			sendMessage(Messages.REPLAYING_WORLD_NOT_FOUND.arg("world", spawnData.getLocation().getWorld()));
			return false;
		}

		ReplayHelper.replaySessions.put(watcher.getName(), this);
		ReplayHelper.activeReplayers.put(replay.getId(), this);

		final SpawnData fSpawn = spawnData;
		if (useDedicated) {
			ReplayWorldHandle handle = ReplayWorldManager.createWorld(sessionId, snapshot);
			if (handle != null) {
				this.replayWorld = handle.getWorld();
				sendMessage("§7Preparing replay world...");
				ReplayWorldManager.pasteChunks(handle, new Runnable() {
					@Override
					public void run() {
						beginPlayback(fSpawn);
					}
				});
				return true;
			}

			// At capacity or creation failed: fall back to the live recorded world.
			if (fSpawn != null && !fSpawn.getLocation().isValidWorld()) {
				sendMessage(Messages.REPLAYING_WORLD_NOT_FOUND.arg("world", fSpawn.getLocation().getWorld()));
				ReplayHelper.replaySessions.remove(watcher.getName());
				ReplayHelper.activeReplayers.remove(replay.getId());
				return false;
			}
		}

		beginPlayback(fSpawn);
		return true;
	}

	private SpawnData findSpawn(ReplayData data) {
		if (data.getActions().containsKey(0)) {
			for (ActionData startData : data.getActions().get(0)) {
				if (startData.getPacketData() instanceof SpawnData) {
					return (SpawnData) startData.getPacketData();
				}
			}
		}
		return findFirstSpawn(data).orElse(null);
	}

	private void beginPlayback(SpawnData spawnData) {
		this.session.startSession();

		if (spawnData != null) {
			Location target = resolveSpawn(spawnData.getLocation());
			for (Player viewer : new ArrayList<>(viewers)) {
				if (viewer != null && viewer.isOnline()) {
					viewer.teleport(target);
				}
			}
		}

		this.speed = 1;

		executeTick(0, ReplayingMode.PLAYING);

		this.run = new BukkitRunnable() {

			@Override
			public void run() {
				updateProgress();

				if (Replayer.this.paused) return;

				Replayer.this.tmpTicks += speed;
				if (Replayer.this.tmpTicks % 1 != 0) return;

				int duration = Replayer.this.replay.getData().getDuration();
				if (currentTicks < duration) {

					executeTick(currentTicks++, ReplayingMode.PLAYING);

					if ((currentTicks + 2) < duration && speed == 2)  {
						executeTick(currentTicks++, ReplayingMode.PLAYING);

					}

				} else {

					stop();
				}
			}
		};

		this.run.runTaskTimerAsynchronously(ReplaySystem.getInstance(), 1, 1);
	}

	/** Resolves a recorded location into the dedicated replay world if one is active, else the live world. */
	public Location resolveSpawn(LocationData data) {
		if (this.replayWorld != null) {
			return new Location(this.replayWorld, data.getX(), data.getY(), data.getZ(), data.getYaw(), data.getPitch());
		}
		return LocationData.toLocation(data);
	}

	public void executeTick(int tick, ReplayingMode mode) {
		ReplayData data = this.replay.getData();
		if (!data.getActions().isEmpty() && data.getActions().containsKey(tick)) {

			if (tick == 0 && started) return;
			this.started = true;

			List<ActionData> list = data.getActions().get(tick);
			for (ActionData action : list) {

				utils.handleAction(action, data, mode);

				if (action.getType() == ActionType.CUSTOM) {
					if (ReplayAPI.getInstance().getHookManager().isRegistered()) {
						for (IReplayHook hook : ReplayAPI.getInstance().getHookManager().getHooks()) {
							hook.onPlay(action, Replayer.this);
						}
					}
				}

			}

			if (tick == 0) data.getActions().remove(tick);
		}
	}

	private void updateProgress() {
		ConfigManager.PROGRESS_TYPE.update(this);
	}

	private Optional<SpawnData> findFirstSpawn(ReplayData data) {
		return data.getActions().values().stream()
				.flatMap(Collection::stream)
				.filter(action -> action.getPacketData() instanceof SpawnData)
				.map(action -> (SpawnData) action.getPacketData())
				.findFirst();
	}

	/** Adds a staff member to the running synced session: saves their state, teleports them in, and re-syncs the current NPCs/entities. */
	public void addViewer(Player player) {
		if (viewers.contains(player)) return;
		viewers.add(player);
		ReplayHelper.replaySessions.put(player.getName(), this);

		session.addViewer(player, false);

		World destWorld = replayWorld != null ? replayWorld : watcher.getWorld();
		Location dest = watcher.isOnline() && watcher.getWorld() == destWorld ? watcher.getLocation() : destWorld.getSpawnLocation();
		player.teleport(dest);

		for (INPC npc : npcs.values()) {
			npc.addViewer(player);
		}
		for (IEntity entity : entities.values()) {
			entity.addViewer(player);
		}
	}

	/** Removes a single (non-owner) viewer from the session, restoring their state. */
	public void removeViewer(Player player) {
		if (!viewers.contains(player)) return;
		viewers.remove(player);
		ReplayHelper.replaySessions.remove(player.getName());

		for (INPC npc : npcs.values()) {
			npc.removeViewer(player);
		}
		for (IEntity entity : entities.values()) {
			entity.removeViewer(player);
		}

		session.removeViewer(player);
	}

	/** Handles a viewer disconnecting: the owner leaving ends the session for everyone. */
	public void handleViewerQuit(Player player) {
		boolean owner = isOwner(player);
		// Restore the quitting player synchronously (inventory etc.) while they are still around.
		removeViewer(player);
		if (owner) {
			stop();
		}
	}

	public void stop() {
		sendMessage(Messages.REPLAYING_FINISHED_WATCHING.getBuilder());

		if (this.run != null) {
			this.run.cancel();
			this.run = null;
		}
		this.getReplay().getData().getActions().clear();

		for (INPC npc : this.npcs.values()) {
			npc.remove();
		}

		for (IEntity entity : this.entities.values()) {
			entity.remove();
		}

		this.utils.despawn(new ArrayList<>(this.utils.getEntities().values()), null);

		this.npcs.clear();

		this.replay.setPlaying(false);

		if (ReplayHelper.activeReplayers.get(replay.getId()) == this) {
			ReplayHelper.activeReplayers.remove(replay.getId());
		}

		// In a dedicated world the whole world is deleted on teardown, so no per-block reset is needed.
		if (ConfigManager.WORLD_RESET && replayWorld == null) this.utils.resetChanges(this.blockChanges);

		this.session.stopSession();

	    if (isPaused()) {
	        ReplayHelper.sendClientPause(watcher, false);
	    }
	}

	public HashMap<String, INPC> getNPCList() {
		return npcs;
	}

	public HashMap<Integer, IEntity> getEntityList() {
		return entities;
	}

	public Map<Location, ItemData> getBlockChanges() {
		return blockChanges;
	}

	public Player getWatchingPlayer() {
		return watcher;
	}

	public Collection<Player> getViewers() {
		return viewers;
	}

	public boolean isOwner(Player player) {
		return player != null && player.equals(watcher);
	}

	public String getSessionId() {
		return sessionId;
	}

	public World getReplayWorld() {
		return replayWorld;
	}

	public Replay getReplay() {
		return replay;
	}

	public ReplayingUtils getUtils() {
		return utils;
	}

	public ReplaySession getSession() {
		return session;
	}

	public boolean isPaused() {
		return paused;
	}

    public void setPaused(boolean paused, boolean updateClient) {
        if (updateClient) {
            for (Player viewer : viewers) {
                if (viewer != null && viewer.isOnline()) {
                    ReplayHelper.sendClientPause(viewer, paused);
                }
            }
        }

        this.paused = paused;
    }

	public void setPaused(boolean paused) {
        this.setPaused(paused, false);
	}



	public void setSpeed(double speed) {
		this.tmpTicks = 1;
		this.speed = speed;

		String sub = speed >= 1 ? "§ax" + speed : "§cx" + speed;
		for (Player viewer : viewers) {
			if (viewer != null && viewer.isOnline()) {
				ReplayHelper.sendTitle(viewer, " ", sub, 10);
			}
		}
	}

	public double getSpeed() {
		return speed;
	}

	public int getCurrentTicks() {
		return currentTicks;
	}

	public void setCurrentTicks(int currentTicks) {
		this.currentTicks = currentTicks;
	}

	public void sendMessage(String message) {
		if (message == null) return;
		for (Player viewer : viewers) {
			if (viewer != null && viewer.isOnline()) {
				viewer.sendMessage(Messages.PREFIX.getFullMessage() + message);
			}
		}
	}

	public void sendMessage(MessageBuilder message) {
		if (message == null) return;
		for (Player viewer : viewers) {
			if (viewer != null && viewer.isOnline()) {
				message.send(viewer);
			}
		}
	}
}
