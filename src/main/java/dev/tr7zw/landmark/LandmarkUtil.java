package dev.tr7zw.landmark;

import com.hypixel.hytale.builtin.teleport.components.*;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.spatial.*;
import com.hypixel.hytale.math.vector.*;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.interface_.*;
import com.hypixel.hytale.server.core.*;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.*;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.command.system.*;
import com.hypixel.hytale.server.core.entity.entities.*;
import com.hypixel.hytale.server.core.modules.entity.*;
import com.hypixel.hytale.server.core.modules.entity.component.*;
import com.hypixel.hytale.server.core.modules.entity.teleport.*;
import com.hypixel.hytale.server.core.universe.world.*;
import com.hypixel.hytale.server.core.universe.world.storage.*;
import it.unimi.dsi.fastutil.objects.*;
import org.checkerframework.checker.nullness.compatqual.*;

public class LandmarkUtil {

    public static void spawnParticle(double x, double y, double z, String particleType, Store<EntityStore> store) {
        SpatialResource<Ref<EntityStore>, EntityStore> spatialresource = store.getResource(
                EntityModule.get().getPlayerSpatialResourceType()
        );
        ObjectList<Ref<EntityStore>> objectlist = SpatialResource.getThreadLocalReferenceList();
        spatialresource.getSpatialStructure().collect(new Vector3d(x, y, z), 75.0, objectlist);
        ParticleUtil.spawnParticleEffect(particleType, x, y, z, objectlist, store);
    }

    public static void teleportToPOI(@NonNullDecl CommandContext context, Ref<EntityStore> ref, PoiManager.PoiData poi) {
        if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            world.execute(() -> {
                var playerLandmarkData = context.senderAsPlayerRef().getStore().getComponent(ref, PlayerLandmarkData.getComponentType());
                if (playerLandmarkData == null || !playerLandmarkData.hasDiscoveredLandmark(poi.id())) {
                    context.sendMessage(Message.raw("You have not discovered this landmark yet."));
                    return;
                }
                TransformComponent transformcomponent = store.getComponent(ref, TransformComponent.getComponentType());

                assert transformcomponent != null;

                HeadRotation headrotation = store.getComponent(ref, HeadRotation.getComponentType());

                assert headrotation != null;

                Vector3d vector3d = transformcomponent.getPosition().clone();
                Vector3f vector3f = headrotation.getRotation().clone();
                Vector3f vector3f1 = transformcomponent.getRotation().clone();
                double x = poi.x() + 0.5;
                double z = poi.z() + 0.5;
                double y = poi.y() + 0.2;
                Teleport teleport = new Teleport(new Vector3d(x, y, z), new Vector3f(vector3f1.getPitch(), vector3f1.getYaw(), vector3f1.getRoll()));
                store.addComponent(ref, Teleport.getComponentType(), teleport);
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    player.getPageManager().setPage(ref, store, Page.None);
                    store.ensureAndGetComponent(ref, TeleportHistory.getComponentType())
                            .append(world, vector3d, vector3f, String.format("Teleport to %s(%s) (%s, %s, %s)", poi.name(), poi.id(), x, y, z));
                    int i = SoundEvent.getAssetMap().getIndex("SFX_Portal_Neutral_Teleport_Local");
                    if (i != Integer.MIN_VALUE) {
                        SoundUtil.playSoundEvent2d(ref, i, SoundCategory.UI, store);
                    }
                    spawnParticle(x, y, z, "Lightning", store);
                }
            });
        }
    }

}
