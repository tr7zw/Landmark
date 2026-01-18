package dev.tr7zw.landmark.ecs;

import com.hypixel.hytale.codec.*;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.spatial.*;
import com.hypixel.hytale.logger.*;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.*;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.*;
import com.hypixel.hytale.server.core.modules.entity.*;
import com.hypixel.hytale.server.core.modules.entity.component.*;
import com.hypixel.hytale.server.core.modules.time.*;
import com.hypixel.hytale.server.core.universe.world.*;
import com.hypixel.hytale.server.core.universe.world.storage.*;
import com.hypixel.hytale.server.core.util.*;
import dev.tr7zw.landmark.*;
import dev.tr7zw.landmark.util.*;
import it.unimi.dsi.fastutil.objects.*;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.time.*;
import java.time.temporal.*;
import java.util.*;

public class LandmarkBlock implements Component<ChunkStore> {
    private static final String[] RANDOM_NAMES = new String[]{"Dawnreach", "Stonehaven", "Emberfall", "Silverbrook", "Oakrest", "Brightwater", "Frosthold", "Goldleaf Crossing", "Highmeadow", "Westfall Outpost", "Whispering Vale", "Suncrest Ridge", "Moonshadow Grove", "Stormwatch Peak", "Ashen Plains", "Crystal Hollow", "Windscar Cliffs", "Starfall Basin", "The Lost Spire", "Blackreach Ruins", "Hollowdeep", "Ancient Watch", "The Shattered Gate", "Deepstone Vault", "Frontier Camp", "Forgotten Crossing", "Old Waystone", "The Far Reach", "Silent Expanse", "Wayfarer’s Rest"};

    public static final BuilderCodec<LandmarkBlock> CODEC = BuilderCodec.builder(LandmarkBlock.class, LandmarkBlock::new)
            .append(new KeyedCodec<>("LandmarkType", Codec.STRING, true), (landmark, s) -> landmark.landmarkAssetId = s, landmark -> landmark.landmarkAssetId)
            .add()
            .append(new KeyedCodec<>("LandmarkUUID", Codec.UUID_BINARY), (landmark, s) -> landmark.landmarkUniqueId = s, landmark -> landmark.landmarkUniqueId)
            .add()
            .append(new KeyedCodec<>("LandmarkName", Codec.STRING), (landmark, s) -> landmark.landmarkName = s, landmark -> landmark.landmarkName)
            .add()
            .build();
    HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private String landmarkAssetId;
    private UUID landmarkUniqueId;
    private String landmarkName;

    public LandmarkBlock() {

    }

    public void onTick(int x, int y, int z, World world) {
        var poi = LandmarkPlugin.get().getPoiManager().getPoi(getLandmarkUniqueId());
        if (poi != null) {
            setLandmarkName(poi.name());
        }
        var store = world.getEntityStore().getStore();
        SpatialResource<Ref<EntityStore>, EntityStore> spatialresource = store.getResource(
                EntityModule.get().getPlayerSpatialResourceType()
        );
        ObjectList<Ref<EntityStore>> objectlist = SpatialResource.getThreadLocalReferenceList();
        spatialresource.getSpatialStructure().collect(new Vector3d(x, y, z), 75.0, objectlist);
        for (var playerRef : objectlist) {
            var playerLandmarkData = store.getComponent(playerRef, PlayerLandmarkData.getComponentType());
            if (playerLandmarkData.hasDiscoveredLandmark(getLandmarkUniqueId())) {
                continue;
            }
            var transform = store.getComponent(playerRef, TransformComponent.getComponentType());
            if (transform == null) {
                continue;
            }
            var position = transform.getPosition();
            if (position.distanceSquaredTo(x, y, z) <= 25) {
                var player = store.getComponent(playerRef, Player.getComponentType());
                LandmarkPlugin.get().getLogger().atInfo().log("Player " + player.getDisplayName() + " discovered " + landmarkAssetId + "-" + landmarkUniqueId + " at " + new Vector3i(x, y, z) + " in world " + world.getName());
                playerLandmarkData.addDiscoveredLandmark(getLandmarkUniqueId());
                ParticleUtil.spawnParticleEffect("Teleport", x, y, z, objectlist, store);
                world.execute(() -> {
                    var poidata = LandmarkPlugin.get().getPoiManager().getPoi(getLandmarkUniqueId());
                    String name = poidata != null ? poidata.name() : getLandmarkName();
                    EventTitleUtil.showEventTitleToPlayer(player.getPlayerRef(), Message.raw(name), Message.raw("Landmark discovered"), true);
                    int i = SoundEvent.getAssetMap().getIndex("SFX_Discovery_Z1_Medium");
                    if (i != Integer.MIN_VALUE) {
                        SoundUtil.playSoundEvent2d(playerRef, i, SoundCategory.UI, store);
                    }
                });
            }
        }
    }

    public String getLandmarkUniqueId() {
        if(landmarkUniqueId == null) {
            landmarkUniqueId = UUID.randomUUID();
        }
        return landmarkUniqueId.toString();
    }

    public String getLandmarkName() {
        if (landmarkName == null) {
            landmarkName = RANDOM_NAMES[new Random().nextInt(RANDOM_NAMES.length)];
        }
        return landmarkName;
    }

    public void setLandmarkName(String name) {
        this.landmarkName = name;
    }

    public PoiManager.LandmarkType getType() {
        return switch (landmarkAssetId) {
            case "Waypoint" -> PoiManager.LandmarkType.WAYPOINT;
            case "POI" -> PoiManager.LandmarkType.POINT_OF_INTEREST;
            // Fallback
            default -> PoiManager.LandmarkType.POINT_OF_INTEREST;
        };
    }

    @NullableDecl
    public Instant getNextScheduledTick(WorldTimeResource worldTimeResource) {
        Instant instant = worldTimeResource.getGameTime();
        return instant.plus(30, ChronoUnit.SECONDS);
    }

    @NullableDecl
    public Component<ChunkStore> clone() {
        LandmarkBlock clone = new LandmarkBlock();
        clone.landmarkAssetId = this.landmarkAssetId;
        clone.landmarkUniqueId = this.landmarkUniqueId;
        clone.landmarkName = this.landmarkName;
        return clone;
    }

}
