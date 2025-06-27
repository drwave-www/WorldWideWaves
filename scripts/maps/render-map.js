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
        zoom
    } = options;

    if (DEBUG) console.log(`Debug: Starting map rendering with options:`, JSON.stringify(options, null, 2));

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
            styleData.sprite = `${path.resolve(styleDir, 'sprites/osm-liberty')}`;
            if (DEBUG) console.log(`Debug: Changed sprite from ${originalSprite} to ${styleData.sprite}`);
        }
        
        // Fix glyphs paths
        if (styleData.glyphs) {
            const originalGlyphs = styleData.glyphs;
            styleData.glyphs = `${path.resolve(styleDir, 'glyphs/{fontstack}/{range}.pbf')}`;
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
                        originalUrl = source.url;
                        source.url = `${path.resolve(mbtilesPath)}`;
                        targetUrl = source.url;
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
                console.log(`READ ${req.url}`)
                try {
                  fs.readFile(path.join(req.url), function(err, data) {
                    if (err) {
                      console.error(`Error reading file ${filePath}: ${err.message}`);
                    }
                    callback(err, { data: data });
                  });
                } catch (error) {
                  console.error(`Request error: ${error.message}`);
                  callback(error);
                }
            },
        });
        
        // Load the style using the proper callback-based approach
        console.log('Debug: Loading map style...');
        
        // Wrap the map.load() callback in a Promise for async/await compatibility
        map.load(styleData)

        console.log('Debug: Starting map rendering');

        return new Promise((resolve, reject) => {
          map.render({
            zoom: zoom,
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

// Parse command line arguments
const args = process.argv.slice(2);
if (args.length < 7) {
    console.error('Usage: node render-map.js <geojsonPath> <mbtilesPath> <stylePath> <outputPath> <width> <height> <centerLat> <centerLng> [<zoom>]');
    process.exit(1);
}

const geojsonPath = args[0];
const mbtilesPath = args[1];
const stylePath = args[2];
const outputPath = args[3];
const width = parseInt(args[4], 10);
const height = parseInt(args[5], 10);
const centerLat = parseFloat(args[6]);
const centerLng = parseFloat(args[7]);
const zoom = args[8] ? parseFloat(args[8]) : 10;

renderMap({
    geojsonPath,
    mbtilesPath,
    stylePath,
    outputPath,
    width,
    height,
    center: [centerLng, centerLat],
    zoom
}).then(success => {
    process.exit(success ? 0 : 1);
});
