package me.jumper251.replay.legacy;

import org.bukkit.World;

/**
 * 1.8 world configuration for dedicated replay worlds. {@code setGameRuleValue(String, String)} is
 * the 1.8 gamerule API (replaced by the typed GameRule API in modern versions), so it lives here.
 */
public class LegacyWorld {

    public static void applyReplaySettings(World world) {
        world.setKeepSpawnInMemory(false);
        world.setAutoSave(false);
        world.setTime(6000);

        world.setGameRuleValue("doDaylightCycle", "false");
        world.setGameRuleValue("doWeatherCycle", "false");
        world.setGameRuleValue("doMobSpawning", "false");
        world.setGameRuleValue("randomTickSpeed", "0");
        world.setGameRuleValue("doFireTick", "false");
    }
}
