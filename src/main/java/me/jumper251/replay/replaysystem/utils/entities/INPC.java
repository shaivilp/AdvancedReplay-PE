package me.jumper251.replay.replaysystem.utils.entities;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;

public interface INPC {

    void spawn(Location loc, int tabMode, Player... players);

    void addViewer(Player player);

    void removeViewer(Player player);

    void respawn(Player... players);

    void despawn();

    void remove();

    void teleport(Location loc, boolean onGround);

    void move(Location loc, boolean onGround, float yaw, float pitch);

    void look(float yaw, float pitch);

    void updateMetadata();

    void animate(int id);

    void sleep(Location loc);

    void addToTeam(String team);

    int getId();

    String getName();

    UUID getUuid();

    void setId(int id);

    void setName(String name);

    void setUuid(UUID uuid);

    void setData(List<EntityData<?>> data);

    List<EntityData<?>> getData();

    void setProfile(UserProfile profile);

    void setPitch(float pitch);

    void setYaw(float yaw);

    Location getLocation();

    void setOrigin(Location origin);

    void setLocation(Location location);

    Location getOrigin();

    Player[] getVisible();

    void setLastEquipment(List<WrapperPlayServerEntityEquipment> list);
}
