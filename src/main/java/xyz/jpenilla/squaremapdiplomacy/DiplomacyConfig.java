package xyz.jpenilla.squaremapdiplomacy;

import java.awt.Color;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.bukkit.configuration.file.FileConfiguration;

public record DiplomacyConfig(
    boolean enabled,
    Path webDirectory,
    String scriptFile,
    String noneLabel,
    int updateIntervalSeconds,
    String layerLabel,
    int layerPriority,
    int layerZIndex,
    Color self,
    Color ally,
    Color enemy,
    Color neutral,
    double fillOpacity,
    int strokeWeight
) {
    static DiplomacyConfig from(final FileConfiguration cfg) {
        return new DiplomacyConfig(
            cfg.getBoolean("enabled", true),
            cfg.getString("squaremap-web-directory", "").isBlank() ? null : Paths.get(cfg.getString("squaremap-web-directory")),
            cfg.getString("script-file", "js/addons/Diplomacy.js"),
            cfg.getString("none-label", "None"),
            cfg.getInt("update-interval-seconds", 30),
            cfg.getString("layer-label", "Diplomacy"),
            cfg.getInt("layer-priority", 50),
            cfg.getInt("layer-z-index", 50),
            color(cfg.getString("colors.self"), "#2ecc71"),
            color(cfg.getString("colors.ally"), "#3498db"),
            color(cfg.getString("colors.enemy"), "#e74c3c"),
            color(cfg.getString("colors.neutral"), "#888888"),
            cfg.getDouble("fill-opacity", 0.45),
            cfg.getInt("stroke-weight", 1)
        );
    }

    private static Color color(final String value, final String fallback) {
        try {
            return Color.decode(value == null ? fallback : value);
        } catch (final NumberFormatException ex) {
            return Color.decode(fallback);
        }
    }
}
