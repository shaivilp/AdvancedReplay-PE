package me.jumper251.replay.replaysystem.replaying;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleSubtitle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleText;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleTimes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTickingState;
import me.jumper251.replay.utils.PacketEventsUtil;
import net.kyori.adventure.text.Component;
import com.google.gson.Gson;
import me.jumper251.replay.filesystem.*;
import me.jumper251.replay.utils.version.MaterialBridge;
import me.jumper251.replay.utils.ReflectionHelper;
import me.jumper251.replay.utils.VersionUtil;
import me.jumper251.replay.utils.VersionUtil.VersionEnum;
import me.jumper251.replay.utils.fetcher.TextureInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.util.*;


public class ReplayHelper {

    private final static Gson GSON = new Gson();
    // Every viewer (owner + joiners) of an active replay maps to its shared Replayer.
    public static HashMap<String, Replayer> replaySessions = new HashMap<>();
    // Replay id -> the active Replayer, so /rp join can find a running synced session.
    public static HashMap<String, Replayer> activeReplayers = new HashMap<>();

    // Base64 textures for the control heads, applied directly on legacy versions where the
    // MHF_* owner-name skulls don't resolve (offline-mode 1.8 servers). Keyed by the owner name.
    private static final Map<String, String> LEGACY_HEAD_TEXTURES = new HashMap<>();

