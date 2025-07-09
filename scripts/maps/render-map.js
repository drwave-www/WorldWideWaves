const fs = require('fs-extra');
const path = require('path');
const { PNG } = require('pngjs');
const maplibre  = require('@maplibre/maplibre-gl-native');
const sharp = require('sharp')
const sqlite3   = require('sqlite3').verbose();
const zlib      = require('zlib');

// Debug mode
const DEBUG = true;

// Add a delay function for async waiting
const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Function to render a map
async function renderMap(options) {
    const {
        geojsonPath,
        mbtilesPath,
        stylePath,
        outputPath,
        width,
        height,
        center,
        zoom,
        bbox       // <- optional [minLng,minLat,maxLng,maxLat]
    } = options;

    if (DEBUG) console.log(`Debug: Starting map rendering with options:`, JSON.stringify(options, null, 2));

    try {
        // Read the GeoJSON file
        if (DEBUG) console.log(`Debug: Reading GeoJSON from ${geojsonPath}`);
        const geojsonData = await fs.readJson(geojsonPath);

        /* ------------------------------------------------------------------
         * Bounds: prefer explicit bbox (from configuration) over GeoJSON.
         * ------------------------------------------------------------------ */
        let geojsonBbox = null;

        if (Array.isArray(bbox) && bbox.length === 4) {
            geojsonBbox = bbox.map(Number);
            if (DEBUG) console.log(`Debug: Using provided bbox override: ${geojsonBbox}`);
        } else {
            geojsonBbox = getGeojsonBounds(geojsonData);
            if (DEBUG) console.log(`Debug: Calculated GeoJSON bbox: ${geojsonBbox}`);
        }

        // ------------------------------------------------------------------
        // Validate bounds – empty GeoJSON returns infinities
        // ------------------------------------------------------------------
        const hasValidBounds = isValidBounds(geojsonBbox);
        if (!hasValidBounds) {
            console.warn(
                `[WARN] GeoJSON file '${path.basename(
                    geojsonPath
                )}' seems empty – falling back to default center/zoom`
            );
        }

        /* ------------------------------------------------------------------
         * Determine center:
         *   1. Explicit center provided on CLI
         *   2. Derive from bbox (explicit or geojson)
         *   3. Fallback hint from filename
         * ------------------------------------------------------------------ */
        let effectiveCenter = [
            (geojsonBbox[0] + geojsonBbox[2]) / 2,
            (geojsonBbox[1] + geojsonBbox[3]) / 2,
        ];

        if (center && center.length === 2 && !isNaN(center[0]) && !isNaN(center[1])) {
            effectiveCenter = center;
            if (DEBUG) console.log(`Debug: Using provided center override: ${effectiveCenter}`);
        } else {
            if (DEBUG) console.log(`Debug: Using calculated center from GeoJSON: ${effectiveCenter}`);
        }

        let effectiveZoom = zoom;
        if (zoom === null || zoom === undefined || zoom < 0) {
            if (DEBUG) console.log(`Debug: Calculating zoom from GeoJSON bbox...`);
            effectiveZoom = hasValidBounds
                ? getBoundsZoomLevel(geojsonBbox, width, height)
                : 10; // sensible default for fallback
            if (DEBUG) console.log(`Debug: Calculated zoom level: ${effectiveZoom}`);
        }
        
        // Read the style file
        if (DEBUG) console.log(`Debug: Reading style from ${stylePath}`);
        const styleData = await fs.readJson(stylePath);
        
        // Make paths in the style absolute
        const styleDir = path.dirname(stylePath);
        if (DEBUG) console.log(`Debug: Style directory is ${styleDir}`);
        
        // Fix sprite paths
        if (styleData.sprite) {
            const originalSprite = styleData.sprite;
            styleData.sprite = `file://${path.resolve(styleDir, 'sprites/osm-liberty')}`;
            if (DEBUG) console.log(`Debug: Changed sprite from ${originalSprite} to ${styleData.sprite}`);
        }
        
        // Fix glyphs paths
        if (styleData.glyphs) {
            const originalGlyphs = styleData.glyphs;
            styleData.glyphs = `file://${path.resolve(styleDir, 'glyphs/{fontstack}/{range}.pbf')}`;
            if (DEBUG) console.log(`Debug: Changed glyphs from ${originalGlyphs} to ${styleData.glyphs}`);
        }
        
        // Fix source paths
        if (styleData.sources) {
            Object.keys(styleData.sources).forEach(sourceId => {
                const source = styleData.sources[sourceId];
                if (source.url && !source.url.startsWith('http') || source.data) {
                    var originalUrl = "";
                    var targetUrl = "";

                    // Use different paths based on source ID
                    if (sourceId === "geojson") {
                        originalUrl = source.data;
                        source.data = geojsonData;
                        targetUrl = "{GEOJSON DATA}";
                    } else if (sourceId === "openmaptiles") {
                        /* Serve tiles through mbtiles:// protocol */
                        originalUrl = source.url;
                        delete source.url;
                        // Keep regular vector source but point its tiles array to mbtiles://
                        source.type  = "vector";
                        const absPath = path.resolve(mbtilesPath);
                        source.tiles = [`mbtiles://${absPath}/{z}/{x}/{y}.pbf`];
                        targetUrl    = source.tiles[0];
                    } else {
                        console.log(`Unrecognized source`);
                    }

                    if (DEBUG) console.log(`Debug: Changed source ${sourceId} URL from ${originalUrl} to ${targetUrl}`);
                }
            });
        }

        fs.writeFileSync('/tmp/tmp-mapstyle.json', JSON.stringify(styleData, null, 2));
        if (DEBUG) console.log('Debug: Dumped modified style data to /tmp/debug-mapstyle.json');
        
        if (DEBUG) console.log('Debug: Creating map instance');
        
        // Create the map
        const map = new maplibre.Map({
            request: function(req, callback) {
                if (req.url.startsWith('file://')) {
                    /* ---- local filesystem fetch (sprites, glyphs, etc.) ---- */
                    // Decode URI components so that `%20` and other encodings
                    // are converted back to their literal characters. This is
                    // required for font stack names that contain spaces
                    // (e.g. “Roboto Regular”), which are stored on disk with
                    // the actual space character.
                    const rawPath  = req.url.replace('file://', '');
                    const filePath = decodeURIComponent(rawPath);
                    fs.readFile(filePath, (err, data) => {
                        if (err) {
                            console.error(`Error reading file: ${err.message}`);
                            return callback(err);
                        }
                        return callback(null, { data });
                    });
                    return;
                }

                /* ---- MBTiles protocol ---- */
                if (req.url.startsWith('mbtiles://')) {
                    handleMbtilesRequest(req.url)
                        .then(buf => callback(null, { data: buf }))
                        .catch(e  => callback(e));
                    return;
                }

                /* Unsupported protocol */
                const err = new Error(`Unsupported protocol in URL: ${req.url}`);
                console.error(err.message);
                callback(err);
            },
        });
        
        // Load the style using the proper callback-based approach
        console.log('Debug: Loading map style...');
        
        // Wrap the map.load() callback in a Promise for async/await compatibility
        map.load(styleData)

        console.log('Debug: Starting map rendering');

        return new Promise((resolve, reject) => {
          map.render({
            zoom: effectiveZoom,
            width: width,
            height: height,
            center: effectiveCenter,
            ratio: 1
          }, function(err, buffer) {
            if (err) {
                console.error(`Render error: ${err.message}`);
                map.release();
                reject(err);
                return;
            }

            try {
                map.release();

                var image = sharp(buffer, {
                    raw: {
                        width: width,
                        height: height,
                        channels: 4
                    }
                });

                // Convert raw image buffer to PNG
                image.toFile(outputPath, function(err) {
                    if (err) {
                        console.log(`Error writing file: ${err}`);
                        reject(err);
                        return;
                    }
                    console.log(`Map rendered successfully: ${outputPath}`);
                    resolve(true);
                });
            } catch (error) {
                console.error(`Processing error: ${error.message}`);
                reject(error);
            }            
          });
        });

    } catch (error) {
        // More detailed error handling
        console.error(`Error rendering map: ${error.message}`);
        console.error(error.stack);
        
        // Try to inspect the style file
        try {
            const styleData = await fs.readJson(stylePath);
            console.log(`Style version: ${styleData.version}`);
            console.log(`Style has ${Object.keys(styleData.sources || {}).length} sources`);
            console.log(`Style has ${(styleData.layers || []).length} layers`);
            
            // Check critical paths
            const styleDir = path.dirname(stylePath);
            const absoluteStylePath = path.resolve(styleDir);
            console.log(`Absolute style directory: ${absoluteStylePath}`);
        } catch (e) {
            console.error(`Error analyzing style file: ${e.message}`);
        }
        
        return false;
    }
}

