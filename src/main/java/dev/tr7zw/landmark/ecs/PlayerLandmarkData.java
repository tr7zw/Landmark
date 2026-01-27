package dev.tr7zw.landmark.ecs;

import com.hypixel.hytale.codec.*;
import com.hypixel.hytale.codec.builder.*;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.universe.world.storage.*;
import dev.tr7zw.landmark.*;

import javax.annotation.*;
import java.util.*;

public class PlayerLandmarkData implements Component<EntityStore> {
    @Nonnull
    public static final BuilderCodec<PlayerLandmarkData> CODEC = BuilderCodec.builder(PlayerLandmarkData.class, PlayerLandmarkData::new)
            .append(new KeyedCodec<>("DiscoveredLandmarks", BuilderCodec.STRING_ARRAY), (o, i) -> {
                o.discoveredLandmarks = new HashSet<>(Arrays.asList(i));
            }, o ->  {
                return o.discoveredLandmarks.toArray(new String[0]);
            })
            .add()
            .build();

    private Set<String> discoveredLandmarks = new HashSet<>();
    private long tpCooldown = 0;

    @Nonnull
    public static ComponentType<EntityStore, PlayerLandmarkData> getComponentType() {
        return LandmarkPlugin.get().getPlayerLandmarkDataComponent();
    }

    public PlayerLandmarkData() {
    }

    public boolean hasDiscoveredLandmark(@Nonnull String landmarkId) {
        return discoveredLandmarks.contains(landmarkId);
    }

    public void addDiscoveredLandmark(@Nonnull String landmarkId) {
        discoveredLandmarks.add(landmarkId);
    }

    public boolean isTpOnCooldown() {
        return System.currentTimeMillis() < tpCooldown;
    }

    public void setTpCooldown(long milliseconds) {
        this.tpCooldown = System.currentTimeMillis() + milliseconds;
    }

    @Nonnull
    public PlayerLandmarkData clone() {
        var copy = new PlayerLandmarkData();
        copy.discoveredLandmarks.addAll(discoveredLandmarks);
        return copy;
    }

    public void reset() {
        discoveredLandmarks.clear();
        tpCooldown = 0;
    }
}
