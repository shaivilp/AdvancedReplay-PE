package me.jumper251.replay.legacy;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

/**
 * Empty (void) world generator for 1.8. The dedicated replay world starts completely empty and the
 * captured 7x7 chunk region is pasted in afterwards. Uses the 1.8 {@code generateBlockSections}
 * signature, which differs from the 1.21 generator API, so it must live in the legacy module. The
 * main module only references it via {@link #create()} as a plain {@link ChunkGenerator}.
 */
public class LegacyVoidGenerator extends ChunkGenerator {

    @Override
    public byte[][] generateBlockSections(World world, Random random, int x, int z, BiomeGrid biomes) {
        // All-null sections => every chunk is air.
        return new byte[world.getMaxHeight() / 16][];
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0, 64, 0);
    }

    public static ChunkGenerator create() {
        return new LegacyVoidGenerator();
    }
}
