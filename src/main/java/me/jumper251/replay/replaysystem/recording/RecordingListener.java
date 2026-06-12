package me.jumper251.replay.replaysystem.recording;

import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import me.jumper251.replay.filesystem.ConfigManager;
import me.jumper251.replay.filesystem.Messages;
import me.jumper251.replay.listener.AbstractListener;
import me.jumper251.replay.replaysystem.data.ActionData;
import me.jumper251.replay.replaysystem.data.ActionType;
import me.jumper251.replay.replaysystem.data.snapshot.WorldSnapshot;
import me.jumper251.replay.replaysystem.data.types.*;
import me.jumper251.replay.replaysystem.utils.ItemUtils;
import me.jumper251.replay.replaysystem.utils.NPCManager;
import me.jumper251.replay.utils.version.MaterialBridge;
import me.jumper251.replay.utils.VersionUtil;
import me.jumper251.replay.utils.VersionUtil.VersionEnum;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class RecordingListener extends AbstractListener {
	
	
	private PacketRecorder packetRecorder;
	private Recorder recorder;
	
	private List<String> replayLeft;
	
	public RecordingListener(PacketRecorder packetRecorder) {
		this.packetRecorder = packetRecorder;
		
		this.recorder = this.packetRecorder.getRecorder();
		this.replayLeft = new ArrayList<String>();
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onClick(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player) {
			Player p = (Player) e.getWhoClicked();
			if (recorder.getPlayers().contains(p.getName())) {
				this.packetRecorder.addData(p.getName(), NPCManager.copyFromPlayer(p, true, true));
			}
			
		}
		
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onHeld(PlayerItemHeldEvent e) {
		Player p = e.getPlayer();
		if (recorder.getPlayers().contains(p.getName())) {
			ItemStack stack = p.getInventory().getItem(e.getNewSlot());
			itemInHand(p, stack);
		}
		
		
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onInteract(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		if (recorder.getPlayers().contains(p.getName())) {
			if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
				
				boolean isInteractable = e.getClickedBlock() != null && ItemUtils.isInteractable(e.getClickedBlock().getType());
				if (e.getItem() != null && ItemUtils.isUsable(e.getItem().getType()) && (!isInteractable || p.isSneaking())) {
					if (!this.recorder.getData().getWatcher(p.getName()).isBlocking()) {
						PlayerWatcher watcher = this.recorder.getData().getWatcher(p.getName());
						watcher.setBlocking(true);
						this.packetRecorder.addData(p.getName(), MetadataUpdate.fromWatcher(watcher));
						
					}
				}
				
				
				if (NPCManager.isArmor(e.getItem()) && !NPCManager.wearsArmor(p, NPCManager.getArmorType(e.getItem()))) {
					InvData data = NPCManager.copyFromPlayer(p, true, true);
					String armorType = NPCManager.getArmorType(e.getItem());
					if (armorType != null) {
						if (armorType.equals("head")) data.setHead(NPCManager.fromItemStack(e.getItem()));
						if (armorType.equals("chest")) data.setChest(NPCManager.fromItemStack(e.getItem()));
						if (armorType.equals("leg")) data.setLeg(NPCManager.fromItemStack(e.getItem()));
						if (armorType.equals("boots")) data.setBoots(NPCManager.fromItemStack(e.getItem()));
					}
					if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
						data.setMainHand(null);
					}
					
					this.packetRecorder.addData(p.getName(), data);
					
				}
			}
			
			if (e.getAction() == Action.LEFT_CLICK_BLOCK && p.getTargetBlock((Set<Material>) null, 5).getType() == Material.FIRE) {
				LocationData location = LocationData.fromLocation(p.getTargetBlock((Set<Material>) null, 5).getLocation());
				
				ItemData before = ItemData.fromMaterial(Material.FIRE);
				ItemData after = ItemData.air();
				
				this.packetRecorder.addData(p.getName(), new BlockChangeData(location, before, after));
			}
			
		}
		
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onConsume(PlayerItemConsumeEvent e) {
		Player p = e.getPlayer();
		if (recorder.getPlayers().contains(p.getName())) {
			if (recorder.getData().getWatcher(p.getName()).isBlocking()) {
				PlayerWatcher watcher = this.recorder.getData().getWatcher(p.getName());
				watcher.setBlocking(false);
				this.packetRecorder.addData(p.getName(), MetadataUpdate.fromWatcher(watcher));
				
				InvData data = NPCManager.copyFromPlayer(p, true, true);
				if (p.getItemInHand() != null && p.getItemInHand().getAmount() <= 1) {
					data.setMainHand(null);
				}
				
				this.packetRecorder.addData(p.getName(), data);
				
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			Player p = (Player) e.getEntity();
			if (recorder.getPlayers().contains(p.getName())) {
				
				this.packetRecorder.addData(p.getName(), new AnimationData(1));
				
				if (p.getFireTicks() > 20 && !this.recorder.getData().getWatcher(p.getName()).isBurning()) {
					this.recorder.getData().getWatcher(p.getName()).setBurning(true);
					this.packetRecorder.addData(p.getName(), new MetadataUpdate(true, this.recorder.getData().getWatcher(p.getName()).isBlocking(), this.recorder.getData().getWatcher(p.getName()).isElytra()));
				} else if (p.getFireTicks() <= 20 && this.recorder.getData().getWatcher(p.getName()).isBurning()) {
					this.recorder.getData().getWatcher(p.getName()).setBurning(false);
					this.packetRecorder.addData(p.getName(), new MetadataUpdate(false, this.recorder.getData().getWatcher(p.getName()).isBlocking(), this.recorder.getData().getWatcher(p.getName()).isElytra()));
				}
			}
		} else if (e.getEntity() instanceof LivingEntity) {
			LivingEntity living = (LivingEntity) e.getEntity();
			if (this.packetRecorder.getEntityLookup().containsKey(living.getEntityId())) {
				
				this.packetRecorder.addData(this.packetRecorder.getEntityLookup().get(living.getEntityId()), new EntityAnimationData(living.getEntityId(), (living.getHealth() - e.getFinalDamage()) > 0 ? 2 : 3));
			}
		}
		
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onCrit(EntityDamageByEntityEvent e) {
		if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
			Player damager = (Player) e.getDamager();
			Player victim = (Player) e.getEntity();
			if (recorder.getPlayers().contains(damager.getName()) && recorder.getPlayers().contains(victim.getName())) {
				if (damager.getFallDistance() > 0.0F && !damager.isOnGround() && damager.getVehicle() == null) {
					
					this.packetRecorder.addData(victim.getName(), new AnimationData(4));
				}
			}
		}
	}
	
	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		if (recorder.getPlayers().contains(p.getName())) {
			
			this.packetRecorder.addData(p.getName(), new ChatData(e.getMessage()));
		}
		
	}
	
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBed(PlayerBedEnterEvent e) {
		Player p = e.getPlayer();
		if (recorder.getPlayers().contains(p.getName())) {
			
			this.packetRecorder.addData(p.getName(), new BedEnterData(LocationData.fromLocation(e.getBed().getLocation())));
		}
		
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBedLeave(PlayerBedLeaveEvent e) {
		Player p = e.getPlayer();
		if (recorder.getPlayers().contains(p.getName())) {
			
			this.packetRecorder.addData(p.getName(), new AnimationData(2));
		}
		
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		if (this.recorder.getPlayers().contains(p.getName())) {
			this.recorder.addData(this.recorder.getCurrentTick(), new ActionData(0, ActionType.DESPAWN, p.getName(), null));
			this.recorder.getPlayers().remove(p.getName());
			
			if (!this.replayLeft.contains(p.getName())) this.replayLeft.add(p.getName());
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		if (!this.recorder.getPlayers().contains(p.getName()) && (this.replayLeft.contains(p.getName())) || ConfigManager.ADD_PLAYERS) {
			this.recorder.getPlayers().add(p.getName());
			this.recorder.getData().getWatchers().put(p.getName(), new PlayerWatcher(p.getName()));
			this.recorder.createSpawnAction(p, p.getLocation(), false);
			this.recorder.addData(this.recorder.getCurrentTick(), new ActionData(this.recorder.getCurrentTick(), ActionType.MESSAGE, p.getName(), new ChatData(Messages.REPLAYING_PLAYER_JOIN.arg("name", p.getName()).build())));
			
		}
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent e) {
		Player p = e.getEntity();
		if (this.recorder.getPlayers().contains(p.getName())) {
			this.recorder.addData(this.recorder.getCurrentTick(), new ActionData(0, ActionType.DEATH, p.getName(), null));
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onRespawn(PlayerRespawnEvent e) {
		Player p = e.getPlayer();
		if (this.recorder.getPlayers().contains(p.getName())) {
			
			this.recorder.createSpawnAction(p, e.getRespawnLocation(), false);
		}
		
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onThrow(PlayerDropItemEvent e) {
		Player p = e.getPlayer();
		if (this.recorder.getPlayers().contains(p.getName())) {
			InvData data = NPCManager.copyFromPlayer(p, true, true);
			// Check if item in hand is thrown
			if (data.getMainHand() != null && p.getItemInHand() != null && p.getItemInHand().getAmount() <= 1 && p.getItemInHand().getType() == data.getMainHand().toMaterial()) {
				data.setMainHand(null);
			}
			
			this.packetRecorder.addData(p.getName(), data);
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onLaunch(ProjectileLaunchEvent e) {
		Projectile proj = e.getEntity();
		if (proj.getShooter() instanceof Player) {
			Player p = (Player) proj.getShooter();
			if (this.recorder.getPlayers().contains(p.getName())) {
				LocationData spawn = LocationData.fromLocation(p.getEyeLocation());
				LocationData velocity = LocationData.fromLocation(proj.getVelocity().toLocation(p.getWorld()));

				// Capture the carried item so the potion type/color is preserved on playback.
				// On 1.8 the potion effect is encoded in the item damage value, which SerializableItemStack keeps.
				ItemData projItem = proj instanceof ThrownPotion
						? NPCManager.fromItemStack(((ThrownPotion) proj).getItem())
						: null;

				this.packetRecorder.addData(p.getName(), new ProjectileData(spawn, velocity, proj.getType(), projItem));
				this.packetRecorder.addData(p.getName(), NPCManager.copyFromPlayer(p, true, true));
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPickup(PlayerPickupItemEvent e) {
		Player p = e.getPlayer();
		if (this.recorder.getPlayers().contains(p.getName())) {
			// Change PlayerItemInHand
			
			if (p.getItemInHand() == null || p.getItemInHand().getType() == Material.AIR) {
				itemInHand(p, e.getItem().getItemStack());
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPlace(BlockPlaceEvent e) {
		Player p = e.getPlayer();
		if (this.recorder.getPlayers().contains(p.getName())) {
			LocationData location = LocationData.fromLocation(e.getBlockPlaced().getLocation());


            ItemData before = VersionUtil.isAbove(VersionEnum.V1_13) ? ItemData.fromMaterial(e.getBlockReplacedState().getType()) : new ItemData(e.getBlockReplacedState().getType().getId(), e.getBlockReplacedState().getData().getData());
			ItemData after = VersionUtil.isAbove(VersionEnum.V1_13) ? ItemData.fromMaterial(e.getBlockPlaced().getBlockData().getMaterial()) : new ItemData(e.getBlockPlaced().getType().getId(), e.getBlockPlaced().getData());

			this.packetRecorder.addData(p.getName(), new BlockChangeData(location, before, after));
			
			// Change PlayerItemInHand when last block in hand
			if (e.getItemInHand().getAmount() == 1) {
				ItemStack stack = new ItemStack(Material.AIR, 1);
				itemInHand(p, stack);
			}
		}
		
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBreak(BlockBreakEvent e) {
		Player p = e.getPlayer();
		if (this.recorder.getPlayers().contains(p.getName())) {
			recordBlockBreak(p, e.getBlock());
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onTNTExplode(EntityExplodeEvent e) {
		if (!(e.getEntity() instanceof TNTPrimed)) return;
		
		TNTPrimed tnt = (TNTPrimed) e.getEntity();
		if (!(tnt.getSource() instanceof Player)) return;
		
		Player p = (Player) tnt.getSource();
		if (!this.recorder.getPlayers().contains(p.getName())) return;
		
		for (Block block : e.blockList()) {
			//Block change is done by the explosion packet.
			// We record the block data for reversed playing, real_changes=true and world reset
			recordBlockBreak(p, block, false, false);
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onTNTPrime(PlayerInteractEvent e) {
		//TNTPrimeEvent is bugged without paper, so we need to use PlayerInteractEvent instead
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (e.getItem() == null || !(e.getItem().getType() == Material.FLINT_AND_STEEL || e.getItem().getType() == MaterialBridge.FIRE_CHARGE.toMaterial()))
			return;
		Block block = e.getClickedBlock();
		if (block == null) return;
		if (block.getType() != Material.TNT) return;
		Player p = e.getPlayer();
		if(p.isSneaking()) return;
		if (!this.recorder.getPlayers().contains(p.getName())) return;
		
		recordBlockBreak(e.getPlayer(), block, false, true);
	}
	
	@SuppressWarnings("deprecation")
	private void recordBlockBreak(Player p, Block block, boolean playEffect, boolean doBlockChange) {
		LocationData location = LocationData.fromLocation(block.getLocation());
		ItemData before = ItemData.fromBlock(block);
		ItemData after = ItemData.air();
		
		this.packetRecorder.addData(p.getName(), new BlockChangeData(location, before, after, playEffect, doBlockChange));
	}
	
	private void recordBlockBreak(Player p, Block block) {
		recordBlockBreak(p, block, true, true);
	}

	// ===== Full world block-state diff =====
	// Captures non-player-driven block changes (liquids, pistons, fire, growth, explosions, falling
	// blocks, ...) within the captured 7x7 region so the replay world reflects them. Player place/break
	// are already handled above. Recorded under the primary player's name so the existing
	// BlockChangeData playback path (which requires an NPC) applies them.

	private final HashMap<Long, Integer> lastBlockTick = new HashMap<>();

	private static long packPos(int x, int y, int z) {
		return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
	}

	private String blockDiffOwner() {
		List<String> players = this.recorder.getPlayers();
		return players.isEmpty() ? null : players.get(0);
	}

	/** True if the block is inside the captured region and hasn't already been recorded this tick. */
	private boolean inDiffRegion(Block block) {
		if (!ConfigManager.FULL_BLOCK_DIFF) return false;
		WorldSnapshot snap = this.recorder.getData().getWorldSnapshot();
		if (snap == null) return false;
		if (!block.getWorld().getName().equals(snap.getOriginalWorld())) return false;
		if (!snap.contains(block.getX(), block.getY(), block.getZ())) return false;

		long key = packPos(block.getX(), block.getY(), block.getZ());
		int tick = this.recorder.getCurrentTick();
		Integer last = lastBlockTick.get(key);
		if (last != null && last == tick) return false;
		if (lastBlockTick.size() > 20000) lastBlockTick.clear();
		lastBlockTick.put(key, tick);
		return true;
	}

	private void recordDiff(Block block, ItemData before, ItemData after) {
		String owner = blockDiffOwner();
		if (owner == null) return;
		LocationData location = LocationData.fromLocation(block.getLocation());
		this.packetRecorder.addData(owner, new BlockChangeData(location, before, after, false, true));
	}

	@SuppressWarnings("deprecation")
	private ItemData fromState(BlockState state) {
		if (VersionUtil.isAbove(VersionEnum.V1_13)) {
			return ItemData.fromMaterial(state.getType());
		}
		return new ItemData(state.getType().getId(), state.getData().getData());
	}

	private void recordStateChange(Block block, BlockState newState) {
		if (!inDiffRegion(block)) return;
		recordDiff(block, ItemData.fromBlock(block), fromState(newState));
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onLiquidFlow(BlockFromToEvent e) {
		Block to = e.getToBlock();
		if (!inDiffRegion(to)) return;
		recordDiff(to, ItemData.fromBlock(to), ItemData.fromMaterial(e.getBlock().getType()));
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onFade(BlockFadeEvent e) {
		recordStateChange(e.getBlock(), e.getNewState());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onForm(BlockFormEvent e) {
		recordStateChange(e.getBlock(), e.getNewState());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onSpread(BlockSpreadEvent e) {
		recordStateChange(e.getBlock(), e.getNewState());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onGrow(BlockGrowEvent e) {
		recordStateChange(e.getBlock(), e.getNewState());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onLeavesDecay(LeavesDecayEvent e) {
		Block b = e.getBlock();
		if (!inDiffRegion(b)) return;
		recordDiff(b, ItemData.fromBlock(b), ItemData.air());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBlockBurn(BlockBurnEvent e) {
		Block b = e.getBlock();
		if (!inDiffRegion(b)) return;
		recordDiff(b, ItemData.fromBlock(b), ItemData.air());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onEntityChangeBlock(EntityChangeBlockEvent e) {
		Block b = e.getBlock();
		if (!inDiffRegion(b)) return;
		recordDiff(b, ItemData.fromBlock(b), ItemData.fromMaterial(e.getTo()));
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onBlockExplode(BlockExplodeEvent e) {
		for (Block b : e.blockList()) {
			if (!inDiffRegion(b)) continue;
			recordDiff(b, ItemData.fromBlock(b), ItemData.air());
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onWorldExplode(EntityExplodeEvent e) {
		// Player TNT is already captured with reverse-play detail by onTNTExplode.
		if (e.getEntity() instanceof TNTPrimed
				&& ((TNTPrimed) e.getEntity()).getSource() instanceof Player
				&& this.recorder.getPlayers().contains(((Player) ((TNTPrimed) e.getEntity()).getSource()).getName())) {
			return;
		}
		for (Block b : e.blockList()) {
			if (!inDiffRegion(b)) continue;
			recordDiff(b, ItemData.fromBlock(b), ItemData.air());
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onStructureGrow(StructureGrowEvent e) {
		for (BlockState state : e.getBlocks()) {
			Block b = state.getBlock();
			if (!inDiffRegion(b)) continue;
			recordDiff(b, ItemData.fromBlock(b), fromState(state));
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPistonExtend(BlockPistonExtendEvent e) {
		recordPistonMove(e.getBlocks(), e.getDirection());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPistonRetract(BlockPistonRetractEvent e) {
		recordPistonMove(e.getBlocks(), e.getDirection());
	}

	private void recordPistonMove(List<Block> blocks, BlockFace dir) {
		for (Block b : blocks) {
			Block dest = b.getRelative(dir);
			ItemData moved = ItemData.fromBlock(b);
			if (inDiffRegion(dest)) recordDiff(dest, ItemData.fromBlock(dest), moved);
			if (inDiffRegion(b)) recordDiff(b, ItemData.fromBlock(b), ItemData.air());
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onFill(PlayerBucketFillEvent e) {
		Player p = e.getPlayer();
		if (this.recorder.getPlayers().contains(p.getName())) {
			LocationData location = LocationData.fromLocation(e.getBlockClicked().getLocation());
			
			ItemData before = ItemData.fromBlock(e.getBlockClicked());
			ItemData after = ItemData.air();
			
			this.packetRecorder.addData(p.getName(), new BlockChangeData(location, before, after));
			
			// Change PlayerItemInHand when fill bucket
			ItemStack stack;

			if (e.getBlockClicked().getState().getType().equals(MaterialBridge.LAVA.toMaterial())) {
				stack = new ItemStack(Material.LAVA_BUCKET, 1);
			} else {
				stack = new ItemStack(Material.WATER_BUCKET, 1);
			}
			itemInHand(p, stack);
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onEmpty(PlayerBucketEmptyEvent e) {
		Player p = e.getPlayer();
		if (this.recorder.getPlayers().contains(p.getName())) {
			Block block = e.getBlockClicked().getRelative(e.getBlockFace());
			LocationData location = LocationData.fromLocation(block.getLocation());
			
			ItemData before = ItemData.fromBlock(block);
			ItemData after;

			if (VersionUtil.isAbove(VersionEnum.V1_13)) {
				Material material = e.getBucket() == Material.LAVA_BUCKET ? Material.LAVA : Material.WATER;
				after = ItemData.fromMaterial(material);
			} else {
                after = new ItemData(e.getBucket() == Material.LAVA_BUCKET ? 11 : 9, 0);
            }

			this.packetRecorder.addData(p.getName(), new BlockChangeData(location, before, after));
			
			// Change PlayerItemInHand
			ItemStack stack = new ItemStack(Material.BUCKET, 1);
			itemInHand(p, stack);
		}
	}
	
	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent e) {
		Player p = e.getPlayer();
		if (this.recorder.getPlayers().contains(p.getName())) {
			LocationData location = LocationData.fromLocation(p.getLocation());
			
			
			this.packetRecorder.addData(p.getName(), new WorldChangeData(location));
		}
		
	}

	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSneak(PlayerToggleSneakEvent e) {
		Player p = e.getPlayer();
		if (VersionUtil.isAbove(VersionEnum.V1_21) && this.recorder.getPlayers().contains(p.getName())) {

			WrapperPlayClientEntityAction.Action action = e.isSneaking()
					? WrapperPlayClientEntityAction.Action.START_SNEAKING
					: WrapperPlayClientEntityAction.Action.STOP_SNEAKING;
			this.packetRecorder.addData(p.getName(), new EntityActionData(action));
		}
	}
	
	public void itemInHand(Player p, ItemStack stack) {
		InvData data = NPCManager.copyFromPlayer(p, true, true);
		data.setMainHand(NPCManager.fromItemStack(stack));
		
		this.packetRecorder.addData(p.getName(), data);
		
		if (recorder.getData().getWatcher(p.getName()).isBlocking()) {
			recorder.getData().getWatcher(p.getName()).setBlocking(false);
			this.packetRecorder.addData(p.getName(), new MetadataUpdate(recorder.getData().getWatcher(p.getName()).isBurning(), false, this.recorder.getData().getWatcher(p.getName()).isElytra()));
		}
	}
}
