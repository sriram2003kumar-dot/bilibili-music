(() => {
  "use strict";

  const audio = document.getElementById("audio");
  const bar = document.getElementById("bar");
  const fill = document.getElementById("fill");
  const current = document.getElementById("current");
  const duration = document.getElementById("duration");

  const cover = document.getElementById("cover");
  const title = document.getElementById("title");
  const artist = document.getElementById("artist");

  const songs = document.getElementById("songs");

  const islandCover = document.getElementById("islandCover");
  const islandTitle = document.getElementById("islandTitle");
  const islandArtist = document.getElementById("islandArtist");
  const islandPlay = document.getElementById("islandPlay");

  if (!audio || !bar || !songs) return;

  const META_KEY = "BilibiliMusicMetaV5";
  const FAV_KEY = "BilibiliMusicFavoritesV1";

  let meta = {};
  let favorites = {};

  try {
    meta = JSON.parse(localStorage.getItem(META_KEY) || "{}");
  } catch (_) {}

  try {
    favorites = JSON.parse(localStorage.getItem(FAV_KEY) || "{}");
  } catch (_) {}

  function saveMeta() {
    try {
      localStorage.setItem(META_KEY, JSON.stringify(meta));
    } catch (_) {}
  }

  function saveFavorites() {
    try {
      localStorage.setItem(FAV_KEY, JSON.stringify(favorites));
    } catch (_) {}
  }

  function time(v) {
    if (!Number.isFinite(v) || v < 0) return "0:00";

    const m = Math.floor(v / 60);
    const s = Math.floor(v % 60);

    return m + ":" + String(s).padStart(2, "0");
  }

  function clean(v) {
    return String(v || "")
      .replace(/[<&>]/g, "")
      .trim();
  }

  function placeholder(name, artistName) {
    const n = clean(name || "BILIBILI MUSIC").slice(0, 22);
    const a = clean(artistName || "LOCAL MUSIC").slice(0, 18);

    const svg = `
      <svg xmlns="http://www.w3.org/2000/svg"
           width="700"
           height="700">
        <rect width="700"
              height="700"
              fill="#101010"/>

        <circle
          cx="560"
          cy="130"
          r="250"
          fill="#c2185b"/>

        <circle
          cx="100"
          cy="620"
          r="250"
          fill="#292929"/>

        <text
          x="35"
          y="535"
          fill="white"
          font-size="38"
          font-family="monospace">
          ${n}
        </text>

        <text
          x="35"
          y="590"
          fill="#aaa"
          font-size="20"
          font-family="monospace">
          ${a}
        </text>
      </svg>
    `;

    return "data:image/svg+xml;charset=utf-8," +
      encodeURIComponent(svg);
  }

  function getSongMeta(row) {
    const name =
      row.querySelector(".songName")?.textContent?.trim() || "";

    const id =
      row.dataset.songId ||
      row.getAttribute("data-id") ||
      "";

    if (id && meta[id]) {
      return meta[id];
    }

    const found = Object.values(meta).find(
      item =>
        String(item.name || "").toLowerCase() ===
        name.toLowerCase()
    );

    return found || {
      id: id || name,
      name,
      artist:
        row.querySelector(".songSize")?.textContent ||
        "LOCAL MUSIC"
    };
  }

  function artwork(song) {
    if (!song) {
      return placeholder("BILIBILI MUSIC", "LOCAL MUSIC");
    }

    if (song.artUrl) {
      return song.artUrl;
    }

    if (song.albumArt) {
      return song.albumArt;
    }

    if (song.albumId != null) {
      return (
        "https://appassets.androidplatform.net/albumart/" +
        encodeURIComponent(String(song.albumId))
      );
    }

    return placeholder(song.name, song.artist);
  }

  function setImage(img, song) {
    if (!img) return;

    const src = artwork(song);

    img.src = src;

    img.onerror = () => {
      img.onerror = null;
      img.src = placeholder(song?.name, song?.artist);
    };
  }

  function updateNowPlaying(song) {
    if (!song) return;

    if (title && song.name) {
      title.textContent = song.name;
    }

    if (artist) {
      artist.textContent =
        song.artist || "LOCAL MUSIC";
    }

    setImage(cover, song);
    setImage(islandCover, song);

    if (islandTitle) {
      islandTitle.textContent =
        song.name || "NO SONG";
    }

    if (islandArtist) {
      islandArtist.textContent =
        song.artist || "LOCAL MUSIC";
    }
  }

  function getPlayingRow() {
    return songs.querySelector(".song.playing");
  }

  function decorateCards() {
    songs.querySelectorAll(".song").forEach(row => {
      if (row.classList.contains("bilibiliCard")) {
        updateFavoriteButton(row);
        return;
      }

      const info = row.querySelector(".songInfo");

      if (!info) return;

      const song = getSongMeta(row);

      row.classList.add("bilibiliCard");

      row.dataset.songId =
        String(song.id || song.name || "");

      const img = document.createElement("img");

      img.className = "bilibiliAlbumArt";

      img.alt = "";

      setImage(img, song);

      row.insertBefore(img, info);

      const fav = document.createElement("button");

      fav.className = "bilibiliFavorite";

      fav.type = "button";

      fav.textContent = "♡";

      fav.addEventListener("click", e => {
        e.stopPropagation();

        const id =
          row.dataset.songId ||
          song.name;

        favorites[id] = !favorites[id];

        saveFavorites();

        updateFavoriteButton(row);
      });

      row.appendChild(fav);

      updateFavoriteButton(row);
    });

    const playing = getPlayingRow();

    if (playing) {
      updateNowPlaying(getSongMeta(playing));
    }
  }

  function updateFavoriteButton(row) {
    const button =
      row.querySelector(".bilibiliFavorite");

    if (!button) return;

    const id =
      row.dataset.songId || "";

    button.textContent =
      favorites[id] ? "♥" : "♡";

    button.classList.toggle(
      "favoriteActive",
      !!favorites[id]
    );
  }

  const style = document.createElement("style");

  style.textContent = `
    .song {
      position: relative;
      min-height: 76px;
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .bilibiliAlbumArt {
      width: 54px;
      height: 54px;
      min-width: 54px;
      border-radius: 12px;
      object-fit: cover;
      background: #111;
      border: 1px solid #292929;
      display: block;
    }

    .bilibiliFavorite {
      width: 34px;
      height: 34px;
      flex: none;
      border: 0;
      background: transparent;
      color: #555;
      font-size: 20px;
      padding: 0;
    }

    .bilibiliFavorite.favoriteActive {
      color: #c2185b;
    }

    .bar {
      position: relative;
      min-height: 22px;
      padding: 7px 0;
      touch-action: none;
      cursor: pointer;
    }

    .bar::before {
      content: "";
      position: absolute;
      left: 0;
      right: 0;
      top: 50%;
      height: 8px;
      transform: translateY(-50%);
    }

    .fill {
      pointer-events: none;
    }

    .song.playing .bilibiliAlbumArt {
      box-shadow: 0 0 0 2px rgba(194,24,91,.5);
    }

    .song.playing .songName {
      color: #fff;
    }

    .song.playing .songSize {
      color: #c2185b;
    }

    .bilibiliSeekPreview {
      position: fixed;
      z-index: 10000;
      pointer-events: none;
      background: #111;
      border: 1px solid #333;
      border-radius: 8px;
      padding: 5px 8px;
      font: 10px "Courier New", monospace;
      color: #fff;
      transform: translate(-50%, -130%);
      display: none;
    }
  `;

  document.head.appendChild(style);

  const preview = document.createElement("div");

  preview.className = "bilibiliSeekPreview";

  document.body.appendChild(preview);

  function seek(clientX, showPreview = false) {
    if (
      !Number.isFinite(audio.duration) ||
      audio.duration <= 0
    ) {
      return;
    }

    const rect =
      bar.getBoundingClientRect();

    let ratio =
      (clientX - rect.left) /
      rect.width;

    ratio = Math.max(
      0,
      Math.min(1, ratio)
    );

    const newTime =
      ratio * audio.duration;

    audio.currentTime = newTime;

    if (showPreview) {
      preview.textContent = time(newTime);

      preview.style.left =
        clientX + "px";

      preview.style.top =
        rect.top + "px";

      preview.style.display =
        "block";
    }
  }

  let dragging = false;

  bar.addEventListener(
    "pointerdown",
    e => {
      dragging = true;

      try {
        bar.setPointerCapture(e.pointerId);
      } catch (_) {}

      seek(e.clientX, true);

      e.preventDefault();
    },
    { passive: false }
  );

  bar.addEventListener(
    "pointermove",
    e => {
      if (!dragging) return;

      seek(e.clientX, true);

      e.preventDefault();
    },
    { passive: false }
  );

  bar.addEventListener(
    "pointerup",
    e => {
      if (!dragging) return;

      seek(e.clientX, false);

      dragging = false;

      preview.style.display = "none";
    }
  );

  bar.addEventListener(
    "pointercancel",
    () => {
      dragging = false;
      preview.style.display = "none";
    }
  );

  bar.addEventListener(
    "click",
    e => {
      if (!dragging) {
        seek(e.clientX);
      }
    }
  );

  let lastTap = 0;

  bar.addEventListener(
    "touchend",
    e => {
      const now = Date.now();

      if (now - lastTap < 320) {
        e.preventDefault();

        if (
          Number.isFinite(audio.duration) &&
          audio.duration > 0
        ) {
          audio.currentTime =
            Math.max(
              0,
              audio.currentTime - 10
            );
        }
      }

      lastTap = now;
    },
    { passive: false }
  );

  audio.addEventListener(
    "timeupdate",
    () => {
      if (
        !Number.isFinite(audio.duration) ||
        audio.duration <= 0
      ) {
        return;
      }

      const percent =
        audio.currentTime /
        audio.duration *
        100;

      if (fill) {
        fill.style.width =
          percent + "%";
      }

      if (current) {
        current.textContent =
          time(audio.currentTime);
      }

      if (duration) {
        duration.textContent =
          time(audio.duration);
      }
    }
  );

  audio.addEventListener(
    "loadedmetadata",
    () => {
      if (duration) {
        duration.textContent =
          time(audio.duration);
      }
    }
  );

  audio.addEventListener(
    "play",
    () => {
      if (islandPlay) {
        islandPlay.textContent = "Ⅱ";
      }
    }
  );

  audio.addEventListener(
    "pause",
    () => {
      if (islandPlay) {
        islandPlay.textContent = "▶";
      }
    }
  );

  if (islandPlay) {
    islandPlay.addEventListener(
      "click",
      e => {
        e.stopPropagation();

        if (audio.paused) {
          audio.play().catch(() => {});
        } else {
          audio.pause();
        }
      }
    );
  }

  const oldScan =
    window.onNativeMusicScan;

  window.onNativeMusicScan =
    function(items) {
      (items || []).forEach(item => {
        const key =
          String(item.id);

        meta[key] = item;
      });

      saveMeta();

      if (typeof oldScan === "function") {
        try {
          oldScan(items);
        } catch (_) {}
      }

      setTimeout(
        decorateCards,
        100
      );

      setTimeout(
        decorateCards,
        700
      );
    };

  songs.addEventListener(
    "click",
    () => {
      setTimeout(
        decorateCards,
        100
      );
    },
    true
  );

  new MutationObserver(
    decorateCards
  ).observe(
    songs,
    {
      childList: true,
      subtree: true
    }
  );

  setInterval(
    decorateCards,
    1200
  );

  decorateCards();
})();
