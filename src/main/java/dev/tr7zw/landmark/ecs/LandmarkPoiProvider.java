package dev.tr7zw.landmark.ecs;

import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.packets.worldmap.*;
import com.hypixel.hytale.server.core.*;
import com.hypixel.hytale.server.core.asset.type.gameplay.*;
import com.hypixel.hytale.server.core.entity.entities.*;
import com.hypixel.hytale.server.core.universe.*;
import com.hypixel.hytale.server.core.universe.world.*;
import com.hypixel.hytale.server.core.universe.world.worldmap.*;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.*;
import com.hypixel.hytale.server.core.util.*;
import dev.tr7zw.landmark.*;
import dev.tr7zw.landmark.util.*;
import org.checkerframework.checker.nullness.compatqual.*;

import java.util.*;

public class LandmarkPoiProvider implements WorldMapManager.MarkerProvider {

    @Override
    public void update(@NonNullDecl World world, @NonNullDecl Player player, @NonNullDecl MarkersCollector markersCollector) {
        var playerLandmarkData = LandmarkPlugin.get().getPoiManager().getPlayerLandmarkData(player);
        for (PoiManager.PoiData poi : LandmarkPlugin.get().getPoiManager().getAllPois()) {
            if(!poi.worldName().equals(world.getName())) {
                continue;
            }
            boolean discovered = playerLandmarkData != null && playerLandmarkData.hasDiscoveredLandmark(poi.id());
            String id = (discovered ? "Explored" : "Unexplored") + "POI-" + poi.id();
            var name = Message.raw(discovered ? "Waypoint - " + poi.name() : "Undiscovered Waypoint").getFormattedMessage();
            markersCollector.addIgnoreViewDistance(new MapMarker(id, name, null, discovered ? "Landmark_Warp.png" : "Landmark_Warp_Undiscovered.png", PositionUtil.toTransformPacket(new Transform(poi.x(), poi.y(), poi.z())), createContextMenuItems(poi, discovered, player), null));

        }
    }

    private ContextMenuItem[] createContextMenuItems(PoiManager.PoiData poi, boolean discovered, Player player) {
        List<ContextMenuItem> contextMenuItemList = new ArrayList<>();
        if (discovered && poi.type() == PoiManager.LandmarkType.WAYPOINT) {
            contextMenuItemList.add(new ContextMenuItem("Click to teleport", "landmark tp " + poi.id()));
        }
        if (discovered && player.hasPermission("tr7zw.landmark.command.landmark.manage")) {
            contextMenuItemList.add(new ContextMenuItem("Rename", "landmark manage " + poi.id()));
        }
        if(contextMenuItemList.isEmpty()) {
            return null;
        }
        return contextMenuItemList.toArray(new ContextMenuItem[0]);
    }

}
