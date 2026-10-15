# squaremap-diplomacy

Paper addon that shows Towny nation diplomacy on [squaremap](https://github.com/jpenilla/squaremap) using only squaremap's Java API. No squaremap patches and no browser JavaScript.

## How it works

For every Towny nation the plugin registers one squaremap layer per world, named `Diplomacy: <Nation>`. Tick a nation's layer in the map's layer selector to see:

- the nation's own claims (green)
- claims of its allies (blue)
- claims of nations it is at war with (red)

Claims are drawn per Towny town block, with a popup naming the nation, the town and the relation. Layers are hidden by default and refreshed every `update-interval-seconds`.

## Build

Requires JDK 21+ and Maven 3.9+.

```bash
mvn -B package
```

## Install

Drop `target/squaremap-diplomacy-*.jar` into `plugins/` (Towny and squaremap required) and restart. Adjust `plugins/squaremap-diplomacy/config.yml` if you want different colours or label.