/**
 * Calculates the optimal zoom level to fit a bounding box within given pixel dimensions.
 * @param {number[]} bbox - The bounding box as [minLng, minLat, maxLng, maxLat].
 * @param {number} imageWidth - The width of the image in pixels.
 * @param {number} imageHeight - The height of the image in pixels.
 * @returns {number} The optimal zoom level.
 */
function getBoundsZoomLevel(bbox, imageWidth, imageHeight) {
    const [minLng, minLat, maxLng, maxLat] = bbox;
    const TILE_SIZE = 512; // MapLibre uses 512px tiles
    const MAX_ZOOM = 24;

    // Helper to convert latitude to its Mercator projection coordinate
    function latRad(lat) {
        const sin = Math.sin(lat * Math.PI / 180);
        // Mercator projection (no absolute value – we need the sign to
        // distinguish northern vs. southern hemisphere)
        const rad = Math.log((1 + sin) / (1 - sin)) / 2;
        return rad;
    }

    // Calculate the fraction of the world's circumference covered by the bbox
    const lngFraction = (maxLng - minLng) === 0 ? 0 : Math.abs(maxLng - minLng) / 360;
    const latFraction = (latRad(maxLat) - latRad(minLat)) === 0 ? 0 : (latRad(maxLat) - latRad(minLat)) / (2 * Math.PI);

    if (lngFraction === 0 || latFraction === 0) {
        return MAX_ZOOM; // It's a point or a straight line, zoom in max
    }

    // Calculate required zoom level for both width and height
    const lngZoom = Math.log(imageWidth / (TILE_SIZE * lngFraction)) / Math.LN2;
    const latZoom = Math.log(imageHeight / (TILE_SIZE * latFraction)) / Math.LN2;

    // The final zoom is the minimum of the two, to ensure the whole box fits
    const zoom = Math.min(lngZoom, latZoom, MAX_ZOOM);

    return zoom > 0 ? zoom : 0; // Ensure zoom is not negative
}