    static {
        LEGACY_HEAD_TEXTURES.put("MHF_ArrowRight", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDM0ZWYwNjM4NTM3MjIyYjIwZjQ4MDY5NGRhZGMwZjg1ZmJlMDc1OWQ1ODFhYTdmY2RmMmU0MzEzOTM3NzE1OCJ9fX0=");
        LEGACY_HEAD_TEXTURES.put("MHF_ArrowLeft", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjdhYWNhZDE5M2UyMjI2OTcxZWQ5NTMwMmRiYTQzMzQzOGJlNDY0NGZiYWI1ZWJmODE4MDU0MDYxNjY3ZmJlMiJ9fX0=");
        LEGACY_HEAD_TEXTURES.put("Push_red_button", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzM4NTFjZmI2NzE3Nzk1OTc0ZDMyMmU3YmI2OThlOWFiNTdlMWUwZTIyOTUwNDg2YTY5OWM4MThlMDJlOTBiMjkifX19");
    }

    public static ItemStack createItem(ItemConfigOption option) {
        String displayName = ChatColor.translateAlternateColorCodes('&', option.getName());

        ItemStack stack = createItem(option.getMaterial(), displayName, option.getData());


        if (stack.getItemMeta() instanceof SkullMeta) {

            SkullMeta meta = (SkullMeta) stack.getItemMeta();

            String legacyTexture = (option.getOwner() != null && VersionUtil.isBelow(VersionEnum.V1_18))
                    ? LEGACY_HEAD_TEXTURES.get(option.getOwner()) : null;

            if (legacyTexture != null) {
                // Pre-1.18 can't set skull textures via the Bukkit profile API, and owner-name heads
                // don't load on offline-mode servers, so apply the texture directly via NMS reflection.
                applyLegacyTexture(meta, legacyTexture);
            } else if (option.getOwner() != null) {
                meta.setOwner(option.getOwner());
            }

            if (option.getTexture() != null) {
                setSkullTexture(meta, option.getTexture());
            }
            meta.setDisplayName(displayName);
            stack.setItemMeta(meta);
        }

        return stack;
    }

    public static ItemStack createItem(Material material, String name, int data) {
        ItemStack stack = new ItemStack(material, 1, (byte) data);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);

        return stack;
    }

    public static ItemStack getPauseItem() {
        return createItem(ItemConfig.getItem(ItemConfigType.PAUSE));
    }

    public static ItemStack getResumeItem() {
        return createItem(ItemConfig.getItem(ItemConfigType.RESUME));
    }

    public static void createTeleporter(Player player, Replayer replayer, int page) {
        List<String> npcNames = new ArrayList<>(replayer.getNPCList().keySet());
        int size = (int) Math.ceil(npcNames.size() / 9.0) * 9;

        int pageSize = 9 * 5;
        int pages = (int) Math.ceil((double) size / pageSize);
        if (size <= (9 * 6)) {
            pages = 1;
            pageSize = size;
        }

        List<String> elements = npcNames.subList((page - 1) * pageSize, Math.min(page * pageSize, npcNames.size()));

        int inventorySize = Math.max(9, pages > 1 ? pageSize + 9 : pageSize);
        Inventory inv = Bukkit.createInventory(null, inventorySize, "§7Teleporter");

        int index = 0;

        for (String name : elements) {
            ItemStack stack = new ItemStack(MaterialBridge.PLAYER_HEAD.toMaterial(), 1, (short) 3);
            SkullMeta meta = (SkullMeta) stack.getItemMeta();
            meta.setDisplayName("§6" + name);
            meta.setOwner(name);
            stack.setItemMeta(meta);

            inv.setItem(index, stack);

            index++;
        }

        if (page < pages) {
            ItemStack nextPage = new ItemStack(Material.ARROW, page + 1);
            ItemMeta nextMeta = nextPage.getItemMeta();
            nextMeta.setDisplayName(Messages.GUI_TELEPORTER_NEXT_PAGE.getFullMessage());
            nextPage.setItemMeta(nextMeta);
            inv.setItem(inventorySize - 1, nextPage);
        }
        if (page > 1) {
            ItemStack previousPage = new ItemStack(Material.ARROW, page - 1);
            ItemMeta previousMeta = previousPage.getItemMeta();
            previousMeta.setDisplayName(Messages.GUI_TELEPORTER_PREVIOUS_PAGE.getFullMessage());
            previousPage.setItemMeta(previousMeta);
            inv.setItem(inventorySize - 9, previousPage);
        }


        player.openInventory(inv);
    }


    public static void sendTitle(Player player, String title, String subTitle, int stay) {
        if (VersionUtil.isAbove(VersionEnum.V1_17)) {
            ReflectionHelper.getInstance().sendTitle(player, title, subTitle, 0, stay, 20);
            return;
        }

        PacketEventsUtil.sendPacket(player, new WrapperPlayServerSetTitleTimes(0, stay, 20));

        if (subTitle != null) {
            PacketEventsUtil.sendPacket(player, new WrapperPlayServerSetTitleSubtitle(Component.text(subTitle)));
        }

        PacketEventsUtil.sendPacket(player, new WrapperPlayServerSetTitleText(
                title != null ? Component.text(title) : Component.empty()));
    }

    public static boolean isInRange(Location loc1, Location loc2) {
        return loc1.getWorld().getName().equals(loc2.getWorld().getName()) && (loc1.distance(loc2) <= 48D);
    }

    /** Sets a skull's texture from a base64 "textures" value using the NMS GameProfile (works on legacy/offline servers). */
    private static void applyLegacyTexture(SkullMeta meta, String base64) {
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object profile = gameProfileClass.getConstructor(UUID.class, String.class).newInstance(UUID.randomUUID(), "head");
            Object properties = gameProfileClass.getMethod("getProperties").invoke(profile);
            Object property = propertyClass.getConstructor(String.class, String.class).newInstance("textures", base64);
            properties.getClass().getMethod("put", Object.class, Object.class).invoke(properties, "textures", property);

            java.lang.reflect.Field field = meta.getClass().getDeclaredField("profile");
            field.setAccessible(true);
            field.set(meta, profile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setSkullTexture(SkullMeta meta, String texture) {
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "");
        PlayerTextures textures = profile.getTextures();
        String decoded = new String(Base64.getDecoder().decode(texture));

        TextureInfo info = GSON.fromJson(decoded, TextureInfo.class);
        if (info.getTextures() != null && info.getTextures().getSkin().getUrl() != null) {
            textures.setSkin(info.getTextures().getSkin().getUrl());
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        }
    }

    public static void sendClientPause(Player player, boolean pause) {
        if (VersionUtil.isBelow(VersionEnum.V1_21) || !ConfigManager.USE_MODERN_PAUSE) {
            return;
        }

        PacketEventsUtil.sendPacket(player, new WrapperPlayServerTickingState(20f, pause));
    }

}
