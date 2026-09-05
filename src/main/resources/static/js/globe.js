import * as THREE from 'three';
import { OrbitControls } from '/controls/OrbitControls.js';
import ThreeGlobe from 'three-globe';
import { __setPrecomputedPolygonGeometry } from 'three-conic-polygon-geometry';

// Country name mapping - critical for matching between DB and map
const countryNameMapping = {
    // Cases of "&" vs "and" and abbreviations
    "Antigua & Barbuda": "Antigua and Barbuda",
    "Trinidad & Tobago": "Trinidad and Tobago",
    "Bosnia & Herzegovina": "Bosnia and Herz.",
    "São Tomé & Príncipe": "São Tomé and Principe",
    "St Vincent & the Grenadines": "St. Vin. and Gren.",
    "St Kitts & Nevis": "Saint Kitts and Nevis",
    "St Lucia": "Saint Lucia",
    "St Martin": "St. Martin",
    "St Barthélemy": "St-Barthélemy",
    "St Helena": "Saint Helena",
    "St Pierre & Miquelon": "St. Pierre and Miquelon",
    "Turks & Caicos Islands": "Turks and Caicos Is.",
    "Wallis & Futuna": "Wallis and Futuna",
    "Svalbard & Jan Mayen": "Svalbard",
    "South Georgia & South Sandwich Islands": "S. Geo. and S. Sandw. Is.",
    "Heard & McDonald Islands": "Heard I. and McDonald Is.",

    // United States and United Kingdom
    "United States": "United States of America",
    "USA": "United States of America",
    "UK": "United Kingdom",

    // Republic name changes (TopoJSON uses abbreviations)
    "Central African Republic": "Central African Rep.",
    "Dominican Republic": "Dominican Rep.",
    "Equatorial Guinea": "Eq. Guinea",

    // Congo
    "Congo - Kinshasa": "Dem. Rep. Congo",
    "Congo - Brazzaville": "Congo",

    // Political and regional adjustments
    "North Macedonia": "Macedonia",
    "Myanmar (Burma)": "Myanmar",
    "Western Sahara": "W. Sahara",
    "South Sudan": "S. Sudan",
    "Eswatini": "eSwatini",
    "Cape Verde": "Cabo Verde",
    "Falkland Islands": "Falkland Is.",
    "Solomon Islands": "Solomon Is.",

    // Apostrophe fix (your DB uses a curly apostrophe ’, the map expects a straight one ')
    "Côte d’Ivoire": "Côte d'Ivoire",

    // Chinese territories (matched Macau to your definition in tinyCountriesExtras)
    "Macao SAR China": "Macau",
    "Hong Kong SAR China": "Hong Kong"
};

// Coordinates for micro-states that are too small to appear as their own
// polygon in the world-atlas dataset. These are rendered as point markers
// instead, but only for names actually missing from the loaded map data
// (checked at runtime in initGlobe) — everything else is a real vector
// polygon now, so it stays crisp and visible at any zoom level.
const tinyCountriesExtras = {
    "Trinidad and Tobago": { lat: 10.69, lon: -61.22 },
    "St. Vin. and Gren.": { lat: 12.98, lon: -61.28 },
    "Grenada": { lat: 12.11, lon: -61.67 },
    "Saint Lucia": { lat: 13.90, lon: -60.97 },
    "Antigua and Barbuda": { lat: 17.06, lon: -61.79 },
    "Barbados": { lat: 13.19, lon: -59.54 },
    "Luxembourg": { lat: 49.81, lon: 6.12 },
    "Monaco": { lat: 43.75, lon: 7.41 },
    "Saint Kitts and Nevis": { lat: 17.35, lon: -62.78 },
    "Malta": { lat: 35.93, lon: 14.37 },
    "Vatican City": { lat: 41.90, lon: 12.45 },
    "Andorra": { lat: 42.54, lon: 1.60 },
    "Anguilla": { lat: 18.22, lon: -63.06 },
    "Aruba": { lat: 12.52, lon: -69.96 },
    "Bermuda": { lat: 32.32, lon: -64.75 },
    "British Virgin Islands": { lat: 18.42, lon: -64.63 },
    "Cayman Islands": { lat: 19.51, lon: -80.56 },
    "Gibraltar": { lat: 36.13, lon: -5.34 },
    "Guernsey": { lat: 49.46, lon: -2.58 },
    "Isle of Man": { lat: 54.23, lon: -4.54 },
    "Jersey": { lat: 49.21, lon: -2.13 },
    "Liechtenstein": { lat: 47.16, lon: 9.55 },
    "Macau": { lat: 22.19, lon: 113.54 },
    "Nauru": { lat: -0.52, lon: 166.93 },
    "San Marino": { lat: 43.94, lon: 12.45 },
    "Singapore": { lat: 1.35, lon: 103.81 },
    "Tuvalu": { lat: -7.10, lon: 177.64 }
};

const SKY_COLOR = 0xffffff;

let scene, camera, renderer, controls, world;
let countriesData = null;
let guessedCountriesColors = {};
let missingPolygonNames = new Set();
let guessedPoints = [];

