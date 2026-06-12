package me.jumper251.replay.replaysystem.utils.entities;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
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

    private final WrapperPlayServerSpawnPlayer spawnPlayer;

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
            this.spawnPlayer = null;
        } else {
            // Players must be spawned with the dedicated Spawn Player packet on < 1.20.2.
            // The spawn-mob packet (SpawnLivingEntity) does not render a player on legacy
            // clients such as 1.8, which is why the replay NPCs were invisible.
            this.spawnPlayer = new WrapperPlayServerSpawnPlayer(0, UUID.randomUUID(), Vector3d.zero(),
                    0f, 0f, new ArrayList<>());
            this.spawnEntity = null;
        }
    }

    public void setEntityID(int id) {
        this.entityId = id;
        if (modern) {
            spawnEntity.setEntityId(id);
        } else {
            spawnPlayer.setEntityId(id);
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
            // Pre-1.15 the Spawn Player packet still carries the entity metadata inline.
            spawnPlayer.setEntityMetadata(metadata);
            deferredMetadata = new ArrayList<>();
        } else {
            // 1.15+ removed metadata from the spawn packet, so it is sent separately.
            deferredMetadata = new ArrayList<>(metadata);
            if (!modern) {
                spawnPlayer.setEntityMetadata(new ArrayList<>());
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
            spawnPlayer.setPosition(pos);
        }
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        if (modern) {
            spawnEntity.setYaw(yaw);
            spawnEntity.setHeadYaw(yaw);
        } else {
            spawnPlayer.setYaw(yaw);
        }
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        if (modern) {
            spawnEntity.setPitch(pitch);
        } else {
            spawnPlayer.setPitch(pitch);
        }
    }

    public void setUniqueId(UUID uuid) {
        this.uuid = uuid;
        if (modern) {
            spawnEntity.setUUID(Optional.of(uuid));
        } else {
            spawnPlayer.setUUID(uuid);
        }
    }

    public void sendPacket(Player player) {
        if (modern) {
            spawnEntity.setEntityType(EntityTypes.PLAYER);
            PacketEventsUtil.sendPacket(player, spawnEntity);
        } else {
            PacketEventsUtil.sendPacket(player, spawnPlayer);
        }
        if (!deferredMetadata.isEmpty()) {
            PacketEventsUtil.sendPacket(player, new WrapperPlayServerEntityMetadata(entityId, deferredMetadata));
        }
    }

    public boolean isOld() {
        return !modern;
    }
}
