package me.jumper251.replay.replaysystem.data.types;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.github.retrooper.packetevents.protocol.sound.Sounds;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import me.jumper251.replay.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExplosionData extends PacketData implements Serializable {

    private static final long serialVersionUID = 2L;

    private LocationData location;
    private float strength;
    private List<BlockPositionData> affectedBlocks;
    private float playerMotionX;
    private float playerMotionY;
    private float playerMotionZ;
    private String blockInteraction;
    private String smallParticleTypeName;
    private String largeParticleTypeName;
    private String explosionSoundId;

    public ExplosionData(LocationData location, float strength, List<BlockPositionData> affectedBlocks,
                         float playerMotionX, float playerMotionY, float playerMotionZ, String blockInteraction,
                         String smallParticleTypeName, String largeParticleTypeName, String explosionSoundId) {
        this.location = location;
        this.strength = strength;
        this.affectedBlocks = affectedBlocks;
        this.playerMotionX = playerMotionX;
        this.playerMotionY = playerMotionY;
        this.playerMotionZ = playerMotionZ;
        this.blockInteraction = blockInteraction;
        this.smallParticleTypeName = smallParticleTypeName;
        this.largeParticleTypeName = largeParticleTypeName;
        this.explosionSoundId = explosionSoundId;
    }

    public LocationData getLocation() {
        return location;
    }

    public float getStrength() {
        return strength;
    }

    public List<BlockPositionData> getAffectedBlocks() {
        return affectedBlocks;
    }

    public float getPlayerMotionX() {
        return playerMotionX;
    }

    public float getPlayerMotionY() {
        return playerMotionY;
    }

    public float getPlayerMotionZ() {
        return playerMotionZ;
    }

    public String getBlockInteraction() {
        return blockInteraction;
    }

    public String getSmallParticleTypeName() {
        return smallParticleTypeName;
    }

    public String getLargeParticleTypeName() {
        return largeParticleTypeName;
    }

    public String getExplosionSoundId() {
        return explosionSoundId;
    }

    public void setBlockInteraction(String blockInteraction) {
        this.blockInteraction = blockInteraction;
    }

    public void setLocation(LocationData location) {
        this.location = location;
    }

    public void setStrength(float strength) {
        this.strength = strength;
    }

    public void setAffectedBlocks(List<BlockPositionData> affectedBlocks) {
        this.affectedBlocks = affectedBlocks;
    }

    public void setPlayerMotionX(float playerMotionX) {
        this.playerMotionX = playerMotionX;
    }

    public void setPlayerMotionY(float playerMotionY) {
        this.playerMotionY = playerMotionY;
    }

    public void setPlayerMotionZ(float playerMotionZ) {
        this.playerMotionZ = playerMotionZ;
    }

    public void setSmallParticleTypeName(String smallParticleTypeName) {
        this.smallParticleTypeName = smallParticleTypeName;
    }

    public void setLargeParticleTypeName(String largeParticleTypeName) {
        this.largeParticleTypeName = largeParticleTypeName;
    }

    public void setExplosionSoundId(String explosionSoundId) {
        this.explosionSoundId = explosionSoundId;
    }

    public static ExplosionData fromPacket(PacketSendEvent event) {
        WrapperPlayServerExplosion w = new WrapperPlayServerExplosion(event);
        Player p = Bukkit.getPlayer(event.getUser().getUUID());
        String world = p != null && p.getWorld() != null ? p.getWorld().getName() : "";
        Vector3d pos = w.getPosition();
        LocationData location = new LocationData(pos.getX(), pos.getY(), pos.getZ(), world);
        float strength = w.getStrength();
        List<BlockPositionData> affectedBlocks = new ArrayList<>();
        for (Object o : w.getRecords()) {
            if (o instanceof Vector3i) {
                Vector3i v = (Vector3i) o;
                affectedBlocks.add(new BlockPositionData(v.getX(), v.getY(), v.getZ()));
            }
        }
        Vector3d kb = w.getKnockback();
        float mx = kb != null ? (float) kb.x : 0f;
        float my = kb != null ? (float) kb.y : 0f;
        float mz = kb != null ? (float) kb.z : 0f;

        if (VersionUtil.isAbove(VersionUtil.VersionEnum.V1_20)) {
            String bi = null;
            try {
                if (w.getBlockInteraction() != null) {
                    bi = w.getBlockInteraction().name();
                }
            } catch (@SuppressWarnings("deprecation") Throwable ignored) {
            }
            Particle small = w.getSmallExplosionParticles();
            Particle large = w.getLargeExplosionParticles();
            Sound sound = w.getExplosionSound();
            return new ExplosionData(location, strength, affectedBlocks, mx, my, mz, bi,
                    small != null && small.getType() != null ? small.getType().getName().toString() : null,
                    large != null && large.getType() != null ? large.getType().getName().toString() : null,
                    sound != null && sound.getSoundId() != null ? sound.getSoundId().toString() : null);
        }
        return new ExplosionData(location, strength, affectedBlocks, mx, my, mz, null, null, null, null);
    }

    public WrapperPlayServerExplosion toPacketWrapper() {
        LocationData loc = getLocation();
        Vector3d position = new Vector3d(loc.getX(), loc.getY(), loc.getZ());
        List<Vector3i> records = getAffectedBlocks().stream()
                .map(bp -> new Vector3i(bp.getX(), bp.getY(), bp.getZ()))
                .collect(Collectors.toList());
        Vector3d motion = new Vector3d(getPlayerMotionX(), getPlayerMotionY(), getPlayerMotionZ());

        Particle small = new Particle<>(ParticleTypes.EXPLOSION);
        Particle large = new Particle<>(ParticleTypes.EXPLOSION_EMITTER);
        Sound sound = Sounds.ENTITY_GENERIC_EXPLODE;
        WrapperPlayServerExplosion.BlockInteraction interaction = WrapperPlayServerExplosion.BlockInteraction.DESTROY_BLOCKS;

        if (VersionUtil.isAbove(VersionUtil.VersionEnum.V1_20) && getSmallParticleTypeName() != null) {
            try {
                small = new Particle<>(com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes.getByName(getSmallParticleTypeName()));
            } catch (Throwable ignored) {
            }
        }
        if (VersionUtil.isAbove(VersionUtil.VersionEnum.V1_20) && getLargeParticleTypeName() != null) {
            try {
                large = new Particle<>(com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes.getByName(getLargeParticleTypeName()));
            } catch (Throwable ignored) {
            }
        }
        if (VersionUtil.isAbove(VersionUtil.VersionEnum.V1_20) && getExplosionSoundId() != null) {
            try {
                sound = com.github.retrooper.packetevents.protocol.sound.Sounds.getByName(getExplosionSoundId());
            } catch (Throwable ignored) {
            }
        }
        if (VersionUtil.isAbove(VersionUtil.VersionEnum.V1_20) && getBlockInteraction() != null) {
            try {
                interaction = WrapperPlayServerExplosion.BlockInteraction.valueOf(getBlockInteraction());
            } catch (IllegalArgumentException ignored) {
            }
        }

        return new WrapperPlayServerExplosion(
                position,
                getStrength(),
                records,
                motion,
                small,
                large,
                interaction,
                sound,
                new com.github.retrooper.packetevents.protocol.util.WeightedList<>()
        );
    }

    @Override
    public String toString() {
        return "ExplosionData{" +
                "location=" + location +
                ", strength=" + strength +
                ", affectedBlocks=" + affectedBlocks +
                ", playerMotionX=" + playerMotionX +
                ", playerMotionY=" + playerMotionY +
                ", playerMotionZ=" + playerMotionZ +
                ", blockInteraction=" + blockInteraction +
                ", smallParticleTypeName=" + smallParticleTypeName +
                ", largeParticleTypeName=" + largeParticleTypeName +
                ", explosionSoundId='" + explosionSoundId + '\'' +
                '}';
    }
}