export async function initGlobe() {
    const canvas = document.getElementById('globeCanvas');
    if (!canvas) return;

    scene = new THREE.Scene();
    scene.background = new THREE.Color(SKY_COLOR);

    camera = new THREE.PerspectiveCamera(50, canvas.clientWidth / canvas.clientHeight, 0.1, 2000);
    camera.position.set(0, 0, 200);

    renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
    // Cap the pixel ratio — on 3x/4x-DPI screens rendering at the full
    // native ratio multiplies the pixel count for little visible gain
    // and was a real contributor to the frame-rate lag.
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.setSize(canvas.clientWidth, canvas.clientHeight);

    // Soft fill light + a "sun" so the sphere shows a realistic lit/shaded side
    scene.add(new THREE.AmbientLight(0xffffff, 1.3));
    const sun = new THREE.DirectionalLight(0xffffff, 1.1);
    sun.position.set(-2, 1, 1);
    scene.add(sun);

    world = new ThreeGlobe()
        .globeImageUrl('/assets/earth-day.jpg')
        .showAtmosphere(true)
        .atmosphereColor('#ffffff')
        .atmosphereAltitude(0.18)
        .polygonCapColor(capColorFor)
        .polygonSideColor(sideColorFor)
        .polygonStrokeColor(strokeColorFor)
        .polygonAltitude(altitudeFor)
        .polygonsTransitionDuration(200)
        .pointLat('lat')
        .pointLng('lng')
        .pointColor('color')
        .pointAltitude(0.012)
        .pointRadius(0.45)
        .pointResolution(24)
        .pointsMerge(false)
        .pointsData([]);

    scene.add(world);

    controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = false;
    controls.rotateSpeed = 0.25;
    // GLOBE_RADIUS is hardcoded to 100 inside three-globe — going below
    // that puts the camera inside the (backface-culled) sphere, which
    // renders as a blank white screen. 118 keeps a safe margin outside
    // the atmosphere shell (radius 100 * 1.18) while still allowing a
    // close zoom on small countries.
    controls.minDistance = 118;
    controls.maxDistance = 480;

    try {
        const [topoResponse, manifest, geometryBuffer] = await Promise.all([
            fetch('/assets/countries-50m.json').then(r => r.json()),
            fetch('/assets/polygon-geometry-manifest.json').then(r => r.json()),
            fetch('/assets/polygon-geometry.bin').then(r => r.arrayBuffer())
        ]);
        countriesData = topojson.feature(topoResponse, topoResponse.objects.countries);

        // Only micro-states genuinely missing a polygon in this dataset need
        // the point-marker fallback — everything else renders as a real,
        // true-to-scale vector polygon.
        const featureNames = new Set(countriesData.features.map(f => f.properties.name));
        missingPolygonNames = new Set(
            Object.keys(tinyCountriesExtras).filter(name => !featureNames.has(name))
        );

        // Supplies the precomputed cap/side triangulation for every country so
        // the browser never has to build it live (~7.5s of blocking work for
        // all 241 countries otherwise) — see tools/globe-geometry-build.
        __setPrecomputedPolygonGeometry(manifest, geometryBuffer);

        world.polygonsData(countriesData.features);
    } catch (error) {
        console.error("Globe Error:", error);
    }

    animate();
}

function capColorFor(feat) {
    return guessedCountriesColors[feat.properties.name] || 'rgba(0,0,0,0)';
}

function sideColorFor(feat) {
    return guessedCountriesColors[feat.properties.name] ? 'rgba(0,0,0,0.25)' : 'rgba(0,0,0,0)';
}

function strokeColorFor(feat) {
    return guessedCountriesColors[feat.properties.name] ? '#222222' : 'rgba(0,0,0,0)';
}

function altitudeFor(feat) {
    return guessedCountriesColors[feat.properties.name] ? 0.006 : 0;
}

export function colorCountry(dbCountryName, hexColor) {
    if (!world || !countriesData) return;

    const cleanName = dbCountryName.trim();
    const mapName = countryNameMapping[cleanName] || cleanName;
    guessedCountriesColors[mapName] = hexColor;

    if (missingPolygonNames.has(mapName)) {
        const coords = tinyCountriesExtras[mapName];
        if (coords) {
            const existing = guessedPoints.find(p => p.name === mapName);
            if (existing) {
                existing.color = hexColor;
            } else {
                guessedPoints.push({ name: mapName, lat: coords.lat, lng: coords.lon, color: hexColor });
            }
            world.pointsData(guessedPoints);
        }
    }

    // Re-run the polygon layer's digest so the cap/side/stroke/altitude
    // accessors above are re-evaluated with the freshly updated colors
    world.polygonsData(countriesData.features);
}

function animate() {
    requestAnimationFrame(animate);
    if (controls) controls.update();
    if (renderer && scene && camera) renderer.render(scene, camera);
}

export function focusOnCountry(lat, lon) {
    if (!world || !camera) return;

    // getCoords(lat, lng, altitude) returns the point on the globe's surface
    // scaled out by (1 + altitude) globe-radii — reused directly as a close-up
    // camera position so tiny countries fill much more of the screen.
    const { x, y, z } = world.getCoords(lat, lon, 0.9);
    camera.position.set(x, y, z);
    camera.lookAt(0, 0, 0);
    if (controls) controls.update();
}
