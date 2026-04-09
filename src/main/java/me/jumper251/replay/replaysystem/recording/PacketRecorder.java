package me.jumper251.replay.replaysystem.recording;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAnimation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMoveAndRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnLivingEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.jumper251.replay.filesystem.ConfigManager;
import me.jumper251.replay.listener.AbstractListener;
import me.jumper251.replay.replaysystem.data.types.*;
import me.jumper251.replay.replaysystem.recording.optimization.ReplayOptimizer;
import me.jumper251.replay.replaysystem.utils.NPCManager;
import me.jumper251.replay.replaysystem.utils.entities.EntityMappings;
import me.jumper251.replay.utils.VersionUtil;
import me.jumper251.replay.utils.VersionUtil.VersionEnum;
import me.jumper251.replay.utils.version.EntityBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PacketRecorder extends AbstractListener {

    private static final Set<PacketTypeCommon> RECORDED_PACKETS = new HashSet<>();

    static {
        RECORDED_PACKETS.add(PacketType.Play.Client.PLAYER_POSITION);
        RECORDED_PACKETS.add(PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION);
        RECORDED_PACKETS.add(PacketType.Play.Client.PLAYER_ROTATION);
        RECORDED_PACKETS.add(PacketType.Play.Client.ENTITY_ACTION);
        RECORDED_PACKETS.add(PacketType.Play.Client.ANIMATION);
        RECORDED_PACKETS.add(PacketType.Play.Client.PLAYER_DIGGING);
        RECORDED_PACKETS.add(PacketType.Play.Server.SPAWN_ENTITY);
        RECORDED_PACKETS.add(PacketType.Play.Server.DESTROY_ENTITIES);
        RECORDED_PACKETS.add(PacketType.Play.Server.ENTITY_VELOCITY);
        RECORDED_PACKETS.add(PacketType.Play.Server.EXPLOSION);
        RECORDED_PACKETS.add(PacketType.Play.Server.ENTITY_RELATIVE_MOVE);
        RECORDED_PACKETS.add(PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION);
        RECORDED_PACKETS.add(PacketType.Play.Server.ENTITY_ROTATION);
        RECORDED_PACKETS.add(PacketType.Play.Server.PLAYER_POSITION_AND_LOOK);
        RECORDED_PACKETS.add(PacketType.Play.Server.ENTITY_TELEPORT);
    }

    private PacketListenerAbstract packetListener;

    private Map<String, List<PacketData>> packetData;

    private List<Integer> spawnedItems;
    private Map<Integer, EntityData> spawnedEntities;
    private Map<Integer, String> entityLookup;
    private Map<Integer, Entity> idLookup;
    private List<Integer> spawnedHooks;
    private List<Integer> spawnedTNT;

    private Recorder recorder;

    private ReplayOptimizer optimizer;

    private AbstractListener compListener, listener;

    public PacketRecorder(Recorder recorder) {
        super();
        this.packetData = new ConcurrentHashMap<>();
        this.spawnedItems = new ArrayList<>();
        this.spawnedEntities = new HashMap<>();
        this.entityLookup = new HashMap<>();
        this.idLookup = new HashMap<>();
        this.spawnedHooks = new ArrayList<>();
        this.spawnedTNT = new ArrayList<>();
        this.recorder = recorder;
        this.optimizer = new ReplayOptimizer();
    }

    @Override
    public void register() {
        super.register();

        Set<PacketTypeCommon> types = new HashSet<>(RECORDED_PACKETS);
        if (VersionUtil.isBelow(VersionEnum.V1_18)) {
            types.add(PacketType.Play.Server.SPAWN_LIVING_ENTITY);
        }

        this.packetListener = new PacketListenerAbstract(PacketListenerPriority.HIGH) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (!types.contains(event.getPacketType())) {
                    return;
                }
                Player p = Bukkit.getPlayer(event.getUser().getUUID());
                if (p == null || !recorder.getPlayers().contains(p.getName())) {
                    return;
                }

                PacketData data = null;
                if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
                    WrapperPlayClientPlayerPosition packet = new WrapperPlayClientPlayerPosition(event);
                    Vector3d pos = packet.getLocation().getPosition();
                    data = new MovingData(pos.getX(), pos.getY(), pos.getZ(), p.getLocation().getPitch(), p.getLocation().getYaw());

                    if (recorder.getData().getWatcher(p.getName()).isBurning() && p.getFireTicks() <= 20) {
                        recorder.getData().getWatcher(p.getName()).setBurning(false);
                        addData(p.getName(), new MetadataUpdate(false, recorder.getData().getWatcher(p.getName()).isBlocking(), recorder.getData().getWatcher(p.getName()).isElytra()));
                    }
                }

                if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
                    WrapperPlayClientPlayerRotation packet = new WrapperPlayClientPlayerRotation(event);
                    data = new MovingData(p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ(), packet.getPitch(), packet.getYaw());
                }

                if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
                    WrapperPlayClientPlayerPositionAndRotation packet = new WrapperPlayClientPlayerPositionAndRotation(event);
                    Vector3d pos = packet.getLocation().getPosition();
                    data = new MovingData(pos.getX(), pos.getY(), pos.getZ(), packet.getLocation().getPitch(), packet.getLocation().getYaw());
                }

                if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
                    WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);
                    WrapperPlayClientEntityAction.Action action = packet.getAction();
                    if (action == WrapperPlayClientEntityAction.Action.START_SNEAKING || action == WrapperPlayClientEntityAction.Action.STOP_SNEAKING) {
                        data = new EntityActionData(action);
                    }
                }
                if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
                    data = new AnimationData(0);
                }

                if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
                    WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
                    if (packet.getAction() == DiggingAction.RELEASE_USE_ITEM) {
                        if (recorder.getData().getWatcher(p.getName()).isBlocking()) {
                            PlayerWatcher watcher = recorder.getData().getWatcher(p.getName());
                            watcher.setBlocking(false);
                            addData(p.getName(), MetadataUpdate.fromWatcher(watcher));
                        }
                    }
                }

                addData(p.getName(), data);
            }

            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (!types.contains(event.getPacketType())) {
                    return;
                }
                Player p = Bukkit.getPlayer(event.getUser().getUUID());
                if (p == null || !recorder.getPlayers().contains(p.getName())) {
                    return;
                }

                if (event.getPacketType() == PacketType.Play.Server.EXPLOSION) {
                    addData(p.getName(), ExplosionData.fromPacket(event));
                }

                if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
                    WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(event);
                    EntityType peType = packet.getEntityType();
                    org.bukkit.entity.EntityType bukkitType = SpigotConversionUtil.toBukkitEntityType(peType);

                    LocationData location = new LocationData(
                            packet.getPosition().getX(),
                            packet.getPosition().getY(),
                            packet.getPosition().getZ(),
                            p.getWorld().getName()
                    );

                    boolean isTnt = bukkitType == EntityBridge.TNT.toEntityType();
                    if (isTnt) {
                        if (!spawnedTNT.contains(packet.getEntityId())) {
                            spawnedTNT.add(packet.getEntityId());
                            addData(p.getName(), new TNTSpawnData(packet.getEntityId(), location));
                        }
                    }

                    boolean isItem = bukkitType == EntityBridge.ITEM.toEntityType();
                    if (isItem && !spawnedItems.contains(packet.getEntityId())) {
                        Entity en = SpigotConversionUtil.getEntityById(p.getWorld(), packet.getEntityId());
                        if (en instanceof Item) {
                            Item item = (Item) en;
                            LocationData velocity = LocationData.fromLocation(item.getVelocity().toLocation(p.getWorld()));
                            addData(p.getName(), new EntityItemData(0, packet.getEntityId(), NPCManager.fromItemStack(item.getItemStack()), location, velocity));
                            spawnedItems.add(packet.getEntityId());
                        }
                    }

                    boolean isHook = bukkitType == EntityBridge.FISHING_BOBBER.toEntityType();
                    if (isHook && !spawnedHooks.contains(packet.getEntityId())) {
                        int throwerId = packet.getData();
                        String ownerName = Bukkit.getOnlinePlayers().stream()
                                .filter(player -> player.getWorld().getName().equals(p.getWorld().getName()) && player.getEntityId() == throwerId)
                                .findFirst()
                                .map(Player::getName)
                                .orElse(null);
                        Vector vel = packet.getVelocity().map(v -> new Vector(v.getX(), v.getY(), v.getZ())).orElse(new Vector(0, 0, 0));
                        addData(p.getName(), new FishingData(packet.getEntityId(), location, vel.getX(), vel.getY(), vel.getZ(), ownerName));
                        spawnedHooks.add(packet.getEntityId());
                    }

                    if (VersionUtil.isAbove(VersionEnum.V1_19) && ConfigManager.RECORD_ENTITIES && bukkitType != null) {
                        if (EntityMappings.getInstance().getTypeId(bukkitType.toString()) != 0) {
                            LocationData locationData = new LocationData(
                                    packet.getPosition().getX(),
                                    packet.getPosition().getY(),
                                    packet.getPosition().getZ(),
                                    p.getWorld().getName()
                            );
                            if (!spawnedEntities.containsKey(packet.getEntityId())) {
                                EntityData entData = new EntityData(0, packet.getEntityId(), locationData, bukkitType.toString());
                                addData(p.getName(), entData);
                                spawnedEntities.put(packet.getEntityId(), entData);
                                entityLookup.put(packet.getEntityId(), p.getName());
                                idLookup.put(packet.getEntityId(), SpigotConversionUtil.getEntityById(p.getWorld(), packet.getEntityId()));
                            }
                        }
                    }
                }

                if (event.getPacketType() == PacketType.Play.Server.SPAWN_LIVING_ENTITY && ConfigManager.RECORD_ENTITIES) {
                    WrapperPlayServerSpawnLivingEntity packet = new WrapperPlayServerSpawnLivingEntity(event);
                    org.bukkit.entity.EntityType bukkitType = SpigotConversionUtil.toBukkitEntityType(packet.getEntityType());
                    if (!spawnedEntities.containsKey(packet.getEntityId())) {
                        LocationData location = new LocationData(
                                packet.getPosition().getX(),
                                packet.getPosition().getY(),
                                packet.getPosition().getZ(),
                                p.getWorld().getName()
                        );
                        String typeStr = bukkitType != null ? bukkitType.toString() : packet.getEntityType().getName().toString();
                        EntityData entData = new EntityData(0, packet.getEntityId(), location, typeStr);
                        addData(p.getName(), entData);
                        spawnedEntities.put(packet.getEntityId(), entData);
                        entityLookup.put(packet.getEntityId(), p.getName());
                        idLookup.put(packet.getEntityId(), SpigotConversionUtil.getEntityById(p.getWorld(), packet.getEntityId()));
                    }
                }

                if (event.getPacketType() == PacketType.Play.Server.DESTROY_ENTITIES) {
                    WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(event);
                    List<Integer> entityIds = IntStream.of(packet.getEntityIds()).boxed().collect(Collectors.toList());

                    for (Integer id : entityIds) {
                        if (spawnedItems.contains(id)) {
                            addData(p.getName(), new EntityItemData(1, id, null, null, null));
                            spawnedItems.remove(id);
                        } else if (spawnedEntities.containsKey(id) && (idLookup.get(id) == null || idLookup.get(id).isDead())) {
                            addData(p.getName(), new EntityData(1, id, spawnedEntities.get(id).getLocation(), spawnedEntities.get(id).getType()));
                            spawnedEntities.remove(id);
                            entityLookup.remove(id);
                            idLookup.remove(id);
                        } else if (spawnedHooks.contains(id)) {
                            addData(p.getName(), new EntityItemData(2, id, null, null, null));
                            spawnedHooks.remove(id);
                        } else if (spawnedTNT.contains(id)) {
                            addData(p.getName(), new EntityDestroyData(id));
                            spawnedTNT.remove(id);
                        }
                    }
                }

                if (event.getPacketType() == PacketType.Play.Server.ENTITY_VELOCITY) {
                    WrapperPlayServerEntityVelocity packet = new WrapperPlayServerEntityVelocity(event);
                    Vector3d v = packet.getVelocity();
                    if (spawnedHooks.contains(packet.getEntityId())
                            || (entityLookup.containsKey(packet.getEntityId()) && entityLookup.get(packet.getEntityId()).equalsIgnoreCase(p.getName()))) {
                        addData(p.getName(), new VelocityData(packet.getEntityId(), (float) v.x, (float) v.y, (float) v.z));
                    } else if (spawnedTNT.contains(packet.getEntityId())) {
                        addData(p.getName(), new VelocityData(packet.getEntityId(), (float) v.x, (float) v.y, (float) v.z));
                    }
                }

                if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
                    WrapperPlayServerEntityRelativeMove packet = new WrapperPlayServerEntityRelativeMove(event);
                    if (entityLookup.containsKey(packet.getEntityId()) && entityLookup.get(packet.getEntityId()).equalsIgnoreCase(p.getName())) {
                        Location loc = checkEntityLocation(SpigotConversionUtil.getEntityById(p.getWorld(), packet.getEntityId()));
                        if (loc != null) {
                            addData(p.getName(), new EntityMovingData(packet.getEntityId(), loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw()));
                        }
                    }
                }

                if (event.getPacketType() == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
                    WrapperPlayServerEntityRelativeMoveAndRotation packet = new WrapperPlayServerEntityRelativeMoveAndRotation(event);
                    if (entityLookup.containsKey(packet.getEntityId()) && entityLookup.get(packet.getEntityId()).equalsIgnoreCase(p.getName())) {
                        Location loc = checkEntityLocation(SpigotConversionUtil.getEntityById(p.getWorld(), packet.getEntityId()));
                        if (loc != null) {
                            addData(p.getName(), new EntityMovingData(packet.getEntityId(), loc.getX(), loc.getY(), loc.getZ(), packet.getPitch(), packet.getYaw()));
                        }
                    }
                }

                if (event.getPacketType() == PacketType.Play.Server.ENTITY_TELEPORT) {
                    WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(event);
                    if (entityLookup.containsKey(packet.getEntityId()) && entityLookup.get(packet.getEntityId()).equalsIgnoreCase(p.getName())) {
                        Location loc = checkEntityLocation(SpigotConversionUtil.getEntityById(p.getWorld(), packet.getEntityId()));
                        if (loc != null) {
                            addData(p.getName(), new EntityMovingData(packet.getEntityId(), loc.getX(), loc.getY(), loc.getZ(), packet.getPitch(), packet.getYaw()));
                        }
                    }
                }
            }
        };

        PacketEvents.getAPI().getEventManager().registerListener(this.packetListener);
        this.registerExternalListeners();
    }

    @Override
    public void unregister() {
        super.unregister();
        if (this.packetListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(this.packetListener);
            this.packetListener = null;
        }
        if (this.compListener != null) {
            this.compListener.unregister();
        }
        this.listener.unregister();
    }

    private void registerExternalListeners() {
        if (!VersionUtil.isCompatible(VersionEnum.V1_8)) {
            this.compListener = new CompListener(this);
            this.compListener.register();
        }
        this.listener = new RecordingListener(this);
        this.listener.register();
    }

    private Location checkEntityLocation(Entity en) {
        if (en == null) {
            return null;
        }
        return en.getLocation();
    }

    public void addData(String name, PacketData data) {
        if (data == null) {
            return;
        }
        if (!optimizer.shouldRecord(name, data)) {
            return;
        }
        this.packetData.computeIfAbsent(name, k -> Collections.synchronizedList(new ArrayList<>())).add(data);
    }

    public Map<String, List<PacketData>> getPacketData() {
        return packetData;
    }

    public Map<Integer, String> getEntityLookup() {
        return entityLookup;
    }

    public Recorder getRecorder() {
        return recorder;
    }
}
