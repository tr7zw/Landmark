package dev.tr7zw.landmark;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.*;
import com.hypixel.hytale.server.core.command.system.arguments.system.*;
import com.hypixel.hytale.server.core.command.system.arguments.types.*;
import com.hypixel.hytale.server.core.command.system.basecommands.*;
import com.hypixel.hytale.server.core.entity.entities.*;
import com.hypixel.hytale.server.core.universe.*;
import com.hypixel.hytale.server.core.universe.world.*;
import com.hypixel.hytale.server.core.universe.world.storage.*;
import dev.tr7zw.landmark.ecs.*;
import dev.tr7zw.landmark.ui.*;
import dev.tr7zw.landmark.util.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.*;

public class LandmarkCommand extends AbstractCommandCollection {


    public LandmarkCommand() {
        super("landmark", "Commands to use and manage Landmarks");
        this.addSubCommand(new ClearPlayerCommand());
        this.addSubCommand(new TeleportCommand());
        this.addSubCommand(new ManageCommand());
        this.addSubCommand(new ListCommand());
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
            LandmarkUtil.teleportToPOI(context, ref, poi);
        }
    }

    private static class ListCommand extends AbstractCommand {

        public ListCommand() {
            super("list", "landmark.commands.list.desc");
            this.requirePermission("tr7zw.landmark.command.landmark.tp");
        }

        @Override
        protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
            CommandSender sender = context.sender();
            if (sender instanceof Player player) {
                Ref<EntityStore> ref = player.getReference();
                if (ref != null && ref.isValid()) {
                    Store<EntityStore> store = ref.getStore();
                    World world = store.getExternalData().getWorld();
                    return CompletableFuture.runAsync(() -> {
                        var playerLandmarkData = context.senderAsPlayerRef().getStore().getComponent(ref, PlayerLandmarkData.getComponentType());
                        PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
                        if (playerRefComponent != null) {
                            Map<String, PoiManager.PoiData> pois = new HashMap<>();
                            for(PoiManager.PoiData poi : LandmarkPlugin.get().getPoiManager().getAllPois()) {
                                if(poi.type() == PoiManager.LandmarkType.WAYPOINT && playerLandmarkData != null && playerLandmarkData.hasDiscoveredLandmark(poi.id())) {
                                    pois.put(poi.id(), poi);
                                }
                            }
                            player.getPageManager().openCustomPage(ref, store, new LandmarkListPage(playerRefComponent, pois, (selectedId) -> {
                                    LandmarkUtil.teleportToPOI(context, ref, pois.get(selectedId));
                            }));
                        }
                    }, world);
                }
            }
            return CompletableFuture.completedFuture(null);
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