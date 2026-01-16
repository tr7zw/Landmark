package dev.tr7zw.landmark;

import com.hypixel.hytale.builtin.teleport.components.*;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.vector.*;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.*;
import com.hypixel.hytale.server.core.command.system.arguments.system.*;
import com.hypixel.hytale.server.core.command.system.arguments.types.*;
import com.hypixel.hytale.server.core.command.system.basecommands.*;
import com.hypixel.hytale.server.core.entity.entities.*;
import com.hypixel.hytale.server.core.modules.entity.component.*;
import com.hypixel.hytale.server.core.modules.entity.teleport.*;
import com.hypixel.hytale.server.core.universe.*;
import com.hypixel.hytale.server.core.universe.world.*;
import com.hypixel.hytale.server.core.universe.world.storage.*;

import javax.annotation.Nonnull;
import java.util.concurrent.*;

public class LandmarkCommand extends AbstractCommandCollection {


    public LandmarkCommand() {
        super("landmark", "Commands to use and manage Landmarks");
        this.addSubCommand(new ClearPlayerCommand());
        this.addSubCommand(new TeleportCommand());
        this.addSubCommand(new ManageCommand());
    }

    private static class TeleportCommand extends CommandBase {

        @Nonnull
        private final RequiredArg<String> nameArg = this.withRequiredArg("id", "landmark.commands.tp.id.desc", ArgTypes.STRING);

        public TeleportCommand() {
            super("tp", "landmark.commands.tp.desc");
            this.requirePermission("tr7zw.landmark.command.landmark.tp");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String id = this.nameArg.get(context);
            PoiManager.PoiData poi = LandmarkPlugin.get().getPoiManager().getPoi(id);
            if (poi == null || poi.type() != PoiManager.LandmarkType.WAYPOINT) {
                context.sendMessage(Message.raw("No waypoint with id " + id + " found."));
                return;
            }
            var ref = context.senderAsPlayerRef();
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
                    double x = poi.x();
                    double z = poi.z();
                    double y = poi.y();
                    float yaw = Float.NaN;
                    float pitch = Float.NaN;
                    float roll = Float.NaN;
                    Teleport teleport = new Teleport(new Vector3d(x, y, z), new Vector3f(vector3f1.getPitch(), yaw, vector3f1.getRoll()))
                            .withHeadRotation(new Vector3f(pitch, yaw, roll));
                    store.addComponent(ref, Teleport.getComponentType(), teleport);
                    Player player = store.getComponent(ref, Player.getComponentType());
                    if (player != null) {
                        store.ensureAndGetComponent(ref, TeleportHistory.getComponentType())
                                .append(world, vector3d, vector3f, String.format("Teleport to %s(%s) (%s, %s, %s)", poi.name(), poi.id(), x, y, z));
                    }
                });
            }
        }
    }

    private static class ManageCommand extends AbstractCommand {

        @Nonnull
        private final RequiredArg<String> nameArg = this.withRequiredArg("id", "landmark.commands.id.desc", ArgTypes.STRING);

        public ManageCommand() {
            super("manage", "landmark.commands.manage.desc");
            this.requirePermission("tr7zw.landmark.command.landmark.manage");
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
            String id = this.nameArg.get(context);
            PoiManager.PoiData poi = LandmarkPlugin.get().getPoiManager().getPoi(id);
            if (poi == null || poi.type() != PoiManager.LandmarkType.WAYPOINT) {
                context.sendMessage(Message.raw("No waypoint with id " + id + " found."));
                return CompletableFuture.completedFuture(null);
            }
            CommandSender sender = context.sender();
            if (sender instanceof Player player) {
                Ref<EntityStore> ref = player.getReference();
                if (ref != null && ref.isValid()) {
                    Store<EntityStore> store = ref.getStore();
                    World world = store.getExternalData().getWorld();
                    return CompletableFuture.runAsync(() -> {
                        PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
                        if (playerRefComponent != null) {
                            player.getPageManager().openCustomPage(ref, store, new RenameUI(playerRefComponent, poi));
                        }
                    }, world);
                }
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class ClearPlayerCommand extends CommandBase {

        @Nonnull
        private final OptionalArg<PlayerRef> playerArg = this.withOptionalArg("player", "commands.argtype.player.desc", ArgTypes.PLAYER_REF);

        public ClearPlayerCommand() {
            super("clear", "landmark.commands.clear.desc", true);
            this.requirePermission("tr7zw.landmark.command.landmark.manage");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            Ref<EntityStore> ref;
            if (this.playerArg.provided(context)) {
                ref = this.playerArg.get(context).getReference();
            } else {
                ref = context.senderAsPlayerRef();
            }
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                World world = store.getExternalData().getWorld();
                world.execute(() -> {
                    var playerLandmarkData = context.senderAsPlayerRef().getStore().getComponent(ref, PlayerLandmarkData.getComponentType());
                    var player = context.senderAsPlayerRef().getStore().getComponent(ref, Player.getComponentType());
                    assert playerLandmarkData != null;
                    assert player != null;
                    playerLandmarkData.reset();
                    context.sendMessage(Message.raw("Cleared landmarks from " + player.getDisplayName()));
                });
            }
        }
    }
}