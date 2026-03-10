import * as THREE from 'three';
import { OrbitControls } from '/controls/OrbitControls.js';

// מילון שמות - קריטי להתאמה בין ה-DB למפה
const countryNameMapping = {
    "Trinidad and Tobago": "Trinidad and Tobago",
    "Saint Vincent and the Grenadines": "St. Vin. and Gren.",
    "Antigua and Barbuda": "Antigua and Barb.",
    "USA": "United States of America",
    "UK": "United Kingdom",
    "South Korea": "Korea",
    "North Korea": "Dem. Rep. Korea"
};

// מילון למדינות קטנות (VIP) - מיקומים מדויקים למעוינים
const tinyCountriesExtras = {
    "Trinidad and Tobago": { lat: 10.69, lon: -61.22 },
    "St. Vin. and Gren.": { lat: 13.25, lon: -61.20 },
    "Grenada": { lat: 12.11, lon: -61.67 },
    "Saint Lucia": { lat: 13.90, lon: -60.96 },
    "Antigua and Barb.": { lat: 17.06, lon: -61.79 },
    "Barbados": { lat: 13.19, lon: -59.54 },
    "Luxembourg": { lat: 49.81, lon: 6.12 },
    "Monaco": { lat: 43.73, lon: 7.42 }
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

function updateGlobeTexture() {
    if (!countriesData) return;

    const canvas = document.createElement('canvas');
    canvas.width = 2048;
    canvas.height = 1024;
    const ctx = canvas.getContext('2d');

    // רקע אוקיינוס
    ctx.fillStyle = '#87CEEB';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // ציור כל המדינות
    countriesData.features.forEach(country => {
        const name = country.properties.name;
        const color = guessedCountriesColors[name] || '#D3D3D3';

        ctx.fillStyle = color;
        ctx.strokeStyle = '#333333';
        ctx.lineWidth = 0.5;

        const coords = country.geometry.coordinates;
        ctx.beginPath();
        if (country.geometry.type === "Polygon") {
            renderRings(ctx, coords, canvas.width, canvas.height);
        } else {
            coords.forEach(poly => renderRings(ctx, poly, canvas.width, canvas.height));
        }
        ctx.fill('evenodd');
        ctx.stroke();

        // ציור המעוין למדינות קטנות שניחשו
        if (guessedCountriesColors[name] && tinyCountriesExtras[name]) {
            const extra = tinyCountriesExtras[name];
            const x = (extra.lon + 180) * (canvas.width / 360);
            const y = (90 - extra.lat) * (canvas.height / 180);

            ctx.save();
            ctx.shadowBlur = 10;
            ctx.shadowColor = guessedCountriesColors[name];
            ctx.fillStyle = guessedCountriesColors[name];

            const size = 6;
            ctx.beginPath();
            ctx.moveTo(x, y - size);
            ctx.lineTo(x + size, y);
            ctx.lineTo(x, y + size);
            ctx.lineTo(x - size, y);
            ctx.closePath();
            ctx.fill();
            ctx.restore();
        }
    });

    const texture = new THREE.CanvasTexture(canvas);
    if (globe.material.map) globe.material.map.dispose();
    globe.material.map = texture;
    globe.material.needsUpdate = true;
}

function renderRings(ctx, rings, width, height) {
    rings.forEach(ring => {
        let prevX = null;
        ring.forEach(([lon, lat], i) => {
            const x = (lon + 180) * (width / 360);
            const y = (90 - lat) * (height / 180);
            if (i === 0 || (prevX !== null && Math.abs(x - prevX) > width / 2)) {
                ctx.moveTo(x, y);
            } else {
                ctx.lineTo(x, y);
            }
            prevX = x;
        });
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