(function () {
    "use strict";

    var DESIGN_WIDTH = 1080;
    var DESIGN_HEIGHTS = { TALL: 1920, SQUARE: 1080 };
    var designHeight = DESIGN_HEIGHTS.TALL;
    var ratio = "TALL";
    var theme = "DARK";
    var THEME_CLASSES = { LIGHT: "light", COURT: "court", SUNSET: "sunset" };
    var TEXT_FIELDS = ["brand", "name", "meta", "partner", "time", "opponents", "games", "location"];

    function byId(id) {
        return document.getElementById(id);
    }

    function setText(id, value) {
        var element = byId(id);
        var text = typeof value === "string" ? value.trim() : "";
        element.textContent = text;
        element.hidden = text.length === 0;
    }

    function clean(value) {
        return typeof value === "string" ? value.trim() : "";
    }

    function setEntry(id, entry) {
        var element = byId(id);
        var values = entry && Array.isArray(entry.values) ? entry.values.map(clean).filter(Boolean) : [];
        var container = element.querySelector(".values");
        while (container.firstChild) {
            container.removeChild(container.firstChild);
        }
        element.querySelector(".caption").textContent = entry ? clean(entry.caption) : "";
        values.forEach(function (value) {
            var line = document.createElement("span");
            line.className = "value";
            line.textContent = value;
            container.appendChild(line);
        });
        element.hidden = values.length === 0;
    }

    function setLocation(value) {
        var text = clean(value);
        byId("location-text").textContent = text;
        byId("location").hidden = text.length === 0;
    }

    function cssPixelsPerOutputPixel() {
        var pageScale = window.visualViewport ? window.visualViewport.scale : 1;
        return 1 / (window.devicePixelRatio * pageScale);
    }

    function fitToViewport() {
        var scale = cssPixelsPerOutputPixel();
        byId("card").style.transform = "scale(" + scale + ")";
        return scale;
    }

    function decoded(image) {
        if (image.hidden || !image.getAttribute("src")) {
            return Promise.resolve();
        }
        return image.decode().catch(function () {
            image.hidden = true;
            image.removeAttribute("src");
            byId("card").classList.add("no-photo");
        });
    }

    function decodedBrandMark() {
        var mark = byId("brand-mark");
        if (byId("brand-row").hidden) {
            return Promise.resolve();
        }
        return mark.decode().catch(function () {
            mark.hidden = true;
        });
    }

    function designRect(element, scale) {
        var rect = element.getBoundingClientRect();
        return {
            left: rect.left / scale,
            top: rect.top / scale,
            right: rect.right / scale,
            bottom: rect.bottom / scale
        };
    }

    function diagnostics(scale) {
        var overflowing = [];
        var rects = {};
        var texts = {};
        TEXT_FIELDS.forEach(function (id) {
            var element = byId(id);
            if (element.hidden) {
                return;
            }
            var rect = designRect(element, scale);
            rects[id] = rect;
            var lines = element.querySelectorAll(".value");
            texts[id] = lines.length > 0
                ? Array.prototype.map.call(lines, function (line) { return line.textContent; }).join("\n")
                : element.textContent;
            var outside = rect.left < -1 || rect.top < -1 || rect.right > DESIGN_WIDTH + 1 ||
                rect.bottom > designHeight + 1;
            var widerThanBox = element.scrollWidth > element.clientWidth + 1 &&
                !element.classList.contains("single");
            if (outside || widerThanBox) {
                overflowing.push(id);
            }
        });
        return {
            overflowing: overflowing,
            rects: rects,
            texts: texts,
            elementCount: byId("card").getElementsByTagName("*").length,
            viewportCssWidth: window.innerWidth,
            devicePixelRatio: window.devicePixelRatio,
            pageScale: window.visualViewport ? window.visualViewport.scale : 1,
            scale: scale,
            ratio: ratio,
            theme: theme,
            designHeight: designHeight,
            photoShown: !byId("photo").hidden,
            courtShown: getComputedStyle(byId("court")).display !== "none"
        };
    }

    window.renderCard = function (data, token) {
        var card = byId("card");
        var photo = byId("photo");
        ratio = data.ratio === "SQUARE" ? "SQUARE" : "TALL";
        theme = Object.prototype.hasOwnProperty.call(THEME_CLASSES, data.theme) ? data.theme : "DARK";
        designHeight = DESIGN_HEIGHTS[ratio];
        card.classList.toggle("square", ratio === "SQUARE");
        Object.keys(THEME_CLASSES).forEach(function (key) {
            card.classList.toggle(THEME_CLASSES[key], key === theme);
        });
        setText("brand", data.brand);
        byId("brand-row").hidden = byId("brand").hidden;
        setText("name", data.displayName);
        setText("meta", data.meta);
        setEntry("partner", data.partner);
        setEntry("time", data.time);
        setEntry("opponents", data.opponents);
        setEntry("games", data.games);
        setLocation(data.location);
        byId("top-row").hidden = byId("partner").hidden && byId("time").hidden;
        if (typeof data.photo === "string" && data.photo.indexOf("data:image/") === 0) {
            photo.src = data.photo;
            photo.hidden = false;
            card.classList.remove("no-photo");
        } else {
            photo.removeAttribute("src");
            photo.hidden = true;
            card.classList.add("no-photo");
        }
        var scale = fitToViewport();
        var fonts = document.fonts
            ? document.fonts.load('800 92px "Baloo 2"').then(function () { return document.fonts.ready; }).catch(function () {})
            : Promise.resolve();
        Promise.all([fonts, decoded(photo), decodedBrandMark()]).then(function () {
            scale = fitToViewport();
            document.body.getBoundingClientRect();
            setTimeout(function () {
                window.PicklejotCard.onLayoutReady(token, JSON.stringify(diagnostics(scale)));
            }, 0);
        });
    };
})();
