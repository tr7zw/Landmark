package dev.tr7zw.landmark.util;

import dev.tr7zw.landmark.ecs.*;

import java.util.*;

public class PoiManager {

    private final Map<String, PoiData> poiDataMap = new HashMap<>();
    //FIXME: there has to be a better way to do this
    private final HashMap<UUID, PlayerLandmarkData> playerDataMap = new HashMap<>();

    public void addPoi(String id, LandmarkType type, String name, int x, int y, int z, String worldName) {
        poiDataMap.put(id, new PoiData(id, type, name, x, y, z, worldName));
    }

    public void removePoi(String id) {
        poiDataMap.remove(id);
    }

    public Collection<PoiData> getAllPois() {
        return poiDataMap.values();
    }

    public PoiData getPoi(String id) {
        PoiData poi = poiDataMap.get(id);
        if (poi != null) {
            return poi;
        }
        for (PoiData p : poiDataMap.values()) {
            if (p.name().replace(' ', '_').equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
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
            poiDataMap.put(id, new PoiData(poi.id(), poi.type(), name, poi.x(), poi.y(), poi.z(), poi.worldName()));
        }
    }

    public record PoiData(String id, LandmarkType type, String name, int x, int y, int z, String worldName) {

    }

    public enum LandmarkType {
        WAYPOINT,
        POINT_OF_INTEREST
    }

}
