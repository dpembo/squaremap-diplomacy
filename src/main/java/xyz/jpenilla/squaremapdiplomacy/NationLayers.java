package xyz.jpenilla.squaremapdiplomacy;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.MapWorld;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

final class NationLayers {
    private enum Relation { SELF, ALLY, ENEMY, NEUTRAL }

    private final SquaremapDiplomacyPlugin plugin;
    private final DiplomacyConfig config;
    private final Map<Relation, Integer> counts = new java.util.EnumMap<>(Relation.class);
    private boolean logged;
    private final Map<UUID, String> nationNames = new HashMap<>();
    // world name -> layer key -> provider
    private final Map<String, Map<Key, SimpleLayerProvider>> providers = new HashMap<>();
    private final Map<String, Map<Key, Set<Key>>> markerKeys = new HashMap<>();

    NationLayers(final SquaremapDiplomacyPlugin plugin, final DiplomacyConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    void refresh() {
        try {
            this.doRefresh();
        } catch (final Exception ex) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to refresh diplomacy layers", ex);
        }
    }

    private void doRefresh() {
        final var squaremap = SquaremapProvider.get();
        final int blockSize = TownySettings.getTownBlockSize();
        final var nations = TownyAPI.getInstance().getNations();

        this.counts.clear();
        this.nationNames.clear();
        for (final Nation nation : nations) {
            this.nationNames.put(nation.getUUID(), nation.getName());
        }

        for (final World world : Bukkit.getWorlds()) {
            final Optional<MapWorld> mapWorld = squaremap.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world));
            if (mapWorld.isEmpty()) {
                continue;
            }
            this.refreshWorld(mapWorld.get(), world.getName(), nations, blockSize);
        }
        if (!this.logged) {
            this.logged = true;
            this.plugin.getLogger().info("Diplomacy layers built for " + nations.size() + " nations; claim markers by relation: " + this.counts);
        }
    }

    private void refreshWorld(final MapWorld mapWorld, final String worldName, final Iterable<Nation> nations, final int blockSize) {
        final Map<Key, SimpleLayerProvider> worldProviders = this.providers.computeIfAbsent(worldName, k -> new HashMap<>());
        final Map<Key, Set<Key>> worldMarkers = this.markerKeys.computeIfAbsent(worldName, k -> new HashMap<>());
        final Set<Key> live = new HashSet<>();

        for (final Nation nation : nations) {
            final Key layerKey = Key.of("diplomacy_nation_" + nation.getUUID());
            live.add(layerKey);

            SimpleLayerProvider provider = worldProviders.get(layerKey);
            if (provider == null) {
                final UUID id = nation.getUUID();
                provider = SimpleLayerProvider.builder(() -> this.config.layerLabel() + ": " + this.nationNames.getOrDefault(id, "?"))
                    .showControls(true)
                    .defaultHidden(true)
                    .layerPriority(this.config.layerPriority())
                    .zIndex(this.config.layerZIndex())
                    .build();
                worldProviders.put(layerKey, provider);
                if (mapWorld.layerRegistry().hasEntry(layerKey)) {
                    mapWorld.layerRegistry().unregister(layerKey);
                }
                mapWorld.layerRegistry().register(layerKey, provider);
            }

            final Set<Key> previous = worldMarkers.getOrDefault(layerKey, Set.of());
            final Set<Key> current = new HashSet<>();

            this.addClaims(provider, current, worldName, nation, nation, Relation.SELF, blockSize);
            for (final Nation ally : nation.getAllies()) {
                this.addClaims(provider, current, worldName, nation, ally, Relation.ALLY, blockSize);
            }
            for (final Nation enemy : nation.getEnemies()) {
                this.addClaims(provider, current, worldName, nation, enemy, Relation.ENEMY, blockSize);
            }
            for (final Nation other : nations) {
                if (!other.equals(nation) && !nation.hasAlly(other) && !nation.hasEnemy(other)) {
                    this.addClaims(provider, current, worldName, nation, other, Relation.NEUTRAL, blockSize);
                }
            }

            for (final Key stale : previous) {
                if (!current.contains(stale)) {
                    provider.removeMarker(stale);
                }
            }
            worldMarkers.put(layerKey, current);
        }

        for (final Key key : Set.copyOf(worldProviders.keySet())) {
            if (!live.contains(key)) {
                if (mapWorld.layerRegistry().hasEntry(key)) {
                    mapWorld.layerRegistry().unregister(key);
                }
                worldProviders.remove(key);
                worldMarkers.remove(key);
            }
        }
    }

    private void addClaims(
        final SimpleLayerProvider provider,
        final Set<Key> current,
        final String worldName,
        final Nation viewer,
        final Nation owner,
        final Relation relation,
        final int blockSize
    ) {
        if (owner == null) {
            return;
        }
        final Color color = switch (relation) {
            case SELF -> this.config.self();
            case ALLY -> this.config.ally();
            case ENEMY -> this.config.enemy();
            case NEUTRAL -> this.config.neutral();
        };
        final String relationLabel = switch (relation) {
            case SELF -> "Own nation";
            case ALLY -> "Ally of " + ColorCodes.toHtml(viewer.getName());
            case ENEMY -> "At war with " + ColorCodes.toHtml(viewer.getName());
            case NEUTRAL -> "Neutral";
        };

        for (final Town town : owner.getTowns()) {
            final String popup = "<b>" + ColorCodes.toHtml(owner.getName()) + "</b><br>Town: " + ColorCodes.toHtml(town.getName()) + "<br>" + relationLabel;
            final MarkerOptions options = MarkerOptions.builder()
                .strokeColor(color)
                .strokeWeight(this.config.strokeWeight())
                .fillColor(color)
                .fillOpacity(this.config.fillOpacity())
                .hoverTooltip(ColorCodes.toHtml(owner.getName()) + " - " + ColorCodes.toHtml(town.getName()))
                .clickTooltip(popup)
                .build();

            for (final TownBlock block : town.getTownBlocks()) {
                if (block.getWorld() == null || !block.getWorld().getName().equals(worldName)) {
                    continue;
                }
                final int x = block.getX() * blockSize;
                final int z = block.getZ() * blockSize;
                final Marker rect = Marker.rectangle(Point.of(x, z), Point.of(x + blockSize, z + blockSize));
                rect.markerOptions(options);
                final Key key = Key.of("claim_" + block.getX() + "_" + block.getZ());
                provider.addMarker(key, rect);
                this.counts.merge(relation, 1, Integer::sum);
                current.add(key);
            }
        }
    }

    void unregisterAll() {
        try {
            final var squaremap = SquaremapProvider.get();
            for (final World world : Bukkit.getWorlds()) {
                final Optional<MapWorld> mapWorld = squaremap.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world));
                final Map<Key, SimpleLayerProvider> worldProviders = this.providers.get(world.getName());
                if (mapWorld.isEmpty() || worldProviders == null) {
                    continue;
                }
                for (final Key key : worldProviders.keySet()) {
                    if (mapWorld.get().layerRegistry().hasEntry(key)) {
                        mapWorld.get().layerRegistry().unregister(key);
                    }
                }
            }
        } catch (final Exception ignored) {
            // squaremap may already be disabled
        }
        this.providers.clear();
        this.markerKeys.clear();
    }
}
