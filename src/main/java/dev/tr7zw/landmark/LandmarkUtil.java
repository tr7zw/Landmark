package dev.tr7zw.landmark;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.spatial.*;
import com.hypixel.hytale.math.vector.*;
import com.hypixel.hytale.server.core.modules.entity.*;
import com.hypixel.hytale.server.core.universe.world.*;
import com.hypixel.hytale.server.core.universe.world.storage.*;
import it.unimi.dsi.fastutil.objects.*;

public class LandmarkUtil {

    public static void spawnParticle(double x, double y, double z, String particleType, Store<EntityStore> store) {
        SpatialResource<Ref<EntityStore>, EntityStore> spatialresource = store.getResource(
                EntityModule.get().getPlayerSpatialResourceType()
        );
        ObjectList<Ref<EntityStore>> objectlist = SpatialResource.getThreadLocalReferenceList();
        spatialresource.getSpatialStructure().collect(new Vector3d(x, y, z), 75.0, objectlist);
        ParticleUtil.spawnParticleEffect(particleType, x, y, z, objectlist, store);
    }

}
