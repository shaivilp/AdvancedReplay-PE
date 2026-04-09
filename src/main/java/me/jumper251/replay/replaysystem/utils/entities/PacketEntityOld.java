package me.jumper251.replay.replaysystem.utils.entities;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import me.jumper251.replay.utils.MathUtils;
import me.jumper251.replay.utils.PacketEventsUtil;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PacketEntityOld implements IEntity {

    private int id;

    private List<EntityData<?>> data;

    private Location location;

    private Location origin;

    private SpawnPacket spawnPacket;

    private float yaw;

    private float pitch;

    private Player[] visible;

    private Player oldVisible;

    private EntityType type;

    public PacketEntityOld(int id, EntityType type) {
        this.id = id;
        this.spawnPacket = new SpawnPacket(true, type);
        this.type = type;
    }

    public PacketEntityOld(EntityType type) {
        this(MathUtils.randInt(50000, 400000), type);
    }

    @Override
    public void spawn(Location loc, Player... players) {
        this.visible = players;
        this.oldVisible = players[0];
        this.location = loc;
        this.origin = loc;

        this.spawnPacket.setEntityID(this.id);
        this.spawnPacket.setType(this.type);
        this.spawnPacket.setUniqueId(UUID.randomUUID());
        this.spawnPacket.setX(this.location.getX());
        this.spawnPacket.setY(this.location.getY());
        this.spawnPacket.setZ(this.location.getZ());
        this.spawnPacket.setYaw(this.yaw);
        this.spawnPacket.setPitch(this.pitch);

        if (this.data != null) {
            this.spawnPacket.setMetadata(this.data);
        }

        for (Player player : Arrays.asList(players)) {
            this.spawnPacket.sendPacket(player);
        }
    }

    @Override
    public void respawn(Player... players) {
    }

    @Override
    public void despawn() {
        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(this.id);
        for (Player player : Arrays.asList(this.visible)) {
            if (player != null) {
                PacketEventsUtil.sendPacket(player, destroyPacket);
            }
        }
        Arrays.fill(this.visible, null);
    }

    @Override
    public void remove() {
        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(this.id);
        if (this.oldVisible != null) {
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
        for (Player player : Arrays.asList(this.visible)) {
            if (player != null) {
                PacketEventsUtil.sendPacket(player, packet);
            }
        }
    }

    @Override
    public void move(Location loc, boolean onGround, float yaw, float pitch) {
        WrapperPlayServerEntityHeadLook head = new WrapperPlayServerEntityHeadLook(this.id, yaw);
        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(
                this.id,
                new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
                yaw,
                pitch,
                onGround
        );
        this.location = loc;
        for (Player player : Arrays.asList(this.visible)) {
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
        for (Player player : Arrays.asList(this.visible)) {
            if (player != null) {
                PacketEventsUtil.sendPacket(player, lookPacket);
                PacketEventsUtil.sendPacket(player, head);
            }
        }
    }

    @Override
    public void updateMetadata() {
    }

    @Override
    public void animate(int id) {
        WrapperPlayServerEntityStatus packet = new WrapperPlayServerEntityStatus(this.id, id);
        for (Player player : Arrays.asList(this.visible)) {
            if (player != null) {
                PacketEventsUtil.sendPacket(player, packet);
            }
        }
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public void setData(List<EntityData<?>> data) {
        this.data = data;
    }

    @Override
    public List<EntityData<?>> getData() {
        return this.data;
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
        return this.location;
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
        return this.origin;
    }

    @Override
    public Player[] getVisible() {
        return this.visible;
    }
}
