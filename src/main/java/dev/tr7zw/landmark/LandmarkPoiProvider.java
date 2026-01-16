package dev.tr7zw.landmark;

import com.hypixel.hytale.math.vector.*;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.worldmap.*;
import com.hypixel.hytale.server.core.asset.type.gameplay.*;
import com.hypixel.hytale.server.core.universe.world.*;
import com.hypixel.hytale.server.core.universe.world.worldmap.*;
import com.hypixel.hytale.server.core.util.*;

import java.util.*;

public class LandmarkPoiProvider implements WorldMapManager.MarkerProvider {

    @Override
    public void update(World world, GameplayConfig gameplayConfig, WorldMapTracker worldMapTracker, int chunkViewRadiusSquared, int playerChunkX, int playerChunkZ) {
        var playerLandmarkData = LandmarkPlugin.get().getPoiManager().getPlayerLandmarkData(worldMapTracker.getPlayer().getUuid());
        for (PoiManager.PoiData poi : LandmarkPlugin.get().getPoiManager().getAllPois()) {
            boolean discovered = playerLandmarkData != null && playerLandmarkData.hasDiscoveredLandmark(poi.id());
            worldMapTracker.trySendMarker(
                    chunkViewRadiusSquared,
                    playerChunkX,
                    playerChunkZ,
                    new Vector3d(poi.x(), poi.y(), poi.z()),
                    0,
                    (discovered ? "Explored" : "Unexplored") + "POI-" + poi.id(),
                    discovered ? "Waypoint - " + poi.name() : "Undiscovered Waypoint",
                    poi,
                    (id, name, sp) -> new MapMarker(id, name, discovered ? "Landmark_Warp.png" : "Landmark_Warp_Undiscovered.png", PositionUtil.toTransformPacket(new Transform(sp.x(), sp.y(), sp.z())), createContextMenuItems(poi, discovered, worldMapTracker))
            );
        }
    }

    private ContextMenuItem[] createContextMenuItems(PoiManager.PoiData poi, boolean discovered, WorldMapTracker worldMapTracker) {
        List<ContextMenuItem> contextMenuItemList = new ArrayList<>();
        if (discovered && poi.type() == PoiManager.LandmarkType.WAYPOINT) {
            contextMenuItemList.add(new ContextMenuItem("Click to teleport", "landmark tp " + poi.id()));
        }
        if (discovered && worldMapTracker.getPlayer().hasPermission("tr7zw.landmark.command.landmark.manage")) {
            contextMenuItemList.add(new ContextMenuItem("Rename", "landmark manage " + poi.id()));
        }
        if(contextMenuItemList.isEmpty()) {
            return null;
        }
        return contextMenuItemList.toArray(new ContextMenuItem[0]);
    }
}
