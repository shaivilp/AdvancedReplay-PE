package me.jumper251.replay.replaysystem.utils.entities;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import me.jumper251.replay.replaysystem.data.types.FishingData;
import me.jumper251.replay.replaysystem.data.types.LocationData;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.UUID;

public final class FishingUtils {

    private FishingUtils() {
    }

    public static WrapperPlayServerSpawnEntity createHookPacket(FishingData fishing, int throwerId, int entityId) {
        Location loc = LocationData.toLocation(fishing.getLocation());
        Vector vel = new Vector(fishing.getX(), fishing.getY(), fishing.getZ());
        Vector3d velocity = new Vector3d(vel.getX(), vel.getY(), vel.getZ());
        return new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.FISHING_BOBBER,
                new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
                loc.getPitch(),
                loc.getYaw(),
                loc.getYaw(),
                throwerId,
                Optional.of(velocity)
        );
    }
}
