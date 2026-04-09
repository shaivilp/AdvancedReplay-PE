package me.jumper251.replay.replaysystem.utils.entities;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.jumper251.replay.utils.PacketEventsUtil;
import me.jumper251.replay.utils.VersionUtil;
import me.jumper251.replay.utils.VersionUtil.VersionEnum;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SpawnPacket {

    private final boolean legacyLiving;

    private final WrapperPlayServerSpawnLivingEntity spawnEntityLiving;

    private final WrapperPlayServerSpawnEntity spawnEntity;

    private int entityId;

    private List<EntityData<?>> deferredMetadata = new ArrayList<>();

    public SpawnPacket(boolean legacyLiving, org.bukkit.entity.EntityType bukkitType) {
        this.legacyLiving = legacyLiving;
        EntityType peType = SpigotConversionUtil.fromBukkitEntityType(bukkitType);
        if (peType == null) {
            peType = com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.ZOMBIE;
        }
        if (legacyLiving) {
            this.spawnEntityLiving = new WrapperPlayServerSpawnLivingEntity(0, UUID.randomUUID(), peType, Vector3d.zero(),
                    0f, 0f, 0f, Vector3d.zero(), new ArrayList<>());
            this.spawnEntity = null;
        } else {
            this.spawnEntity = new WrapperPlayServerSpawnEntity(0, Optional.empty(), peType, Vector3d.zero(),
                    0f, 0f, 0f, 0, Optional.empty());
            this.spawnEntityLiving = null;
        }
    }

    public void setEntityID(int id) {
        this.entityId = id;
        if (legacyLiving) {
            spawnEntityLiving.setEntityId(id);
        } else {
            spawnEntity.setEntityId(id);
        }
    }

    public void setType(org.bukkit.entity.EntityType type) {
        EntityType peType = SpigotConversionUtil.fromBukkitEntityType(type);
        if (peType == null) {
            return;
        }
        if (legacyLiving) {
            spawnEntityLiving.setEntityType(peType);
        } else {
            spawnEntity.setEntityType(peType);
        }
    }

    public void setMetadata(List<EntityData<?>> data) {
        List<EntityData<?>> list = data != null ? new ArrayList<>(data) : new ArrayList<>();
        if (legacyLiving && VersionUtil.isBelow(VersionEnum.V1_15)) {
            spawnEntityLiving.setEntityMetadata(list);
            deferredMetadata = new ArrayList<>();
        } else {
            deferredMetadata = new ArrayList<>(list);
            if (legacyLiving) {
                spawnEntityLiving.setEntityMetadata(new ArrayList<>());
            }
        }
    }

    public void setX(double x) {
        if (legacyLiving) {
            Vector3d p = spawnEntityLiving.getPosition();
            spawnEntityLiving.setPosition(new Vector3d(x, p.getY(), p.getZ()));
        } else {
            Vector3d p = spawnEntity.getPosition();
            spawnEntity.setPosition(new Vector3d(x, p.getY(), p.getZ()));
        }
    }

    public void setY(double y) {
        if (legacyLiving) {
            Vector3d p = spawnEntityLiving.getPosition();
            spawnEntityLiving.setPosition(new Vector3d(p.getX(), y, p.getZ()));
        } else {
            Vector3d p = spawnEntity.getPosition();
            spawnEntity.setPosition(new Vector3d(p.getX(), y, p.getZ()));
        }
    }

    public void setZ(double z) {
        if (legacyLiving) {
            Vector3d p = spawnEntityLiving.getPosition();
            spawnEntityLiving.setPosition(new Vector3d(p.getX(), p.getY(), z));
        } else {
            Vector3d p = spawnEntity.getPosition();
            spawnEntity.setPosition(new Vector3d(p.getX(), p.getY(), z));
        }
    }

    public void setYaw(float yaw) {
        if (legacyLiving) {
            spawnEntityLiving.setYaw(yaw);
        } else {
            spawnEntity.setYaw(yaw);
            spawnEntity.setHeadYaw(yaw);
        }
    }

    public void setPitch(float pitch) {
        if (legacyLiving) {
            spawnEntityLiving.setPitch(pitch);
            spawnEntityLiving.setHeadPitch(pitch);
        } else {
            spawnEntity.setPitch(pitch);
        }
    }

    public void setUniqueId(UUID uuid) {
        if (legacyLiving) {
            spawnEntityLiving.setEntityUUID(uuid);
        } else {
            spawnEntity.setUUID(Optional.of(uuid));
        }
    }

    public void sendPacket(Player player) {
        if (legacyLiving) {
            PacketEventsUtil.sendPacket(player, spawnEntityLiving);
        } else {
            PacketEventsUtil.sendPacket(player, spawnEntity);
        }
        if (!deferredMetadata.isEmpty()) {
            PacketEventsUtil.sendPacket(player, new WrapperPlayServerEntityMetadata(entityId, deferredMetadata));
        }
    }

    public boolean isOld() {
        return legacyLiving;
    }
}
