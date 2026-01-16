package dev.tr7zw.landmark;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.*;
import com.hypixel.hytale.component.system.*;
import com.hypixel.hytale.component.system.tick.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.*;
import com.hypixel.hytale.server.core.asset.type.blocktick.*;
import com.hypixel.hytale.server.core.entity.entities.*;
import com.hypixel.hytale.server.core.modules.block.*;
import com.hypixel.hytale.server.core.modules.time.*;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.chunk.*;
import com.hypixel.hytale.server.core.universe.world.chunk.section.*;
import com.hypixel.hytale.server.core.universe.world.events.*;
import com.hypixel.hytale.server.core.universe.world.storage.*;
import org.checkerframework.checker.nullness.compatqual.*;

import javax.annotation.Nonnull;
import java.time.*;

public class LandmarkPlugin extends JavaPlugin {

    private static LandmarkPlugin instance;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private PoiManager poiManager;
    private ComponentType<ChunkStore, LandmarkBlock> landmarkComponent;
    private ComponentType<EntityStore, PlayerLandmarkData> playerLandmarkDataComponent;

    public LandmarkPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
        poiManager = new PoiManager();
    }

    @Override
    protected void setup() {
        instance = this;
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        this.getCommandRegistry().registerCommand(new LandmarkCommand());
        playerLandmarkDataComponent = getEntityStoreRegistry().registerComponent(PlayerLandmarkData.class, "PlayerLandmarkData", PlayerLandmarkData.CODEC);
        this.landmarkComponent = this.getChunkStoreRegistry()
                .registerComponent(LandmarkBlock.class, "LandmarkBlockData", LandmarkBlock.CODEC);
        this.getChunkStoreRegistry().registerSystem(new OnLandmarkAdded());
        this.getChunkStoreRegistry().registerSystem(new Ticking());
        this.getEntityStoreRegistry().registerSystem(new PlayerSpawnedSystem());
        this.getEventRegistry()
                .registerGlobal(AddWorldEvent.class, event -> event.getWorld().getWorldMapManager().getMarkerProviders().put("landmark_plugin", new LandmarkPoiProvider()));
    }

    public ComponentType<ChunkStore, LandmarkBlock> getLandmarkBlockComponent() {
        return landmarkComponent;
    }

    public ComponentType<EntityStore, PlayerLandmarkData> getPlayerLandmarkDataComponent() {
        return playerLandmarkDataComponent;
    }

    public PoiManager getPoiManager() {
        return poiManager;
    }

    public static LandmarkPlugin get() {
        return instance;
    }

    public static class PlayerSpawnedSystem extends RefSystem<EntityStore> {
        @Nonnull
        @Override
        public Query<EntityStore> getQuery() {
            return Player.getComponentType();
        }

        @Override
        public void onEntityAdded(
                @Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer
        ) {
            Player player = store.getComponent(ref, Player.getComponentType());
            assert player != null;
            player.getWorld().execute(() -> {
                var playerLandmarkData = store.getComponent(ref, PlayerLandmarkData.getComponentType());
                if (playerLandmarkData == null) {
                    playerLandmarkData = new PlayerLandmarkData();
                    store.addComponent(ref, PlayerLandmarkData.getComponentType(), playerLandmarkData);
                }
                get().getPoiManager().setPlayerLandmarkData(player.getUuid(), playerLandmarkData);
            });
        }

        @Override
        public void onEntityRemove(
                @Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer
        ) {
            Player player = store.getComponent(ref, Player.getComponentType());
            assert player != null;
            get().getPoiManager().removePlayerLandmarkData(player.getUuid());
        }
    }


    public static class OnLandmarkAdded extends RefSystem<ChunkStore> {
        private final Query<ChunkStore> QUERY = Query.and(BlockModule.BlockStateInfo.getComponentType(), get().getLandmarkBlockComponent());

        @Override
        public void onEntityAdded(
                @NonNullDecl Ref<ChunkStore> ref,
                @NonNullDecl AddReason reason,
                @NonNullDecl Store<ChunkStore> store,
                @NonNullDecl CommandBuffer<ChunkStore> commandBuffer
        ) {
            LandmarkBlock landmarkblock = commandBuffer.getComponent(ref, get().getLandmarkBlockComponent());
            if (landmarkblock != null) {
                WorldTimeResource worldtimeresource = commandBuffer.getExternalData()
                        .getWorld()
                        .getEntityStore()
                        .getStore()
                        .getResource(WorldTimeResource.getResourceType());
                BlockModule.BlockStateInfo blockmodule$blockstateinfo = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());

                assert blockmodule$blockstateinfo != null;

                int i = ChunkUtil.xFromBlockInColumn(blockmodule$blockstateinfo.getIndex());
                int j = ChunkUtil.yFromBlockInColumn(blockmodule$blockstateinfo.getIndex());
                int k = ChunkUtil.zFromBlockInColumn(blockmodule$blockstateinfo.getIndex());
                BlockChunk blockchunk = commandBuffer.getComponent(blockmodule$blockstateinfo.getChunkRef(), BlockChunk.getComponentType());

                BlockSection blocksection = blockchunk.getSectionAtBlockY(j);
                blocksection.scheduleTick(ChunkUtil.indexBlock(i, j, k), landmarkblock.getNextScheduledTick(worldtimeresource));

                ChunkColumn chunkcolumn = commandBuffer.getComponent(blockmodule$blockstateinfo.getChunkRef(), ChunkColumn.getComponentType());
                assert chunkcolumn != null;
                Ref<ChunkStore> refx = chunkcolumn.getSection(ChunkUtil.chunkCoordinate(j));
                assert refx != null;
                ChunkSection chunksection = commandBuffer.getComponent(refx, ChunkSection.getComponentType());
                int x = ChunkUtil.worldCoordFromLocalCoord(chunksection.getX(), i);
                int y = ChunkUtil.worldCoordFromLocalCoord(chunksection.getY(), j);
                int z = ChunkUtil.worldCoordFromLocalCoord(chunksection.getZ(), k);
                LandmarkPlugin.get().getPoiManager().addPoi(landmarkblock.getLandmarkUniqueId(), landmarkblock.getType(), landmarkblock.getLandmarkName(), x, y, z);
                LOGGER.atInfo().log("Landmark placed at " + x + ", " + y + ", " + z + " with "+ landmarkblock.getType() +" ID: " + landmarkblock.getLandmarkUniqueId());
            }
        }

        @Override
        public void onEntityRemove(
                @NonNullDecl Ref<ChunkStore> ref,
                @NonNullDecl RemoveReason reason,
                @NonNullDecl Store<ChunkStore> store,
                @NonNullDecl CommandBuffer<ChunkStore> commandBuffer
        ) {
            if (reason != RemoveReason.UNLOAD) {
                LandmarkBlock landmarkblock = commandBuffer.getComponent(ref, get().getLandmarkBlockComponent());
                if (landmarkblock != null) {
                    BlockModule.BlockStateInfo blockmodule$blockstateinfo = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());

                    assert blockmodule$blockstateinfo != null;

                    int i = ChunkUtil.xFromBlockInColumn(blockmodule$blockstateinfo.getIndex());
                    int j = ChunkUtil.yFromBlockInColumn(blockmodule$blockstateinfo.getIndex());
                    int k = ChunkUtil.zFromBlockInColumn(blockmodule$blockstateinfo.getIndex());

                    ChunkColumn chunkcolumn = commandBuffer.getComponent(blockmodule$blockstateinfo.getChunkRef(), ChunkColumn.getComponentType());
                    assert chunkcolumn != null;
                    Ref<ChunkStore> refx = chunkcolumn.getSection(ChunkUtil.chunkCoordinate(j));
                    assert refx != null;
                    ChunkSection chunksection = commandBuffer.getComponent(refx, ChunkSection.getComponentType());
                    int x = ChunkUtil.worldCoordFromLocalCoord(chunksection.getX(), i);
                    int y = ChunkUtil.worldCoordFromLocalCoord(chunksection.getY(), j);
                    int z = ChunkUtil.worldCoordFromLocalCoord(chunksection.getZ(), k);
                    var poi = LandmarkPlugin.get().getPoiManager().getPoi(landmarkblock.getLandmarkUniqueId());
                    if (poi != null) {
                        landmarkblock.setLandmarkName(poi.name());
                    }
                    LandmarkPlugin.get().getPoiManager().removePoi(landmarkblock.getLandmarkUniqueId());
                    LOGGER.atInfo().log("Landmark removed at " + x + ", " + y + ", " + z + " with " + landmarkblock.getType() + " ID: " + landmarkblock.getLandmarkUniqueId());
                }
            }
        }

        @NullableDecl
        @Override
        public Query<ChunkStore> getQuery() {
            return QUERY;
        }
    }

    public static class Ticking extends EntityTickingSystem<ChunkStore> {
        private static final Query<ChunkStore> QUERY = Query.and(BlockSection.getComponentType(), ChunkSection.getComponentType());

        @Override
        public void tick(
                float dt,
                int index,
                @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk,
                @Nonnull Store<ChunkStore> store,
                @Nonnull CommandBuffer<ChunkStore> commandBuffer
        ) {
            BlockSection blocksection = archetypeChunk.getComponent(index, BlockSection.getComponentType());

            assert blocksection != null;

            if (blocksection.getTickingBlocksCountCopy() != 0) {
                ChunkSection chunksection = archetypeChunk.getComponent(index, ChunkSection.getComponentType());

                assert chunksection != null;

                BlockComponentChunk blockcomponentchunk = commandBuffer.getComponent(
                        chunksection.getChunkColumnReference(), BlockComponentChunk.getComponentType()
                );

                assert blockcomponentchunk != null;

                var worldtimeresource = commandBuffer.getExternalData()
                        .getWorld()
                        .getEntityStore()
                        .getStore()
                        .getResource(WorldTimeResource.getResourceType());

                Ref<ChunkStore> ref = archetypeChunk.getReferenceTo(index);
                blocksection.forEachTicking(
                        blockcomponentchunk, commandBuffer, chunksection.getY(), (blockComponentChunk1, commandBuffer1, localX, localY, localZ, blockId) -> {
                            Ref<ChunkStore> ref1 = blockComponentChunk1.getEntityReference(ChunkUtil.indexBlockInColumn(localX, localY, localZ));
                            if (ref1 == null) {
                                return BlockTickStrategy.IGNORED;
                            } else {
                                LandmarkBlock landmarkBlock = commandBuffer.getComponent(ref1, get().getLandmarkBlockComponent());
                                if (landmarkBlock != null) {
                                    int x = ChunkUtil.worldCoordFromLocalCoord(chunksection.getX(), localX);
                                    int y = ChunkUtil.worldCoordFromLocalCoord(chunksection.getY(), localY);
                                    int z = ChunkUtil.worldCoordFromLocalCoord(chunksection.getZ(), localZ);
                                    landmarkBlock.onTick(x, y, z, commandBuffer.getExternalData().getWorld());
                                    Instant instant = landmarkBlock.getNextScheduledTick(worldtimeresource);
                                    if (instant != null) {
                                        blocksection.scheduleTick(ChunkUtil.indexBlock(localX, localY, localZ), instant);
                                    }
                                    return BlockTickStrategy.SLEEP;
                                } else {
                                    return BlockTickStrategy.IGNORED;
                                }
                            }
                        }
                );
            }
        }

        @NullableDecl
        @Override
        public Query<ChunkStore> getQuery() {
            return QUERY;
        }
    }

}