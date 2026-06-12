package me.jumper251.replay.replaysystem.utils;


import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataType;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.jumper251.replay.utils.VersionUtil;
import me.jumper251.replay.utils.VersionUtil.VersionEnum;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class MetadataBuilder {

    private final List<EntityData<?>> entries;

    public MetadataBuilder(Entity en) {
        List<EntityData<?>> from = SpigotConversionUtil.getEntityMetadata(en);
        this.entries = new ArrayList<>();
        for (EntityData<?> ed : from) {
            this.entries.add(cloneEntry(ed));
        }
    }

    public MetadataBuilder(List<EntityData<?>> data) {
        this.entries = new ArrayList<>();
        for (EntityData<?> ed : data) {
            this.entries.add(cloneEntry(ed));
        }
    }

    public MetadataBuilder() {
        this.entries = new ArrayList<>();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EntityData<?> cloneEntry(EntityData<?> ed) {
        return new EntityData(ed.getIndex(), ed.getType(), ed.getValue());
    }

    private void removeIndex(int index) {
        entries.removeIf(e -> e.getIndex() == index);
    }

    private <T> void put(int index, EntityDataType<T> type, T value) {
        removeIndex(index);
        entries.add(new EntityData<>(index, type, value));
    }

    public MetadataBuilder setValue(int index, Object value) {
        if (value instanceof Byte) {
            put(index, EntityDataTypes.BYTE, (Byte) value);
        } else if (value instanceof Integer) {
            put(index, EntityDataTypes.INT, (Integer) value);
        } else if (value instanceof Float) {
            put(index, EntityDataTypes.FLOAT, (Float) value);
        } else if (value instanceof Boolean) {
            put(index, EntityDataTypes.BOOLEAN, (Boolean) value);
        } else {
            throw new IllegalArgumentException("Unsupported metadata value type: " + value.getClass());
        }
        return this;
    }

    public MetadataBuilder setByte(int index, byte value) {
        put(index, EntityDataTypes.BYTE, value);
        return this;
    }

    public MetadataBuilder setBoolean(int index, boolean value) {
        put(index, EntityDataTypes.BOOLEAN, value);
        return this;
    }

    public MetadataBuilder setInteger(int index, int value) {
        put(index, EntityDataTypes.INT, value);
        return this;
    }

    public MetadataBuilder setInvisible() {
        return setValue(0, (byte) 0x20);
    }

    public MetadataBuilder setCrouched() {
        return setValue(0, (byte) 0x02);
    }

    public MetadataBuilder resetValue() {
        if (VersionUtil.isAbove(VersionEnum.V1_14)) {
            return setByte(0, (byte) 0).setPoseField("STANDING");
        }
        return setValue(0, (byte) 0);
    }

    public MetadataBuilder setArrows(int amount) {
        if (VersionUtil.isBetween(VersionEnum.V1_10, VersionEnum.V1_13)) {
            return setValue(10, amount);
        } else if (VersionUtil.isBetween(VersionEnum.V1_14, VersionEnum.V1_16)) {
            return setValue(11, amount);
        } else if (VersionUtil.isAbove(VersionEnum.V1_17)) {
            return setValue(12, amount);
        }
        return setValue(9, amount);
    }

    public MetadataBuilder setGlowing() {
        return setValue(0, (byte) 0x20);
    }

    public MetadataBuilder setSilent() {
        return setValue(4, true);
    }

    public MetadataBuilder setNoGravity() {
        return setValue(5, true);
    }

    public MetadataBuilder setHealth(float amount) {
        return setValue(7, amount);
    }

    public MetadataBuilder setAir(int amount) {
        return setValue(1, amount);
    }

    public MetadataBuilder enableSkinParts() {
        if (VersionUtil.isBetween(VersionEnum.V1_9, VersionEnum.V1_20)) {
            return this;
        }
        byte skinFlags = (byte) (0x01 | 0x02 | 0x04 | 0x08 | 0x10 | 0x20 | 0x40);
        if (VersionUtil.isCompatible(VersionEnum.V1_8)) {
            return setValue(10, skinFlags);
        } else if (VersionUtil.isBetween(VersionEnum.V1_9, VersionEnum.V1_21)) {
            return setByte(17, skinFlags);
        }
        return setByte(16, skinFlags);
    }

    public List<EntityData<?>> getData() {
        enableSkinParts();
        return new ArrayList<>(entries);
    }

    public MetadataBuilder setPoseField(String type) {
        if (!VersionUtil.isAbove(VersionEnum.V1_14)) {
            return this;
        }
        EntityPose pose;
        if ("SNEAKING".equals(type)) {
            pose = EntityPose.CROUCHING;
        } else {
            pose = EntityPose.valueOf(type);
        }
        put(6, EntityDataTypes.ENTITY_POSE, pose);
        return this;
    }
}
