package me.jumper251.replay.replaysystem.utils;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.jumper251.replay.replaysystem.data.types.InvData;
import me.jumper251.replay.replaysystem.data.types.ItemData;
import me.jumper251.replay.replaysystem.data.types.SerializableItemStack;
import me.jumper251.replay.utils.version.MaterialBridge;
import me.jumper251.replay.utils.VersionUtil;
import me.jumper251.replay.utils.VersionUtil.VersionEnum;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NPCManager {

    private static final List<Material> ARMOR = new ArrayList<>();

    public static List<String> names = new ArrayList<>();

    static {
        ARMOR.add(Material.DIAMOND_HELMET);
        ARMOR.add(Material.DIAMOND_CHESTPLATE);
        ARMOR.add(Material.DIAMOND_LEGGINGS);
        ARMOR.add(Material.DIAMOND_BOOTS);
        ARMOR.add(Material.IRON_HELMET);
        ARMOR.add(Material.IRON_CHESTPLATE);
        ARMOR.add(Material.IRON_LEGGINGS);
        ARMOR.add(Material.IRON_BOOTS);
        ARMOR.add(Material.CHAINMAIL_HELMET);
        ARMOR.add(Material.CHAINMAIL_CHESTPLATE);
        ARMOR.add(Material.CHAINMAIL_LEGGINGS);
        ARMOR.add(Material.CHAINMAIL_BOOTS);
        ARMOR.add(MaterialBridge.GOLDEN_HELMET.toMaterial());
        ARMOR.add(MaterialBridge.GOLDEN_CHESTPLATE.toMaterial());
        ARMOR.add(MaterialBridge.GOLDEN_LEGGINGS.toMaterial());
        ARMOR.add(MaterialBridge.GOLDEN_BOOTS.toMaterial());
        ARMOR.add(Material.LEATHER_HELMET);
        ARMOR.add(Material.LEATHER_CHESTPLATE);
        ARMOR.add(Material.LEATHER_LEGGINGS);
        ARMOR.add(Material.LEATHER_BOOTS);

        if (VersionUtil.isAbove(VersionEnum.V1_16)) {
            ARMOR.add(Material.NETHERITE_HELMET);
            ARMOR.add(Material.NETHERITE_CHESTPLATE);
            ARMOR.add(Material.NETHERITE_LEGGINGS);
            ARMOR.add(Material.NETHERITE_BOOTS);
        }
    }

    private static ItemStack peStack(org.bukkit.inventory.ItemStack bukkit) {
        return SpigotConversionUtil.fromBukkitItemStack(bukkit);
    }

    public static List<WrapperPlayServerEntityEquipment> updateEquipmentv16(int id, InvData data) {
        List<Equipment> items = new ArrayList<>();
        items.add(new Equipment(EquipmentSlot.HELMET, peStack(fromID(data.getHead()))));
        items.add(new Equipment(EquipmentSlot.CHEST_PLATE, peStack(fromID(data.getChest()))));
        items.add(new Equipment(EquipmentSlot.LEGGINGS, peStack(fromID(data.getLeg()))));
        items.add(new Equipment(EquipmentSlot.BOOTS, peStack(fromID(data.getBoots()))));
        items.add(new Equipment(EquipmentSlot.MAIN_HAND, peStack(fromID(data.getMainHand()))));
        items.add(new Equipment(EquipmentSlot.OFF_HAND, peStack(fromID(data.getOffHand()))));
        return Collections.singletonList(new WrapperPlayServerEntityEquipment(id, items));
    }

    public static List<WrapperPlayServerEntityEquipment> updateEquipment(int id, InvData data) {
        List<WrapperPlayServerEntityEquipment> list = new ArrayList<>();
        list.add(new WrapperPlayServerEntityEquipment(id, Collections.singletonList(new Equipment(EquipmentSlot.HELMET, peStack(fromID(data.getHead()))))));
        list.add(new WrapperPlayServerEntityEquipment(id, Collections.singletonList(new Equipment(EquipmentSlot.CHEST_PLATE, peStack(fromID(data.getChest()))))));
        list.add(new WrapperPlayServerEntityEquipment(id, Collections.singletonList(new Equipment(EquipmentSlot.LEGGINGS, peStack(fromID(data.getLeg()))))));
        list.add(new WrapperPlayServerEntityEquipment(id, Collections.singletonList(new Equipment(EquipmentSlot.BOOTS, peStack(fromID(data.getBoots()))))));
        list.add(new WrapperPlayServerEntityEquipment(id, Collections.singletonList(new Equipment(EquipmentSlot.MAIN_HAND, peStack(fromID(data.getMainHand()))))));
        if (!VersionUtil.isCompatible(VersionEnum.V1_8)) {
            list.add(new WrapperPlayServerEntityEquipment(id, Collections.singletonList(new Equipment(EquipmentSlot.OFF_HAND, peStack(fromID(data.getOffHand()))))));
        }
        return list;
    }

    /**
     * 1.8 equipment indices: 0 main, 1 boots, 2 legs, 3 chest, 4 head — map to {@link EquipmentSlot} legacy ids.
     */
    public static List<WrapperPlayServerEntityEquipment> updateEquipmentOld(int id, InvData data) {
        return Arrays.asList(
                new WrapperPlayServerEntityEquipment(id, Collections.singletonList(new Equipment(EquipmentSlot.MAIN_HAND, peStack(fromID(data.getMainHand()))))),
                new WrapperPlayServerEntityEquipment(id, Collections.singletonList(new Equipment(EquipmentSlot.BOOTS, peStack(fromID(data.getBoots()))))),
                new WrapperPlayServerEntityEquipment(id, Collections.singletonList(new Equipment(EquipmentSlot.LEGGINGS, peStack(fromID(data.getLeg()))))),
                new WrapperPlayServerEntityEquipment(id, Collections.singletonList(new Equipment(EquipmentSlot.CHEST_PLATE, peStack(fromID(data.getChest()))))),
                new WrapperPlayServerEntityEquipment(id, Collections.singletonList(new Equipment(EquipmentSlot.HELMET, peStack(fromID(data.getHead())))))
        );
    }

    public static org.bukkit.inventory.ItemStack fromID(ItemData data) {
        if (data == null) {
            return new org.bukkit.inventory.ItemStack(Material.AIR);
        }
        if (data.getItemStack() != null) {
            return data.getItemStack().toItemStack();
        }
        return new org.bukkit.inventory.ItemStack(MaterialBridge.fromID(data.getId()), 1, (short) data.getSubId());
    }

    public static ItemData fromItemStack(org.bukkit.inventory.ItemStack stack) {
        if (stack == null) {
            return null;
        }
        return new ItemData(SerializableItemStack.fromItemStack(stack));
    }

    @SuppressWarnings("deprecation")
    public static InvData copyFromPlayer(Player player, boolean armor, boolean off) {
        InvData data = new InvData();
        if (VersionUtil.isCompatible(VersionEnum.V1_8)) {
            data.setMainHand(fromItemStack(player.getItemInHand()));
        } else {
            data.setMainHand(fromItemStack(player.getInventory().getItemInMainHand()));
            if (off) {
                data.setOffHand(fromItemStack(player.getInventory().getItemInOffHand()));
            }
        }
        if (armor) {
            data.setHead(fromItemStack(player.getInventory().getHelmet()));
            data.setChest(fromItemStack(player.getInventory().getChestplate()));
            data.setLeg(fromItemStack(player.getInventory().getLeggings()));
            data.setBoots(fromItemStack(player.getInventory().getBoots()));
        }
        return data;
    }

    public static boolean isArmor(org.bukkit.inventory.ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return ARMOR.contains(stack.getType());
    }

    public static String getMaterialName(org.bukkit.inventory.ItemStack stack) {
        return stack.getType().name();
    }

    public static String getArmorType(org.bukkit.inventory.ItemStack stack) {
        if (stack == null) {
            return null;
        }
        if (getMaterialName(stack).contains("HELMET")) {
            return "head";
        }
        if (getMaterialName(stack).contains("CHESTPLATE")) {
            return "chest";
        }
        if (getMaterialName(stack).contains("LEGGINGS")) {
            return "leg";
        }
        if (getMaterialName(stack).contains("BOOTS")) {
            return "boots";
        }
        return null;
    }

    public static boolean wearsArmor(Player p, String type) {
        PlayerInventory inv = p.getInventory();
        if (type == null) {
            return false;
        }
        if (type.equals("head") && isArmor(inv.getHelmet())) {
            return true;
        }
        if (type.equals("chest") && isArmor(inv.getChestplate())) {
            return true;
        }
        if (type.equals("leg") && isArmor(inv.getLeggings())) {
            return true;
        }
        return type.equals("boots") && isArmor(inv.getBoots());
    }
}
