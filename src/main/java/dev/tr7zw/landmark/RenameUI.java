package dev.tr7zw.landmark;

import com.hypixel.hytale.assetstore.*;
import com.hypixel.hytale.codec.*;
import com.hypixel.hytale.codec.builder.*;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.protocol.packets.interface_.*;
import com.hypixel.hytale.server.core.*;
import com.hypixel.hytale.server.core.asset.type.item.config.*;
import com.hypixel.hytale.server.core.entity.entities.*;
import com.hypixel.hytale.server.core.entity.entities.player.pages.*;
import com.hypixel.hytale.server.core.ui.*;
import com.hypixel.hytale.server.core.ui.builder.*;
import com.hypixel.hytale.server.core.universe.*;
import com.hypixel.hytale.server.core.universe.world.storage.*;

import javax.annotation.*;

public class RenameUI extends InteractiveCustomUIPage<RenameUI.ConfigGuiData> {

    private final PoiManager.PoiData poi;

    public RenameUI(@Nonnull PlayerRef playerRef, PoiManager.PoiData poi) {
        super(playerRef, CustomPageLifetime.CanDismiss, ConfigGuiData.CODEC);
        this.poi = poi;
    }

    public static class ConfigGuiData {
        public static final BuilderCodec<ConfigGuiData> CODEC = BuilderCodec.<ConfigGuiData>builder(ConfigGuiData.class, ConfigGuiData::new)
                .addField(new KeyedCodec("@Name", Codec.STRING), (entry, s) -> entry.name = s, (entry) -> entry.name)
                .build();

        private String name;

    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {

        uiCommandBuilder.append("Pages/LandmarkRename.ui");

        uiCommandBuilder.set("#Name.Value", poi.name());
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#Name", EventData.of("@Name", "#Name.Value"), false);
    }

    @Override
    public void handleDataEvent( Ref<EntityStore> ref,  Store<EntityStore> store,  RenameUI.ConfigGuiData data) {
        super.handleDataEvent(ref, store, data);
        System.out.println(data.name);
        if(data.name != null && !data.name.isBlank()) {
            LandmarkPlugin.get().getPoiManager().renamePoi(poi.id(), data.name);
        }
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        this.sendUpdate(commandBuilder, eventBuilder, false);
    }


}