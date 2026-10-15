/**
 * Adds a "Diplomacy" box under squaremap's layer control. Hovering it flies out a
 * radio list of nations. Selecting one toggles the matching (hidden) squaremap
 * layer checkbox, so no access to squaremap internals is needed.
 */
(function () {
    let label = "Diplomacy";
    let noneLabel = "None";
    let keyColors = { self: "#2ecc71", ally: "#3498db", enemy: "#e74c3c", neutral: "#888888" };
    const MAX_VISIBLE = 12;
    const ROW_HEIGHT = 20;
    let box = null;
    let list = null;
    let signature = "";
    let scheduled = false;

    const COLORS = {
        0: "#000000", 1: "#0000AA", 2: "#00AA00", 3: "#00AAAA", 4: "#AA0000", 5: "#AA00AA",
        6: "#FFAA00", 7: "#AAAAAA", 8: "#555555", 9: "#5555FF", a: "#55FF55", b: "#55FFFF",
        c: "#FF5555", d: "#FF55FF", e: "#FFFF55", f: "#FFFFFF",
    };

    function isWhite(c) {
        return c.toLowerCase() === "#ffffff";
    }

    // Renders Minecraft colour codes (\u00A7 or &) into el; white becomes black for the light background.
    function renderCodes(el, text) {
        el.textContent = "";
        let st = {};
        let buf = "";
        const flush = () => {
            if (!buf) return;
            const span = document.createElement("span");
            span.textContent = buf;
            if (st.color) span.style.color = isWhite(st.color) ? "#000000" : st.color;
            if (st.bold) span.style.fontWeight = "bold";
            if (st.italic) span.style.fontStyle = "italic";
            const deco = [st.underline ? "underline" : "", st.strike ? "line-through" : ""].filter(Boolean).join(" ");
            if (deco) span.style.textDecoration = deco;
            el.appendChild(span);
            buf = "";
        };
        for (let i = 0; i < text.length; i++) {
            const ch = text[i];
            if ((ch === "\u00A7" || ch === "&") && i + 1 < text.length) {
                const n = text[i + 1].toLowerCase();
                const hex = n === "#" ? text.substring(i + 2, i + 8) : "";
                if (/^[0-9a-f]{6}$/i.test(hex)) {
                    flush();
                    st = { color: "#" + hex };
                    i += 7;
                    continue;
                }
                if (COLORS[n] !== undefined) {
                    flush();
                    st = { color: COLORS[n] };
                    i += 1;
                    continue;
                }
                if ("lonmr".includes(n)) {
                    flush();
                    if (n === "l") st.bold = true;
                    else if (n === "o") st.italic = true;
                    else if (n === "n") st.underline = true;
                    else if (n === "m") st.strike = true;
                    else st = {};
                    i += 1;
                    continue;
                }
            }
            buf += ch;
        }
        flush();
        if (!el.hasChildNodes()) el.textContent = text;
    }

    function overlayRows() {
        const prefix = label + ": ";
        return [...document.querySelectorAll(".leaflet-control-layers-overlays label")]
            .map((row) => ({ row, name: row.textContent.trim() }))
            .filter((r) => r.name.startsWith(prefix))
            .map((r) => ({ row: r.row, name: r.name.substring(prefix.length), input: r.row.querySelector("input") }))
            .filter((r) => r.input);
    }

    function select(name) {
        for (const r of overlayRows()) {
            const want = name !== null && r.name === name;
            if (r.input.checked !== want) r.input.click();
        }
        setTimeout(sync, 0);
    }

    function buildBox() {
        const layers = document.querySelector(".leaflet-control-layers");
        if (!layers || !layers.parentNode) return false;

        box = document.createElement("div");
        box.className = "leaflet-control-layers leaflet-control diplomacy-box";
        const toggle = document.createElement("a");
        toggle.href = "#";
        toggle.title = label;
        toggle.textContent = "\u2694";
        const ref = layers.querySelector(".leaflet-control-layers-toggle");
        const w = ref && ref.offsetWidth ? ref.offsetWidth : 44;
        const h = ref && ref.offsetHeight ? ref.offsetHeight : 44;
        toggle.style.cssText =
            "display:block;width:" + w + "px;height:" + h + "px;line-height:" + h +
            "px;text-align:center;font-size:" + Math.round(h * 0.55) + "px;text-decoration:none;color:#333;";
        toggle.addEventListener("click", (e) => {
            e.preventDefault();
            setOpen(list.style.display === "none");
        });
        list = document.createElement("div");
        list.style.cssText = "display:none;padding:6px 10px 6px 6px;color:#333;";
        box.appendChild(toggle);
        box.appendChild(list);
        box.addEventListener("mouseenter", () => setOpen(true));
        box.addEventListener("mouseleave", () => setOpen(false));
        layers.parentNode.insertBefore(box, layers.nextSibling);
        return true;
    }

    function setOpen(open) {
        list.style.display = open ? "block" : "none";
        box.firstChild.style.display = open ? "none" : "block";
    }

    function render(rows) {
        list.textContent = "";
        const title = document.createElement("div");
        title.textContent = label;
        title.style.cssText = "font-weight:bold;margin-bottom:4px;";
        list.appendChild(title);
        const entries = [{ name: null, text: noneLabel }, ...rows.map((r) => ({ name: r.name, text: r.name }))];
        const scroller = document.createElement("div");
        if (rows.length > MAX_VISIBLE) {
            // 12 nations + the "None" row are visible before scrolling
            scroller.style.cssText = "overflow-y:auto;overflow-x:hidden;padding-right:6px;max-height:" + (MAX_VISIBLE + 1) * ROW_HEIGHT + "px;";
        }
        list.appendChild(scroller);
        for (const e of entries) {
            const item = document.createElement("label");
            item.style.cssText = "display:block;cursor:pointer;white-space:nowrap;line-height:" + ROW_HEIGHT + "px;height:" + ROW_HEIGHT + "px;";
            const radio = document.createElement("input");
            radio.type = "radio";
            radio.name = "diplomacy-nation";
            radio.className = "leaflet-control-layers-selector";
            radio.dataset.nation = e.name === null ? "" : e.name;
            radio.addEventListener("change", () => select(e.name));
            const span = document.createElement("span");
            span.appendChild(document.createTextNode(" "));
            const name = document.createElement("span");
            if (e.name === null) name.textContent = e.text;
            else renderCodes(name, e.text);
            span.appendChild(name);
            item.appendChild(radio);
            item.appendChild(span);
            scroller.appendChild(item);
        }
        const key = document.createElement("div");
        key.style.cssText = "margin-top:6px;padding-top:4px;border-top:1px solid #ccc;font-size:11px;";
        for (const [text, color] of [
            ["Selected", keyColors.self],
            ["Ally", keyColors.ally],
            ["Enemy", keyColors.enemy],
            ["Neutral", keyColors.neutral],
        ]) {
            const row = document.createElement("div");
            row.style.cssText = "white-space:nowrap;line-height:15px;";
            const swatch = document.createElement("span");
            swatch.style.cssText =
                "display:inline-block;width:10px;height:10px;margin-right:5px;vertical-align:middle;border:1px solid rgba(0,0,0,.4);background:" + color + ";";
            row.appendChild(swatch);
            row.appendChild(document.createTextNode(text));
            key.appendChild(row);
        }
        list.appendChild(key);
    }

    function sync() {
        const rows = overlayRows();
        for (const r of rows) {
            if (r.row.style.display !== "none") r.row.style.display = "none";
        }
        if (rows.length === 0) {
            if (box) box.style.display = "none";
            return;
        }
        if (!box && !buildBox()) return;
        box.style.display = "";

        const sig = rows.map((r) => r.name).join("\u0001");
        if (sig !== signature) {
            signature = sig;
            render(rows);
        }
        const checked = rows.find((r) => r.input.checked);
        const current = checked ? checked.name : "";
        for (const radio of list.querySelectorAll("input[type=radio]")) {
            const on = radio.dataset.nation === current;
            if (radio.checked !== on) radio.checked = on;
        }
    }

    function schedule() {
        if (scheduled) return;
        scheduled = true;
        setTimeout(() => {
            scheduled = false;
            sync();
        }, 0);
    }

    async function start() {
        try {
            const res = await fetch("diplomacy-settings.json", { cache: "no-store" });
            if (res.ok) {
                const s = await res.json();
                if (s && s.label) label = s.label;
                if (s && s.noneLabel) noneLabel = s.noneLabel;
                if (s && s.colors) keyColors = Object.assign(keyColors, s.colors);
            }
        } catch (e) {
            // keep defaults
        }
        new MutationObserver((records) => {
            if (box && records.every((r) => box.contains(r.target))) return;
            schedule();
        }).observe(document.body, { childList: true, subtree: true });
        schedule();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", start);
    } else {
        start();
    }
})();
