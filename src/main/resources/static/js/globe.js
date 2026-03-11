import * as THREE from 'three';
import { OrbitControls } from '/controls/OrbitControls.js';

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

// Small countries with precise coordinates - drawn as tiny polygons on the texture
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

let scene, camera, renderer, globe, controls;
let countriesData = null;
let guessedCountriesColors = {};

export async function initGlobe() {
    const canvas = document.getElementById('globeCanvas');
    if (!canvas) return;

    scene = new THREE.Scene();
    scene.background = new THREE.Color(0xffffff);

    camera = new THREE.PerspectiveCamera(45, canvas.clientWidth / canvas.clientHeight, 0.1, 1000);
    camera.position.set(0, 0, 4);

    renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
    renderer.setSize(canvas.clientWidth, canvas.clientHeight);

    const ambientLight = new THREE.AmbientLight(0xffffff, 1.2);
    scene.add(ambientLight);

    const geometry = new THREE.SphereGeometry(1.5, 64, 64);
    const material = new THREE.MeshBasicMaterial({ color: 0xffffff });
    globe = new THREE.Mesh(geometry, material);
    scene.add(globe);

    controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.rotateSpeed = 0.3;

    try {
        const response = await fetch('https://unpkg.com/world-atlas@2.0.2/countries-50m.json');
        const topoData = await response.json();
        countriesData = topojson.feature(topoData, topoData.objects.countries);

        updateGlobeTexture();
    } catch (error) {
        console.error("Globe Error:", error);
    }

    animate();
}

// Draws a small irregular polygon centered at (cx, cy) with given size and color
function drawTinyCountryPolygon(ctx, cx, cy, size, color) {
    const offsets = [
        { dx: 0,           dy: -size },
        { dx:  size,       dy: -size * 0.3 },
        { dx:  size * 0.7, dy:  size },
        { dx: -size * 0.5, dy:  size },
        { dx: -size,       dy:  size * 0.1 }
    ];

    ctx.beginPath();
    ctx.moveTo(cx + offsets[0].dx, cy + offsets[0].dy);
    for (let i = 1; i < offsets.length; i++) {
        ctx.lineTo(cx + offsets[i].dx, cy + offsets[i].dy);
    }
    ctx.closePath();

    ctx.fillStyle = color;
    ctx.fill();
    ctx.strokeStyle = '#333333';
    ctx.lineWidth = 0.5;
    ctx.stroke();
}

function updateGlobeTexture() {
    if (!countriesData) return;

    const canvas = document.createElement('canvas');
    canvas.width = 2048;
    canvas.height = 1024;
    const ctx = canvas.getContext('2d');

    // Ocean background
    ctx.fillStyle = '#87CEEB';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // --- ANTARCTICA HOLE FIX (SEAMLESS) ---
    const antarcticaColor = guessedCountriesColors["Antarctica"] || '#D3D3D3';
    ctx.fillStyle = antarcticaColor;
    ctx.fillRect(0, canvas.height - 40, canvas.width, 40);
    // --------------------------------------

    // Draw all countries from TopoJSON
    countriesData.features.forEach(country => {
        const name = country.properties.name;
        const color = guessedCountriesColors[name] || '#D3D3D3';

        ctx.fillStyle = color;
        ctx.strokeStyle = '#333333';
        ctx.lineWidth = 0.5;

        // Create a reusable function to draw the country path
        const drawCountryPath = () => {
            ctx.beginPath();
            const coords = country.geometry.coordinates;
            if (country.geometry.type === "Polygon") {
                renderRings(ctx, coords, canvas.width, canvas.height);
            } else {
                coords.forEach(poly => renderRings(ctx, poly, canvas.width, canvas.height));
            }
            ctx.fill('evenodd');
            ctx.stroke();
        };

        // 1. Draw normally (Center)
        drawCountryPath();

        // 2. Draw shifted left (Fixes parts extending past the right edge)
        ctx.save();
        ctx.translate(-canvas.width, 0);
        drawCountryPath();
        ctx.restore();

        // 3. Draw shifted right (Fixes parts extending past the left edge)
        ctx.save();
        ctx.translate(canvas.width, 0);
        drawCountryPath();
        ctx.restore();
    });

    // Draw tiny countries
    Object.entries(tinyCountriesExtras).forEach(([name, extra]) => {
        const color = guessedCountriesColors[name];
        if (!color) return;

        const cx = (extra.lon + 180) * (canvas.width / 360);
        const cy = (90 - extra.lat) * (canvas.height / 180);

        drawTinyCountryPolygon(ctx, cx, cy, 3, color);
    });

    const texture = new THREE.CanvasTexture(canvas);
    if (globe.material.map) globe.material.map.dispose();
    globe.material.map = texture;
    globe.material.needsUpdate = true;
}

function renderRings(ctx, rings, width, height) {
    rings.forEach(ring => {
        if (ring.length === 0) return;

        // Start the shape
        let currentLon = ring[0][0];
        const startX = (currentLon + 180) * (width / 360);
        const startY = (90 - ring[0][1]) * (height / 180);
        ctx.moveTo(startX, startY);

        for (let i = 1; i < ring.length; i++) {
            let [lon, lat] = ring[i];

            // --- ANTI-MERIDIAN FIX ---
            // If the distance between the previous point and this point is huge,
            // it means we crossed the 180 line. Instead of jumping across the screen,
            // we adjust the longitude to keep drawing continuously outside the canvas.
            while (lon - currentLon > 180) lon -= 360;
            while (currentLon - lon > 180) lon += 360;

            currentLon = lon;

            const x = (lon + 180) * (width / 360);
            const y = (90 - lat) * (height / 180);
            ctx.lineTo(x, y);
        }
    });
}

export function colorCountry(dbCountryName, hexColor) {
    if (!countriesData) return;
    const cleanName = dbCountryName.trim();
    const mapName = countryNameMapping[cleanName] || cleanName;
    guessedCountriesColors[mapName] = hexColor;
    updateGlobeTexture();
}

function animate() {
    requestAnimationFrame(animate);
    if (controls) controls.update();
    if (renderer && scene && camera) renderer.render(scene, camera);
}

export function focusOnCountry(lat, lon) {
    const phi = (90 - lat) * (Math.PI / 180);
    const theta = (lon + 90) * (Math.PI / 180);
    const radius = 4;
    camera.position.set(
        radius * Math.sin(phi) * Math.sin(theta),
        radius * Math.cos(phi),
        radius * Math.sin(phi) * Math.cos(theta)
    );
    camera.lookAt(0, 0, 0);
}