/**
 * Calculates the bounding box of a GeoJSON object.
 * @param {object} geojson - The GeoJSON object.
 * @returns {number[]} The bounding box as [minLng, minLat, maxLng, maxLat].
 */
function getGeojsonBounds(geojson) {
    const bounds = [Infinity, Infinity, -Infinity, -Infinity]; // [minLng, minLat, maxLng, maxLat]

    function processCoordinates(coordinates) {
        if (!coordinates) return;
        // Check if it's a single coordinate pair
        if (typeof coordinates[0] === 'number' && typeof coordinates[1] === 'number') {
            bounds[0] = Math.min(bounds[0], coordinates[0]);
            bounds[1] = Math.min(bounds[1], coordinates[1]);
            bounds[2] = Math.max(bounds[2], coordinates[0]);
            bounds[3] = Math.max(bounds[3], coordinates[1]);
        } else { // It's an array of coordinates
            for (const coord of coordinates) {
                processCoordinates(coord);
            }
        }
    }

    function processGeometry(geometry) {
        if (!geometry || !geometry.coordinates) return;
        if (geometry.type === 'GeometryCollection') {
            for (const geom of geometry.geometries) {
                processGeometry(geom);
            }
        } else {
            processCoordinates(geometry.coordinates);
        }
    }

    if (geojson.type === 'FeatureCollection') {
        for (const feature of geojson.features) {
            processGeometry(feature.geometry);
        }
    } else if (geojson.type === 'Feature') {
        processGeometry(geojson.geometry);
    } else { // It's a geometry object
        processGeometry(geojson);
    }

    return bounds;
}

/**
 * Returns true if bbox numbers are finite and not equal to infinities.
 */
function isValidBounds(bbox) {
    return (
        bbox &&
        bbox.length === 4 &&
        bbox.every((n) => Number.isFinite(n)) &&
        !(bbox[0] === Infinity ||
            bbox[1] === Infinity ||
            bbox[2] === -Infinity ||
            bbox[3] === -Infinity)
    );
}

/**
 * Very small helper: try to guess a center coord from the filename.
 * Extend this dictionary as needed.
 */
function getHintCenterFromFilename(filePath) {
    const name = path.basename(filePath, path.extname(filePath)).toLowerCase();
    const hints = {
        london_england: [-0.1276, 51.5072],
        new_york_usa: [-73.9795, 40.6971],
        paris_france: [2.3522, 48.8566],
    };
    return hints[name] || [0, 0];
}

/* -------------------------------------------------------------------------- */
/* -----------------------  MBTiles   helper  section  ----------------------- */
/* -------------------------------------------------------------------------- */

// Simple in-memory cache of opened mbtiles databases to avoid reopening files
const mbtilesCache = new Map(); //  key: absolute path, value: sqlite3.Database

/**
 * Open (or retrieve from cache) a readonly sqlite3 DB handle to the given
 * mbtiles file.  The handle is cached for the lifetime of this process.
 */
function getDb(absPath) {
    if (mbtilesCache.has(absPath)) {
        return mbtilesCache.get(absPath);
    }
    const db = new sqlite3.Database(absPath, sqlite3.OPEN_READONLY);
    mbtilesCache.set(absPath, db);
    return db;
}

/**
 * Parse a `mbtiles://` URL of the form
 *   mbtiles:///absolute/path/file.mbtiles/{z}/{x}/{y}.pbf
 * and return `{ dbPath, z, x, y }`
 */
