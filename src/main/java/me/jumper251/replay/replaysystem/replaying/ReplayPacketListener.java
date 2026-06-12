package me.jumper251.replay.replaysystem.replaying;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;

import me.jumper251.replay.listener.AbstractListener;
import me.jumper251.replay.utils.PacketEventsUtil;

public class ReplayPacketListener extends AbstractListener {

    private PacketListenerAbstract packetListener;

    private final Replayer replayer;

    private int previous;

    private final HashMap<Player, Integer> spectating;

    public ReplayPacketListener(Replayer replayer) {
        this.replayer = replayer;
        this.spectating = new HashMap<>();
        this.previous = -1;
    }

    @Override
    public void register() {
        if (isRegistered()) {
            return;
        }
        this.packetListener = new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
                    return;
                }
                WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
                Player p = Bukkit.getPlayer(event.getUser().getUUID());
                if (p == null) {
                    return;
                }
                if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                    return;
                }
                if (!ReplayHelper.replaySessions.containsKey(p.getName())) {
                    return;
                }
                if (replayer.getNPCList().values().stream().noneMatch(ent -> packet.getEntityId() == ent.getId())) {
                    return;
                }
                if (p.getGameMode() != GameMode.SPECTATOR) {
                    previous = p.getGameMode().getValue();
                }
                setCamera(p, packet.getEntityId(), 3F);
            }

            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (event.getPacketType() != PacketType.Play.Server.DESTROY_ENTITIES) {
                    return;
                }
                WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(event);
                Player p = Bukkit.getPlayer(event.getUser().getUUID());
                if (p == null) {
                    return;
                }
                if (!ReplayHelper.replaySessions.containsKey(p.getName()) || !isSpectating(p)) {
                    return;
                }
                for (int id : packet.getEntityIds()) {
                    if (id == spectating.get(p)) {
                        setCamera(p, p.getEntityId(), previous);
                    }
                }
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);
    }

    @Override
    public void unregister() {
        if (packetListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
            packetListener = null;
        }
    }

    public boolean isRegistered() {
        return this.packetListener != null;
    }

    public int getPrevious() {
        return previous;
    }

    public boolean isSpectating(Player p) {
        return this.spectating.containsKey(p);
    }

    /**
     * Takes every spectating viewer out of the client-side spectator camera and restores their
     * previous gamemode. Must run before the viewers are teleported back, otherwise the client is
     * left attached to a now-removed entity and renders the world as void until relog.
     * Idempotent: if the despawn handler already reverted a viewer, the map is empty and this no-ops.
     */
    public void revertCameras() {
        if (this.spectating.isEmpty()) {
            return;
        }
        for (Player p : new ArrayList<>(this.spectating.keySet())) {
            if (p != null && p.isOnline()) {
                setCamera(p, p.getEntityId(), previous);
            }
        }
        this.spectating.clear();
    }

    /** Reverts a single viewer's spectator camera (used when one viewer leaves a synced session). */
    public void revertCamera(Player p) {
        if (p != null && this.spectating.containsKey(p)) {
            setCamera(p, p.getEntityId(), previous);
        }
    }

    public void setCamera(Player p, int entityID, float gamemode) {
        WrapperPlayServerChangeGameState state = new WrapperPlayServerChangeGameState(
                WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE,
                gamemode < 0 ? 0 : gamemode
        );
        PacketEventsUtil.sendPacket(p, state);
        PacketEventsUtil.sendPacket(p, new WrapperPlayServerCamera(entityID));

        if (gamemode == 3F) {
            this.spectating.put(p, entityID);
        } else if (this.spectating.containsKey(p)) {
            this.spectating.remove(p);
        }
    }
}
