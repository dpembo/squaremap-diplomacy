package xyz.jpenilla.squaremapdiplomacy;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import java.util.Collection;
import java.util.stream.Collectors;
import me.silverwolfg11.maptowny.MapTownyPlugin;
import me.silverwolfg11.maptowny.events.MapReloadEvent;
import me.silverwolfg11.maptowny.managers.LayerManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Soft-depends on MapTowny: registers {@code %allies%} and {@code %enemies%}
 * tooltip placeholders so town click/hover popups can list the nation relations.
 *
 * <p>Add to {@code plugins/MapTowny/click_tooltip.html} (and optionally
 * {@code hover_tooltip.html}), for example:</p>
 * <pre>{@code
 * Allies: <span style="font-weight:bold;">%allies%</span>
 * <br/>
 * Enemies: <span style="font-weight:bold;">%enemies%</span>
 * }</pre>
 *
 * <p>Replacements are cleared on {@code /maptowny reload}; this hook
 * re-registers them via {@link MapReloadEvent}.</p>
 */
final class MapTownyHook implements Listener {
    private static final String ALLIES_KEY = "%allies%";
    private static final String ENEMIES_KEY = "%enemies%";
    private static final String NONE = "None";

    private final SquaremapDiplomacyPlugin plugin;

    MapTownyHook(final SquaremapDiplomacyPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @return true if MapTowny was present and placeholders were registered
     */
    boolean tryRegister() {
        final Plugin raw = Bukkit.getPluginManager().getPlugin("MapTowny");
        if (raw == null || !raw.isEnabled()) {
            this.plugin.getLogger().info("MapTowny not found; skipping %allies%/%enemies% tooltip placeholders.");
            return false;
        }
        if (!(raw instanceof MapTownyPlugin mapTowny)) {
            this.plugin.getLogger().warning("MapTowny is present but does not implement MapTownyPlugin; skipping placeholders.");
            return false;
        }
        final LayerManager layerManager = mapTowny.getLayerManager();
        if (layerManager == null) {
            this.plugin.getLogger().warning("MapTowny LayerManager is null; skipping placeholders.");
            return false;
        }

        this.register(layerManager);
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        this.plugin.getLogger().info("Registered MapTowny tooltip placeholders: %allies%, %enemies%");
        return true;
    }

    @EventHandler
    public void onMapTownyReload(final MapReloadEvent event) {
        final Plugin raw = Bukkit.getPluginManager().getPlugin("MapTowny");
        if (!(raw instanceof MapTownyPlugin mapTowny)) {
            return;
        }
        final LayerManager layerManager = mapTowny.getLayerManager();
        if (layerManager == null) {
            return;
        }
        this.register(layerManager);
        this.plugin.getLogger().info("Re-registered MapTowny tooltip placeholders after reload.");
    }

    private void register(final LayerManager layerManager) {
        layerManager.registerReplacement(ALLIES_KEY, MapTownyHook::alliesOf);
        layerManager.registerReplacement(ENEMIES_KEY, MapTownyHook::enemiesOf);
    }

    private static String alliesOf(final Town town) {
        final Nation nation = TownyAPI.getInstance().getTownNationOrNull(town);
        if (nation == null) {
            return NONE;
        }
        return joinNames(nation.getAllies());
    }

    private static String enemiesOf(final Town town) {
        final Nation nation = TownyAPI.getInstance().getTownNationOrNull(town);
        if (nation == null) {
            return NONE;
        }
        return joinNames(nation.getEnemies());
    }

    private static String joinNames(final Collection<Nation> nations) {
        if (nations == null || nations.isEmpty()) {
            return NONE;
        }
        return nations.stream()
            .map(Nation::getName)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(Collectors.joining(", "));
    }
}