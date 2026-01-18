package dev.tr7zw.landmark.ui;

import com.hypixel.hytale.codec.*;
import com.hypixel.hytale.codec.builder.*;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.protocol.packets.interface_.*;
import com.hypixel.hytale.server.core.entity.entities.*;
import com.hypixel.hytale.server.core.entity.entities.player.pages.*;
import com.hypixel.hytale.server.core.ui.builder.*;
import com.hypixel.hytale.server.core.universe.*;
import com.hypixel.hytale.server.core.universe.world.storage.*;
import dev.tr7zw.landmark.util.*;
import it.unimi.dsi.fastutil.objects.*;

import javax.annotation.*;
import java.util.*;
import java.util.function.*;

public class LandmarkListPage extends InteractiveCustomUIPage<LandmarkListPage.LandmarkListPageEventData> {
    @Nonnull
    private final Consumer<String> callback;
    private final Map<String, PoiManager.PoiData> warps;
    @Nonnull
    private String searchQuery = "";

    public LandmarkListPage(@Nonnull PlayerRef playerRef, Map<String, PoiManager.PoiData> warps, Consumer<String> callback) {
        super(playerRef, CustomPageLifetime.CanDismiss, LandmarkListPageEventData.CODEC);
        this.warps = warps;
        this.callback = callback;
    }

    private void buildWarpList(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        commandBuilder.clear("#WarpList");
        ObjectArrayList<String> warps = new ObjectArrayList(this.warps.keySet());
        if (warps.isEmpty()) {
            commandBuilder.appendInline("#WarpList", "Label { Text: %landmark.waypoint.list.empty; Style: (Alignment: Center); }");
        } else {
            if (!this.searchQuery.isEmpty()) {
                warps.removeIf((w) -> !((PoiManager.PoiData) this.warps.get(w)).name().toLowerCase().contains(this.searchQuery));
            }

            Collections.sort(warps);
            int i = 0;

            for (int bound = warps.size(); i < bound; ++i) {
                String selector = "#WarpList[" + i + "]";
                String warp = (String) warps.get(i);
                commandBuilder.append("#WarpList", "Pages/LandmarkEntryButton.ui");
                commandBuilder.set(selector + " #Name.Text", ((PoiManager.PoiData) this.warps.get(warp)).name());
                commandBuilder.set(selector + " #World.Text", "");// Bad name to show ((PoiManager.PoiData) this.warps.get(warp)).worldName());
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Warp", warp), false);
            }
        }

    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        commandBuilder.append("Pages/LandmarkListPage.ui");
        eventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput", EventData.of("@SearchQuery", "#SearchInput.Value"));
        this.buildWarpList(commandBuilder, eventBuilder);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull LandmarkListPage.LandmarkListPageEventData eventData) {
        if (eventData.getWarp() != null) {
            Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());

            assert playerComponent != null;

            playerComponent.getPageManager().setPage(ref, store, Page.None);
            this.callback.accept(eventData.getWarp());
        } else if (eventData.getSearchQuery() != null) {
            this.searchQuery = eventData.getSearchQuery().trim().toLowerCase();
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            this.buildWarpList(commandBuilder, eventBuilder);
            this.sendUpdate(commandBuilder, eventBuilder, false);
        }

    }

    public static class LandmarkListPageEventData {
        static final String KEY_WARP = "Warp";
        static final String KEY_SEARCH_QUERY = "@SearchQuery";
        @Nonnull
        public static final BuilderCodec<LandmarkListPageEventData> CODEC;
        private String warp;
        private String searchQuery;

        public String getWarp() {
            return this.warp;
        }

        public String getSearchQuery() {
            return this.searchQuery;
        }

        static {
            CODEC = BuilderCodec.builder(LandmarkListPageEventData.class, LandmarkListPageEventData::new)
            .addField(new KeyedCodec("Warp", Codec.STRING), (entry, s) -> entry.warp = s, (entry) -> entry.warp)
            .addField(new KeyedCodec("@SearchQuery", Codec.STRING), (entry, s) -> ((LandmarkListPageEventData)entry).searchQuery = (String)s, (entry) -> ((LandmarkListPageEventData)entry).searchQuery)
            .build();
        }
    }
}