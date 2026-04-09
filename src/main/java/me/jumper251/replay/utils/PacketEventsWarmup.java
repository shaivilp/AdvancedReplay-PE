package me.jumper251.replay.utils;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import me.jumper251.replay.replaysystem.utils.entities.PacketNPC;

import java.util.Collections;
import java.util.Optional;

public final class PacketEventsWarmup {

    private PacketEventsWarmup() {
    }

    public static void prepare() {
        new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.ADD_PLAYER, Collections.emptyList());
        new WrapperPlayServerSpawnEntity(0, Optional.empty(), EntityTypes.PLAYER, new Vector3d(0, 0, 0),
                0f, 0f, 0f, 0, Optional.empty());
        new WrapperPlayServerEntityMetadata(0, Collections.emptyList());
        PacketNPC npc = new PacketNPC();
        npc.setData(Collections.emptyList());
        npc.look(0, 0);
    }
}
