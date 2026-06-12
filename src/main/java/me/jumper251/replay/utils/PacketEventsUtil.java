package me.jumper251.replay.utils;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.bukkit.entity.Player;

public final class PacketEventsUtil {

    private PacketEventsUtil() {
    }

    /**
     * Spigot's {@code PlayerManager} expects a Bukkit {@link Player} for channel lookup;
     * passing a PE {@code User} causes {@code ClassCastException} in {@code PlayerManagerImpl#getChannel}.
     */
    public static void sendPacket(Player player, PacketWrapper<?> wrapper) {
        if (player == null || wrapper == null) {
            return;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, wrapper);
    }
}
