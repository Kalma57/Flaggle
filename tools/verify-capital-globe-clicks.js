#!/usr/bin/env node
/**
 * Verifies that every country the Capital Locations globe game can pick as a target is
 * actually clickable-and-confirmable, by running the REAL production click-detection code
 * from src/main/resources/static/js/globe.js (not a reimplementation) against every eligible
 * target's own stored (lat, lon), plus a small jitter around it for coastline-precision
 * robustness.
 *
 * Run from the repo root: node tools/verify-capital-globe-clicks.js
 *
 * Requires python3 on PATH (used only to dump the Countries table to JSON).
 */
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const vm = require('vm');

const ROOT = path.resolve(__dirname, '..');
const DB_PATH = path.join(ROOT, 'src/main/resources/static/DB/Flaggle.db');
const GLOBE_JS_PATH = path.join(ROOT, 'src/main/resources/static/js/globe.js');
const TOPOJSON_PATH = path.join(ROOT, 'src/main/resources/static/assets/countries-50m.json');
const TOPOJSON_CLIENT_PATH = path.join(ROOT, 'src/main/resources/static/js/topojson-client.min.js');

// ---- 1. Dump the Countries table via python3 (sqlite3 is stdlib, no extra deps needed) ----
const pyScript = `
import sqlite3, json
con = sqlite3.connect(${JSON.stringify(DB_PATH)})
cur = con.cursor()
cur.execute('SELECT CountryName, Capital, Latitude, Longitude FROM Countries')
rows = [{"name": n, "capital": c, "lat": lat, "lon": lon} for n, c, lat, lon in cur.fetchall()]
print(json.dumps(rows))
`;
const pyOut = execFileSync('python3', ['-c', pyScript], { encoding: 'utf-8' });
const dbRows = JSON.parse(pyOut);

// ---- 2. Load the REAL globe.js production code into a sandbox, exposing its internals ----
let globeSrc = fs.readFileSync(GLOBE_JS_PATH, 'utf-8');
globeSrc = globeSrc
    .replace(/^import .*\r?\n/gm, '')     // strip ES module imports (THREE, OrbitControls, etc.)
    .replace(/^export default function/gm, 'function')
    .replace(/^export async function/gm, 'async function')
    .replace(/^export function/gm, 'function');
globeSrc += `
__hooks.findCountryAtLatLon = findCountryAtLatLon;
__hooks.mapNameToDbName = mapNameToDbName;
__hooks.stripDetachedSubPolygons = stripDetachedSubPolygons;
__hooks.unwrapAntimeridianRings = unwrapAntimeridianRings;
__hooks.setCountriesData = (d) => { countriesData = d; };
`;

const topojson = new Function('module', 'exports', fs.readFileSync(TOPOJSON_CLIENT_PATH, 'utf-8') + '\nmodule.exports = exports;');
const topojsonModule = { exports: {} };
topojson(topojsonModule, topojsonModule.exports);
const topojsonLib = topojsonModule.exports;

const __hooks = {};
const sandbox = {
    THREE: { Vector2: class {}, Raycaster: class { setFromCamera() {} intersectObject() { return []; } } },
    OrbitControls: class {},
    ThreeGlobe: class {},
    __setPrecomputedPolygonGeometry: () => {},
    topojson: topojsonLib,
    __hooks,
    console,
};
vm.createContext(sandbox);
vm.runInContext(globeSrc, sandbox, { filename: 'globe.js (sandboxed)' });

const topoResponse = JSON.parse(fs.readFileSync(TOPOJSON_PATH, 'utf-8'));
const countriesData = topojsonLib.feature(topoResponse, topoResponse.objects.countries);
__hooks.stripDetachedSubPolygons(countriesData);
__hooks.unwrapAntimeridianRings(countriesData);
__hooks.setCountriesData(countriesData);

// ---- 3. Same eligibility rule as CapitalGlobeTargetPicker.java ----
const TOO_SMALL_FOR_GLOBE = new Set(['Vatican City', 'Monaco']);
const NO_CLICKABLE_POLYGON = new Set([
    'Cocos (Keeling) Islands', 'Christmas Island', 'French Guiana', 'Gibraltar',
    'Guadeloupe', 'Martinique', 'Réunion', 'Svalbard & Jan Mayen', 'Tuvalu', 'Mayotte'
]);

const eligible = dbRows.filter(r => {
    if (!r.capital || !r.capital.trim()) return false;
    const lat = r.lat || 0, lon = r.lon || 0;
    if (lat === 0 && lon === 0) return false;
    if (TOO_SMALL_FOR_GLOBE.has(r.name) || NO_CLICKABLE_POLYGON.has(r.name)) return false;
    return true;
});

// ---- 4. For each eligible target, click its own stored point AND 4 nearby jittered points ----
const JITTER = 0.3; // degrees - approximates a slightly-off click near the true point
const failures = [];
for (const country of eligible) {
    const offsets = [[0, 0], [JITTER, 0], [-JITTER, 0], [0, JITTER], [0, -JITTER]];
    let anyHit = false;
    const misses = [];
    for (const [dLat, dLon] of offsets) {
        const feature = __hooks.findCountryAtLatLon(country.lat + dLat, country.lon + dLon);
        const resolvedDbName = feature ? __hooks.mapNameToDbName(feature.properties.name) : null;
        if (resolvedDbName === country.name) { anyHit = true; }
        else { misses.push({ dLat, dLon, resolvedTo: resolvedDbName }); }
    }
    if (!anyHit) {
        failures.push({ name: country.name, lat: country.lat, lon: country.lon, misses });
    }
}

console.log(`Checked ${eligible.length} eligible target countries.`);
if (failures.length === 0) {
    console.log('ALL PASS - every eligible target resolves correctly to itself when clicked at (or very near) its stored coordinate.');
} else {
    console.log(`${failures.length} FAILURES:`);
    for (const f of failures) {
        console.log(`  ${f.name} (${f.lat}, ${f.lon}):`, JSON.stringify(f.misses));
    }
    process.exitCode = 1;
}
