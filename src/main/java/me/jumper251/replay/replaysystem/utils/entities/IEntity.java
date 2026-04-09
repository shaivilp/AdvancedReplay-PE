package me.jumper251.replay.replaysystem.utils.entities;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;

public interface IEntity {

    void spawn(Location loc, Player... players);

    void respawn(Player... players);

    void despawn();

    void remove();

    void teleport(Location loc, boolean onGround);

    void move(Location loc, boolean onGround, float yaw, float pitch);

    void look(float yaw, float pitch);

    void updateMetadata();

    void animate(int id);

    int getId();

    void setId(int id);

    void setData(List<EntityData<?>> data);

    List<EntityData<?>> getData();

    void setPitch(float pitch);

    void setYaw(float yaw);

    Location getLocation();

    void setOrigin(Location origin);

    void setLocation(Location location);

    Location getOrigin();

    Player[] getVisible();
}
