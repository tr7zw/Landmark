package dev.tr7zw.landmark;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.universe.*;
import com.hypixel.hytale.server.core.universe.world.storage.*;

import java.util.*;

public class PoiManager {

    private final Map<String, PoiData> poiDataMap = new HashMap<>();
    //FIXME: there has to be a better way to do this
    private final HashMap<UUID, PlayerLandmarkData> playerDataMap = new HashMap<>();

    public void addPoi(String id, LandmarkType type, String name, int x, int y, int z) {
        poiDataMap.put(id, new PoiData(id, type, name, x, y, z));
    }

    public void removePoi(String id) {
        poiDataMap.remove(id);
    }

    public Collection<PoiData> getAllPois() {
        return poiDataMap.values();
    }

    public PoiData getPoi(String id) {
        return poiDataMap.get(id);
    }

    public void setPlayerLandmarkData(UUID uuid, PlayerLandmarkData data) {
        playerDataMap.put(uuid, data);
    }

    public void removePlayerLandmarkData(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    public PlayerLandmarkData getPlayerLandmarkData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public void renamePoi(String id, String name) {
        PoiData poi = poiDataMap.get(id);
        if (poi != null) {
            poiDataMap.put(id, new PoiData(poi.id(), poi.type(), name, poi.x(), poi.y(), poi.z()));
        }
    }

    public record PoiData(String id, LandmarkType type, String name, int x, int y, int z) {

    }

    public enum LandmarkType {
        WAYPOINT,
        POINT_OF_INTEREST
    }

}