function parseMbtilesUrl(url) {
    const withoutProto = url.replace('mbtiles://', '');
    // pattern:  /absolute/path/file.mbtiles/Z/X/Y(.pbf|.png)
    const match = withoutProto.match(/^(.*?\.mbtiles)\/(\d+)\/(\d+)\/(\d+)/);
    if (!match) {
        throw new Error(`Invalid mbtiles url: ${url}`);
    }
    const [, dbPath, zStr, xStr, yStr] = match;
    const z = parseInt(zStr, 10);
    const x = parseInt(xStr, 10);
    const y = parseInt(yStr, 10);
    return { dbPath, z, x, y };
}

/**
 * Given a `mbtiles://` URL return a Promise that resolves with a Buffer
 * containing the tile data.  Converts XYZ → TMS (row) index internally.
 */
function handleMbtilesRequest(url) {
    return new Promise((resolve, reject) => {
        let parsed;
        try {
            parsed = parseMbtilesUrl(url);
        } catch (e) {
            return reject(e);
        }

        const { dbPath, z, x, y } = parsed;
        const db = getDb(dbPath);

        // MBTiles spec stores rows in TMS (flipped-Y) orientation
        const tmsY = (1 << z) - 1 - y;

        db.get(
            'SELECT tile_data FROM tiles WHERE zoom_level=? AND tile_column=? AND tile_row=?',
            [z, x, tmsY],
            (err, row) => {
                if (err) return reject(err);
                // If tile is missing, resolve with an empty buffer instead of
                // failing the entire render. Missing tiles will simply render
                // blank areas, which is preferable to aborting the map render.
                if (!row) {
                    if (DEBUG) {
                        console.warn(`Tile not found ${z}/${x}/${y} in ${path.basename(dbPath)}`);
                    }
                    return resolve(Buffer.alloc(0));
                }
                let data = row.tile_data;
                /* ----------------------------------------------------------
                 * Tiles inside MBTiles are commonly gzipped.  MapLibre expects
                 * raw, uncompressed PBF bytes.  If we detect the gzip magic
                 * header (0x1f 0x8b) we transparently decompress.
                 * ---------------------------------------------------------- */
                if (data && data.length > 2 && data[0] === 0x1f && data[1] === 0x8b) {
                    try {
                        data = zlib.unzipSync(data);
                    } catch (e) {
                        return reject(e);
                    }
                }
                resolve(data);
            }
        );
    });
}

// Parse command line arguments
const args = process.argv.slice(2);
if (args.length < 6) {
    console.error(
        'Usage: node render-map.js ' +
        '<geojsonPath> <mbtilesPath> <stylePath> <outputPath> <width> <height> ' +
        '[<centerLng> <centerLat> [<zoom>]] | ' +
        '[<minLng> <minLat> <maxLng> <maxLat>]'
    );
    process.exit(1);
}

const geojsonPath = args[0];
const mbtilesPath = args[1];
const stylePath = args[2];
const outputPath = args[3];
const width = parseInt(args[4], 10);
const height = parseInt(args[5], 10);

/* -------------------------------------------------------------------------- */
/* ------------------------  Optional argument parsing  --------------------- */
/* -------------------------------------------------------------------------- */

function isNumeric(v) {
    return v !== undefined && v !== null && !isNaN(parseFloat(v));
}

const remaining = args.slice(6);          // after the 6 mandatory params
let center = null;
let zoom   = -1;                          // auto-calc by default
let bbox   = null;

/* --------------------------------------------------------------------------
 * Debug: inspect CLI optional arguments
 * -------------------------------------------------------------------------- */
if (DEBUG) {
    console.log('Debug: Remaining CLI args:', remaining);
    console.log(
        'Debug: isNumeric flags:',
        remaining.map(v => `${v}:${isNumeric(v)}`)
    );
    console.log('Debug: remaining.length =', remaining.length);
}

// Detect “bbox mode” – exactly 4 numeric values
if (remaining.length === 4 && remaining.every(isNumeric)) {
    bbox = remaining.map(Number);         // [minLng,minLat,maxLng,maxLat]
    if (DEBUG) console.log('Debug: Detected bbox mode. Parsed bbox =', bbox);
} else {
    // Legacy center/zoom mode
    if (remaining.length >= 2 && isNumeric(remaining[0]) && isNumeric(remaining[1])) {
        center = [parseFloat(remaining[0]), parseFloat(remaining[1])];
    }
    if (remaining.length >= 3 && isNumeric(remaining[2])) {
        zoom = parseFloat(remaining[2]);
    }
    if (DEBUG) console.log('Debug: Detected center/zoom mode.', { center, zoom });
}

renderMap({
    geojsonPath,
    mbtilesPath,
    stylePath,
    outputPath,
    width,
    height,
    center,
    zoom,
    bbox
}).then(success => {
    process.exit(success ? 0 : 1);
});
