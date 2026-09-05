/*
 * Precomputes the 3D polygon geometry (cap + side triangulation) for every
 * country in countries-50m.json, using the exact same library and exact
 * same parameters that three-globe's polygon layer uses internally
 * (ConicPolygonGeometry(coords, 0, GLOBE_RADIUS, false, true, true,
 * capCurvatureResolution), GLOBE_RADIUS=100, capCurvatureResolution=5 —
 * three-globe's own default, which globe.js does not override).
 *
 * WHY: measured on 2026-09-05, building this geometry live in the browser
 * takes ~7.5 SECONDS of blocking main-thread work for the 241 countries
 * (1616 individual polygon/multi-polygon parts) in this dataset — and it
 * reruns on every single page load, including every "Play Again". No amount
 * of caching/compression/bundling touches this cost, because it's CPU work,
 * not network transfer. This script moves that cost to build time instead.
 *
 * Output (written into src/main/resources/static/assets/):
 *   - polygon-geometry-manifest.json: hash(coords) -> byte/element offsets
 *   - polygon-geometry.bin: one binary blob containing every polygon's
 *     position (Float32) and index (Uint16) arrays, back to back.
 *
 * The geometry is later reconstructed in the browser via
 * three-globe.bundle.js's patched polygon layer (see
 * build-three-globe-bundle.js), which looks up precomputed geometry by an
 * identical hash of the same coords and falls back to live computation
 * for any miss — so this is purely an optimization, never a correctness
 * requirement.
 *
 * Run with: npm install && npm run build   (see package.json)
 */

global.window = {}; // three-conic-polygon-geometry prefers window.THREE if present, else its own import — stub so requiring it under Node doesn't throw

const fs = require('fs');
const path = require('path');
const topojson = require('topojson-client');
const ConicPolygonGeometry = require('three-conic-polygon-geometry').default;
const { hashCoords } = require('./hash-fn-source');

const GLOBE_RADIUS = 100;
const CAP_CURVATURE_RESOLUTION = 5; // three-globe's polygonCapCurvatureResolution default

const REPO_ROOT = path.resolve(__dirname, '..', '..');
const SRC_TOPOJSON = path.join(REPO_ROOT, 'src/main/resources/static/assets/countries-50m.json');
const OUT_MANIFEST = path.join(REPO_ROOT, 'src/main/resources/static/assets/polygon-geometry-manifest.json');
const OUT_BIN = path.join(REPO_ROOT, 'src/main/resources/static/assets/polygon-geometry.bin');

console.log('Reading', SRC_TOPOJSON);
const topoData = JSON.parse(fs.readFileSync(SRC_TOPOJSON, 'utf8'));
const geo = topojson.feature(topoData, topoData.objects.countries);
console.log(`Converted ${geo.features.length} country features.`);

// Replicate three-globe's own polygons.js splitting logic exactly:
// Polygon -> single entry; MultiPolygon -> one entry per part.
// (See tools/globe-geometry-build/README notes / the three-globe source
// extracted from its sourcemap for the original: src/layers/polygons.js)
const singlePolygons = [];
geo.features.forEach(feature => {
  const geoJson = feature.geometry;
  if (!geoJson) return;
  if (geoJson.type === 'Polygon') {
    singlePolygons.push({ name: feature.properties.name, coords: geoJson.coordinates });
  } else if (geoJson.type === 'MultiPolygon') {
    geoJson.coordinates.forEach(coords => {
      singlePolygons.push({ name: feature.properties.name, coords });
    });
  } else {
    console.warn(`Skipping unsupported geometry type ${geoJson.type} for ${feature.properties.name}`);
  }
});
console.log(`Total single-ring polygons to precompute: ${singlePolygons.length}`);

const manifest = {};
const positionChunks = [];
const indexChunks = [];
let posCursor = 0; // in float units
let idxCursor = 0; // in uint16 units
let collisions = 0;
let skippedTooLarge = 0;

const t0 = process.hrtime.bigint();

for (const { name, coords } of singlePolygons) {
  const key = hashCoords(JSON.stringify(coords));

  if (manifest[key]) {
    // Should be astronomically unlikely (64-bit-ish hash over ~1600 entries).
    // If it ever happens, skip precomputing this one — it will just fall
    // back to live computation at runtime for this single polygon, so
    // correctness is unaffected, only the speed win for that one polygon.
    collisions++;
    console.warn(`Hash collision for ${name} (key ${key}) — skipping precompute for this polygon, it will fall back to live rendering.`);
    continue;
  }

  const geom = new ConicPolygonGeometry(coords, 0, GLOBE_RADIUS, false, true, true, CAP_CURVATURE_RESOLUTION);
  const positions = geom.attributes.position.array; // Float32Array
  const indexArr = geom.index.array; // Uint32Array or Uint16Array depending on vertex count

  let maxIndex = 0;
  for (let i = 0; i < indexArr.length; i++) if (indexArr[i] > maxIndex) maxIndex = indexArr[i];

  if (maxIndex >= 65536) {
    // Never expected for a single country/part, but guard anyway — skip
    // precompute, fall back to live rendering for this one polygon only.
    skippedTooLarge++;
    console.warn(`${name}: max index ${maxIndex} exceeds Uint16 range — skipping precompute, will fall back to live rendering.`);
    continue;
  }

  const idx16 = new Uint16Array(indexArr.length);
  idx16.set(indexArr);

  manifest[key] = {
    posOffset: posCursor,
    posCount: positions.length,
    idxOffset: idxCursor,
    idxCount: idx16.length,
    groups: geom.groups.map(g => [g.start, g.count, g.materialIndex])
  };

  positionChunks.push(Buffer.from(positions.buffer, positions.byteOffset, positions.byteLength));
  indexChunks.push(Buffer.from(idx16.buffer, idx16.byteOffset, idx16.byteLength));
  posCursor += positions.length;
  idxCursor += idx16.length;
}

const t1 = process.hrtime.bigint();
console.log(`Built ${Object.keys(manifest).length} precomputed geometries in ${Number(t1 - t0) / 1e6} ms (this cost now happens once, at build time, instead of on every page load).`);
if (collisions) console.log(`Hash collisions skipped: ${collisions}`);
if (skippedTooLarge) console.log(`Oversized-index polygons skipped: ${skippedTooLarge}`);

const positionsBuf = Buffer.concat(positionChunks);
const indicesBuf = Buffer.concat(indexChunks);

const header = Buffer.alloc(8);
header.writeUInt32LE(posCursor, 0); // total position floats
header.writeUInt32LE(idxCursor, 4); // total index uint16s

const outBuf = Buffer.concat([header, positionsBuf, indicesBuf]);
fs.writeFileSync(OUT_BIN, outBuf);
fs.writeFileSync(OUT_MANIFEST, JSON.stringify(manifest));

console.log(`Wrote ${OUT_BIN} (${(outBuf.length / 1024).toFixed(1)} KB)`);
console.log(`Wrote ${OUT_MANIFEST} (${(fs.statSync(OUT_MANIFEST).size / 1024).toFixed(1)} KB)`);
