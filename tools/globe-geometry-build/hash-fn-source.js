// This file holds ONE canonical implementation of the coordinate-hashing
// function used to key precomputed polygon geometry.
//
// It is consumed in two places that must produce byte-identical results:
//   1. precompute-geometry.js (Node) — requires this file directly.
//   2. build-three-globe-bundle.js — reads this file's *source text* and
//      injects it verbatim into the patched three-globe bundle, so the
//      browser computes the exact same hash for the exact same coords.
//
// Keeping a single source of truth (instead of hand-duplicating the function
// in two places) removes any risk of the two copies drifting apart, which
// would silently break every lookup (falling back to live computation —
// safe, but defeats the whole optimization).
//
// IMPORTANT: keep this function pure ES (no Node-only APIs like Buffer),
// since it also has to run unmodified in the browser.

function hashCoords(jsonStr) {
  let h1 = 0x811c9dc5;
  let h2 = 0xc271a137;
  for (let i = 0; i < jsonStr.length; i++) {
    const c = jsonStr.charCodeAt(i);
    h1 = Math.imul(h1 ^ c, 0x01000193);
    h2 = Math.imul(h2 ^ c, 0x85ebca6b);
  }
  return (h1 >>> 0).toString(36) + '_' + (h2 >>> 0).toString(36);
}

module.exports = { hashCoords };
