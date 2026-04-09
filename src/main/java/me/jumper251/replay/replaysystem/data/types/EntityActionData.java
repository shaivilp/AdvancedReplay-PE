package me.jumper251.replay.replaysystem.data.types;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

public class EntityActionData extends PacketData {

    private static final long serialVersionUID = 7841723539864388570L;

    private WrapperPlayClientEntityAction.Action action;

    public EntityActionData(WrapperPlayClientEntityAction.Action action) {
        this.action = action;
    }

    public WrapperPlayClientEntityAction.Action getAction() {
        return action;
    }
}
