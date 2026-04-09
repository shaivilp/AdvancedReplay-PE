package me.jumper251.replay.replaysystem.utils.entities;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHurtAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUseBed;
import me.jumper251.replay.replaysystem.utils.NPCManager;
import me.jumper251.replay.utils.MathUtils;
import me.jumper251.replay.utils.PacketEventsUtil;
import me.jumper251.replay.utils.StringUtils;
import me.jumper251.replay.utils.VersionUtil;
import me.jumper251.replay.utils.VersionUtil.VersionEnum;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class PacketNPC implements INPC {

    private int id;

    private UUID uuid;

    private String name;

    private int tabMode;

    private List<EntityData<?>> data;

    private UserProfile profile;

    private Location location;

    private Location origin;

    private NPCSpawnPacket spawnPacket;

    private float yaw;

    private float pitch;

    private Player[] visible;

    private Player oldVisible;

    private List<com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment> lastEquipment;

    public PacketNPC(int id, UUID uuid, String name) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
        this.tabMode = 1;
        this.lastEquipment = new ArrayList<>();
        this.visible = new Player[]{};
        this.spawnPacket = new NPCSpawnPacket(VersionUtil.isAbove(VersionEnum.V1_20));
    }

    public PacketNPC() {
        this(MathUtils.randInt(50000, 400000), UUID.randomUUID(), StringUtils.getRandomString(6));
    }

    private UserProfile effectiveProfile() {
        if (profile != null) {
            return profile;
        }
        return new UserProfile(uuid, name);
    }

    private void sendTabAdd(Player player) {
        UserProfile up = effectiveProfile();
        if (VersionUtil.isAbove(VersionEnum.V1_19)) {
            EnumSet<WrapperPlayServerPlayerInfoUpdate.Action> actions = EnumSet.of(
                    WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                    WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED,
                    WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE,
                    WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY,
                    WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME);
            WrapperPlayServerPlayerInfoUpdate.PlayerInfo info = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                    up, true, 1, GameMode.CREATIVE, Component.text(name), null);
            PacketEventsUtil.sendPacket(player, new WrapperPlayServerPlayerInfoUpdate(actions, Collections.singletonList(info)));
        } else {
            WrapperPlayServerPlayerInfo.PlayerData dataRow = new WrapperPlayServerPlayerInfo.PlayerData(
                    Component.text(name), up, GameMode.CREATIVE, 1);
            PacketEventsUtil.sendPacket(player, new WrapperPlayServerPlayerInfo(
                    WrapperPlayServerPlayerInfo.Action.ADD_PLAYER, Collections.singletonList(dataRow)));
        }
    }

    private void sendTabRemove(Player player) {
        UserProfile up = effectiveProfile();
        if (VersionUtil.isAbove(VersionEnum.V1_19)) {
            PacketEventsUtil.sendPacket(player, new WrapperPlayServerPlayerInfoRemove(Collections.singletonList(up.getUUID())));
        } else {
            WrapperPlayServerPlayerInfo.PlayerData dataRow = new WrapperPlayServerPlayerInfo.PlayerData(
                    null, up, null, -1);
            PacketEventsUtil.sendPacket(player, new WrapperPlayServerPlayerInfo(
                    WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER, Collections.singletonList(dataRow)));
        }
    }

    @Override
    public void spawn(Location loc, int tabMode, Player... players) {
        this.tabMode = tabMode;
        this.visible = players;
        this.oldVisible = players[0];
        this.location = loc;
        this.origin = loc;
        NPCManager.names.add(this.name);

        this.spawnPacket.setEntityID(this.id);
        this.spawnPacket.setUniqueId(uuid);
        this.spawnPacket.setPosition(loc.toVector());
        this.spawnPacket.setYaw(this.yaw);
        this.spawnPacket.setPitch(this.pitch);

        if (this.data != null) {
            this.spawnPacket.setMetadata(this.data);
        }

        for (Player player : players) {
            if (tabMode != 0) {
                sendTabAdd(player);
            }
            this.spawnPacket.sendPacket(player);

            if (tabMode == 1) {
                sendTabRemove(player);
            }
        }
    }

    @Override
    public void respawn(Player... players) {
        this.visible = players;
        this.spawnPacket.setMetadata(this.data);
        this.spawnPacket.setPosition(this.location.toVector());
        this.spawnPacket.setYaw(this.yaw);
        this.spawnPacket.setPitch(this.pitch);

        for (Player player : this.visible) {
            this.spawnPacket.sendPacket(player);

            for (com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment equipment : this.lastEquipment) {
                PacketEventsUtil.sendPacket(player, equipment);
            }
        }

        this.look(this.yaw, this.pitch);
        this.updateMetadata();
    }

    @Override
    public void despawn() {
        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(this.id);

        for (Player player : this.visible) {
            if (player != null) {
                PacketEventsUtil.sendPacket(player, destroyPacket);
            }
        }

        Arrays.fill(this.visible, null);
    }

    @Override
    public void remove() {
        NPCManager.names.remove(this.name);

        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(this.id);

        if (this.oldVisible != null) {
            if (this.tabMode == 2) {
                sendTabRemove(this.oldVisible);
            }

            PacketEventsUtil.sendPacket(this.oldVisible, destroyPacket);
        }
    }

    @Override
    public void teleport(Location loc, boolean onGround) {
        this.location = loc;

        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                this.id,
                new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
                loc.getYaw(),
                loc.getPitch(),
                onGround
        );

        for (Player player : this.visible) {
            if (player != null) {
                PacketEventsUtil.sendPacket(player, packet);
            }
        }
    }

    @Override
    public void move(Location loc, boolean onGround, float yaw, float pitch) {
        double dx = loc.getX() - this.location.getX();
        double dy = loc.getY() - this.location.getY();
        double dz = loc.getZ() - this.location.getZ();
        WrapperPlayServerEntityRelativeMoveAndRotation packet = new WrapperPlayServerEntityRelativeMoveAndRotation(
                this.id, dx, dy, dz, yaw, pitch, onGround);
        WrapperPlayServerEntityHeadLook head = new WrapperPlayServerEntityHeadLook(this.id, yaw);

        this.location = loc;
        this.yaw = yaw;
        this.pitch = pitch;

        for (Player player : this.visible) {
            if (player != null) {
                PacketEventsUtil.sendPacket(player, packet);
                PacketEventsUtil.sendPacket(player, head);
            }
        }
    }

    @Override
    public void look(float yaw, float pitch) {
        WrapperPlayServerEntityRotation lookPacket = new WrapperPlayServerEntityRotation(this.id, yaw, pitch, true);
        WrapperPlayServerEntityHeadLook head = new WrapperPlayServerEntityHeadLook(this.id, yaw);

        for (Player player : this.visible) {
            if (player != null) {
                PacketEventsUtil.sendPacket(player, lookPacket);
                PacketEventsUtil.sendPacket(player, head);
            }
        }
    }

    @Override
    public void updateMetadata() {
        if (this.data == null) {
            return;
        }
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(this.id, this.data);

        for (Player player : this.visible) {
            if (player != null) {
                PacketEventsUtil.sendPacket(player, packet);
            }
        }
    }

    @Override
    public void animate(int id) {
        if (id == 1 && VersionUtil.isAbove(VersionEnum.V1_20)) {
            hurt();
            return;
        }

        WrapperPlayServerEntityAnimation.EntityAnimationType[] types = WrapperPlayServerEntityAnimation.EntityAnimationType.values();
        WrapperPlayServerEntityAnimation.EntityAnimationType type = (id >= 0 && id < types.length)
                ? types[id]
                : WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM;
        WrapperPlayServerEntityAnimation packet = new WrapperPlayServerEntityAnimation(this.id, type);

        for (Player player : this.visible) {
            if (player != null) {
                PacketEventsUtil.sendPacket(player, packet);
            }
        }
    }

    private void hurt() {
        WrapperPlayServerHurtAnimation packet = new WrapperPlayServerHurtAnimation(this.id, this.yaw);
        for (Player player : this.visible) {
            if (player != null) {
                PacketEventsUtil.sendPacket(player, packet);
            }
        }
    }

    @Override
    public void sleep(Location loc) {
        WrapperPlayServerUseBed packet = new WrapperPlayServerUseBed(this.id,
                new Vector3i(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));

        for (Player player : this.visible) {
            if (player != null) {
                PacketEventsUtil.sendPacket(player, packet);
            }
        }
    }

    @Override
    public void addToTeam(String team) {
        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(team, WrapperPlayServerTeams.TeamMode.ADD_ENTITIES,
                (WrapperPlayServerTeams.ScoreBoardTeamInfo) null, Collections.singletonList(this.name));

        for (Player player : this.visible) {
            if (player != null) {
                PacketEventsUtil.sendPacket(player, packet);
            }
        }
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public void setData(List<EntityData<?>> data) {
        this.data = data;
    }

    @Override
    public List<EntityData<?>> getData() {
        return data;
    }

    @Override
    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }

    @Override
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    @Override
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public void setOrigin(Location origin) {
        this.origin = origin;
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public Location getOrigin() {
        return origin;
    }

    @Override
    public Player[] getVisible() {
        return this.visible;
    }

    @Override
    public void setLastEquipment(List<com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment> list) {
        this.lastEquipment = list;
    }
}
