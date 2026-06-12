package me.jumper251.replay.replaysystem.data.types;

import org.bukkit.entity.EntityType;

public class ProjectileData extends PacketData {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8320803760880960739L;

	private LocationData spawn, velocity;

	private EntityType type;

	// The item the projectile carries (e.g. the potion type/color for thrown potions). Nullable;
	// old replays and non-item projectiles leave this null.
	private ItemData item;


	public ProjectileData(LocationData spawn, LocationData velocity, EntityType type) {
		this(spawn, velocity, type, null);
	}

	public ProjectileData(LocationData spawn, LocationData velocity, EntityType type, ItemData item) {
		this.spawn = spawn;
		this.velocity = velocity;
		this.type = type;
		this.item = item;
	}

	public LocationData getSpawn() {
		return spawn;
	}

	public LocationData getVelocity() {
		return velocity;
	}

	public EntityType getType() {
		return type;
	}

	public ItemData getItem() {
		return item;
	}
}
