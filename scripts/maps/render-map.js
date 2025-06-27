const fs = require('fs-extra');
const path = require('path');
const { PNG } = require('pngjs');
const maplibre = require('@maplibre/maplibre-gl-native');
const sharp = require('sharp')

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
        bbox
    } = options;

    if (DEBUG) console.log(`Debug: Starting map rendering with options:`, JSON.stringify(options, null, 2));

    let effectiveZoom = zoom;
    if (bbox && (zoom === null || zoom === undefined || zoom < 0)) {
        if (DEBUG) console.log(`Debug: Calculating zoom from bounding box: ${bbox}`);
        effectiveZoom = getBoundsZoomLevel(bbox, width, height);
        // Subtract a bit for padding
        effectiveZoom = effectiveZoom > 0.5 ? effectiveZoom - 0.5 : effectiveZoom;
        if (DEBUG) console.log(`Debug: Calculated zoom level: ${effectiveZoom}`);
    }

    try {
        // Read the GeoJSON file
        if (DEBUG) console.log(`Debug: Reading GeoJSON from ${geojsonPath}`);
        const geojsonData = await fs.readJson(geojsonPath);
        
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
            if (DEBUG) console.log(`Reading sources`);
            Object.keys(styleData.sources).forEach(sourceId => {
                if (DEBUG) console.log(`sourceID --${sourceId}--`);
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
                        /* Convert to MapLibre MBTiles source declaration */
                        originalUrl = source.url;
                        delete source.url;
                        source.type = "mbtiles";
                        source.path = path.resolve(mbtilesPath);
                        targetUrl = source.path;
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
                    const filePath = req.url.replace('file://', '');
                    fs.readFile(filePath, (err, data) => {
                        if (err) {
                            console.error(`Error reading file: ${err.message}`);
                            return callback(err);
                        }
                        return callback(null, { data });
                    });
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
            center: center,
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
        // This is a simplified version of the Mercator projection formula part
        const rad = Math.log((1 + sin) / (1 - sin)) / 2;
        return Math.abs(rad);
    }

    // Calculate the fraction of the world's circumference covered by the bbox
    const lngFraction = Math.abs(maxLng - minLng) / 360;
    const latFraction = (latRad(maxLat) - latRad(minLat)) / (2 * Math.PI);

    // Calculate required zoom level for both width and height
    const lngZoom = Math.log(imageWidth / (TILE_SIZE * lngFraction)) / Math.LN2;
    const latZoom = Math.log(imageHeight / (TILE_SIZE * latFraction)) / Math.LN2;

    // The final zoom is the minimum of the two, to ensure the whole box fits
    const zoom = Math.min(lngZoom, latZoom, MAX_ZOOM);

    return zoom > 0 ? zoom : 0; // Ensure zoom is not negative
}


// Parse command line arguments
const args = process.argv.slice(2);
if (args.length < 8) {
    console.error('Usage: node render-map.js <geojsonPath> <mbtilesPath> <stylePath> <outputPath> <width> <height> <centerLng> <centerLat> [<zoom> [<bbox>]]');
    process.exit(1);
}

const geojsonPath = args[0];
const mbtilesPath = args[1];
const stylePath = args[2];
const outputPath = args[3];
const width = parseInt(args[4], 10);
const height = parseInt(args[5], 10);
const centerLng = parseFloat(args[6]);
const centerLat = parseFloat(args[7]);
// If zoom is not provided or is invalid, set to -1 to trigger auto-calculation
const zoom = (args[8] && !isNaN(parseFloat(args[8]))) ? parseFloat(args[8]) : -1;
// Bbox is expected as a comma-separated string: "minLng,minLat,maxLng,maxLat"
const bbox = args[9] ? args[9].split(',').map(Number) : null;

renderMap({
    geojsonPath,
    mbtilesPath,
    stylePath,
    outputPath,
    width,
    height,
    center: [centerLng, centerLat],
    zoom,
    bbox
}).then(success => {
    process.exit(success ? 0 : 1);
});
