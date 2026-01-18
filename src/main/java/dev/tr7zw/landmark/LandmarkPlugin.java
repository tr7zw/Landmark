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
import dev.tr7zw.landmark.ecs.*;
import dev.tr7zw.landmark.util.*;
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
        this.getChunkStoreRegistry().registerSystem(new LandmarkSystems.OnLandmarkAdded());
        this.getChunkStoreRegistry().registerSystem(new LandmarkSystems.Ticking());
        this.getEntityStoreRegistry().registerSystem(new LandmarkSystems.PlayerSpawnedSystem());
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

}