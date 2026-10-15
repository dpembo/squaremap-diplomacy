package xyz.jpenilla.squaremapdiplomacy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class SquaremapDiplomacyPlugin extends JavaPlugin {
    private BukkitTask task;
    private NationLayers layers;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        final DiplomacyConfig config = DiplomacyConfig.from(this.getConfig());
        if (!config.enabled()) {
            this.getLogger().info("Disabled in config.yml");
            return;
        }
        this.installWebAssets(config);
        this.layers = new NationLayers(this, config);
        final long period = Math.max(20L, config.updateIntervalSeconds() * 20L);
        this.task = this.getServer().getScheduler().runTaskTimer(this, this.layers::refresh, 40L, period);
    }

    @Override
    public void onDisable() {
        if (this.task != null) {
            this.task.cancel();
        }
        if (this.layers != null) {
            this.layers.unregisterAll();
        }
    }

    private void installWebAssets(final DiplomacyConfig config) {
        final Path web = config.webDirectory();
        try {
            final Path script = web.resolve(config.scriptFile());
            Files.createDirectories(script.getParent());
            try (InputStream in = this.getResource("web/Diplomacy.js")) {
                if (in == null) {
                    throw new IOException("Missing jar resource web/Diplomacy.js");
                }
                Files.copy(in, script, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(
                web.resolve("diplomacy-settings.json"),
                "{\"label\":" + jsonString(config.layerLabel()) + ",\"noneLabel\":" + jsonString(config.noneLabel())
                    + ",\"colors\":{\"self\":" + jsonString(hex(config.self()))
                    + ",\"ally\":" + jsonString(hex(config.ally()))
                    + ",\"enemy\":" + jsonString(hex(config.enemy()))
                    + ",\"neutral\":" + jsonString(hex(config.neutral())) + "}}\n"
            );
            this.injectScriptTag(web.resolve("index.html"), config.scriptFile());
        } catch (final IOException ex) {
            this.getLogger().warning("Could not install web assets into " + web + ": " + ex.getMessage());
        }
    }

    private static String hex(final java.awt.Color c) {
        return String.format("#%06x", c.getRGB() & 0xFFFFFF);
    }

    private static String jsonString(final String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void injectScriptTag(final Path index, final String script) throws IOException {
        if (!Files.isRegularFile(index)) {
            this.getLogger().warning("index.html not found at " + index + "; add <script src=\"" + script + "\" defer></script> manually.");
            return;
        }
        final String html = Files.readString(index);
        if (html.contains(script)) {
            return;
        }
        final int head = html.lastIndexOf("</head>");
        if (head < 0) {
            this.getLogger().warning("No </head> in " + index + "; add the Diplomacy script tag manually.");
            return;
        }
        Files.writeString(index, html.substring(0, head) + "    <script src=\"" + script + "\" defer></script>\n" + html.substring(head));
    }
}
