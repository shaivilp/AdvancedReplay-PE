package me.jumper251.replay.replaysystem.utils.entities;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import me.jumper251.replay.utils.PacketEventsUtil;
import me.jumper251.replay.utils.VersionUtil;
import me.jumper251.replay.utils.VersionUtil.VersionEnum;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class NPCSpawnPacket {

    private final boolean modern;

    private final WrapperPlayServerSpawnEntity spawnEntity;

    private final WrapperPlayServerSpawnLivingEntity spawnLiving;

    private int entityId;

    private UUID uuid;

    private double x;

    private double y;

    private double z;

    private float yaw;

    private float pitch;

    private List<EntityData<?>> metadata = new ArrayList<>();

    private List<EntityData<?>> deferredMetadata = new ArrayList<>();

    public NPCSpawnPacket(boolean modern) {
        this.modern = modern;
        if (modern) {
            this.spawnEntity = new WrapperPlayServerSpawnEntity(0, Optional.empty(), EntityTypes.PLAYER, Vector3d.zero(),
                    0f, 0f, 0f, 0, Optional.empty());
            this.spawnLiving = null;
        } else {
            this.spawnLiving = new WrapperPlayServerSpawnLivingEntity(0, UUID.randomUUID(), EntityTypes.PLAYER, Vector3d.zero(),
                    0f, 0f, 0f, Vector3d.zero(), new ArrayList<>());
            this.spawnEntity = null;
        }
    }

    public void setEntityID(int id) {
        this.entityId = id;
        if (modern) {
            spawnEntity.setEntityId(id);
        } else {
            spawnLiving.setEntityId(id);
        }
    }

    public void setPosition(Vector position) {
        setX(position.getX());
        setY(position.getY());
        setZ(position.getZ());
    }

    public void setMetadata(List<EntityData<?>> data) {
        this.metadata = data != null ? new ArrayList<>(data) : new ArrayList<>();
        if (!modern && VersionUtil.isBelow(VersionEnum.V1_15)) {
            spawnLiving.setEntityMetadata(metadata);
            deferredMetadata = new ArrayList<>();
        } else {
            deferredMetadata = new ArrayList<>(metadata);
            if (!modern) {
                spawnLiving.setEntityMetadata(new ArrayList<>());
            }
        }
    }

    public void setX(double x) {
        this.x = x;
        applyPosition();
    }

    public void setY(double y) {
        this.y = y;
        applyPosition();
    }

    public void setZ(double z) {
        this.z = z;
        applyPosition();
    }

    private void applyPosition() {
        Vector3d pos = new Vector3d(x, y, z);
        if (modern) {
            spawnEntity.setPosition(pos);
        } else {
            spawnLiving.setPosition(pos);
        }
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        if (modern) {
            spawnEntity.setYaw(yaw);
            spawnEntity.setHeadYaw(yaw);
        } else {
            spawnLiving.setYaw(yaw);
        }
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        if (modern) {
            spawnEntity.setPitch(pitch);
        } else {
            spawnLiving.setPitch(pitch);
            spawnLiving.setHeadPitch(pitch);
        }
    }

    public void setUniqueId(UUID uuid) {
        this.uuid = uuid;
        if (modern) {
            spawnEntity.setUUID(Optional.of(uuid));
        } else {
            spawnLiving.setEntityUUID(uuid);
        }
    }

    public void sendPacket(Player player) {
        if (modern) {
            spawnEntity.setEntityType(EntityTypes.PLAYER);
            PacketEventsUtil.sendPacket(player, spawnEntity);
        } else {
            spawnLiving.setEntityType(EntityTypes.PLAYER);
            PacketEventsUtil.sendPacket(player, spawnLiving);
        }
        if (!deferredMetadata.isEmpty()) {
            PacketEventsUtil.sendPacket(player, new WrapperPlayServerEntityMetadata(entityId, deferredMetadata));
        }
    }

    public boolean isOld() {
        return !modern;
    }
}
