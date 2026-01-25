package dev.tr7zw.landmark.util;

import com.google.gson.*;
import com.hypixel.hytale.server.core.entity.entities.*;
import dev.tr7zw.landmark.*;
import dev.tr7zw.landmark.ecs.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

public class PoiManager {

    private final Map<String, PoiData> poiDataMap = new HashMap<>();
    //FIXME: there has to be a better way to do this
    private final HashMap<Player, PlayerLandmarkData> playerDataMap = new HashMap<>();

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

    public void setPlayerLandmarkData(Player player, PlayerLandmarkData data) {
        playerDataMap.put(player, data);
    }

    public void removePlayerLandmarkData(Player player) {
        playerDataMap.remove(player);
    }

    public PlayerLandmarkData getPlayerLandmarkData(Player player) {
        return playerDataMap.get(player);
    }

    public void renamePoi(String id, String name) {
        PoiData poi = poiDataMap.get(id);
        if (poi != null) {
            poiDataMap.put(id, new PoiData(poi.id(), poi.type(), name, poi.x(), poi.y(), poi.z(), poi.worldName()));
        }
    }

    public void save() {
        try {
            File cacheFile = new File(LandmarkPlugin.get().getDataDirectory().toFile(), "poi_cache.json");
            if(!cacheFile.getParentFile().exists()) {
                cacheFile.getParentFile().mkdirs();
            }
            if(cacheFile.exists()) {
                cacheFile.delete();
            }
            Files.write(cacheFile.toPath(), new Gson().toJson(poiDataMap.values()).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LandmarkPlugin.get().getLogger().at(Level.SEVERE).withCause(e).log("Failed to save POI data");
        }
    }

    public void load() {
        File cacheFile = new File(LandmarkPlugin.get().getDataDirectory().toFile(), "poi_cache.json");
        if (cacheFile.exists()) {
            try {
                String content = Files.readString(cacheFile.toPath(), StandardCharsets.UTF_8);
                PoiData[] pois = new Gson().fromJson(content, PoiData[].class);
                poiDataMap.clear();
                for (PoiData poi : pois) {
                    poiDataMap.put(poi.id(), poi);
                }
            } catch (Exception e) {
                LandmarkPlugin.get().getLogger().at(Level.SEVERE).withCause(e).log("Failed to load POI data");
            }
        }
    }

    public record PoiData(String id, LandmarkType type, String name, int x, int y, int z, String worldName) {

    }

    public enum LandmarkType {
        WAYPOINT,
        POINT_OF_INTEREST
    }

}
