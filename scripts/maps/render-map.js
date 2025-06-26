const fs = require('fs-extra');
const path = require('path');
const { PNG } = require('pngjs');
const maplibre = require('@maplibre/maplibre-gl-native');

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
            styleData.sprite = `file://${path.resolve(styleDir, 'sprites')}`;
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
                        originalUrl = source.url;
                        source.url = `file://${path.resolve(mbtilesPath)}`;
                        targetUrl = source.url;
                    } else {
                        console.log(`Unrecognized source`);
                    }

                    if (DEBUG) console.log(`Debug: Changed source ${sourceId} URL from ${originalUrl} to ${targetUrl}`);
                }
            });
        }

        fs.writeFileSync('/tmp/debug-mapstyle.json', JSON.stringify(styleData, null, 2));
        if (DEBUG) console.log('Debug: Dumped modified style data to /tmp/debug-mapstyle.json');
        
        if (DEBUG) console.log('Debug: Creating map instance');
        
        // Create the map
        const map = new maplibre.Map({
            width,
            height,
            ratio: 1,
            center,
            zoom,
            style: styleData,
            request: (req, callback) => {
                try {
                    // Handle file requests (sprites, fonts, etc.)
                    const url = req.url;
                    if (DEBUG) console.log(`Debug: Request for ${url}`);
                    
                    if (url.startsWith('file://')) {
                        const filePath = url.replace('file://', '');
                        if (fs.existsSync(filePath)) {
                            if (DEBUG) console.log(`Debug: Found file ${filePath}`);
                            const data = fs.readFileSync(filePath);
                            callback(null, { data });
                        } else {
                            if (DEBUG) console.log(`Debug: File not found: ${filePath}`);
                            callback(new Error(`File not found: ${filePath}`));
                        }
                    } else {
                        if (DEBUG) console.log(`Debug: Unsupported URL: ${url}`);
                        callback(new Error(`Unsupported URL: ${url}`));
                    }
                } catch (error) {
                    console.error(`Request error: ${error.message}`);
                    callback(error);
                }
            }
        });
        
        // Load the style using the proper callback-based approach
        console.log('Debug: Loading map style...');
        
        // Wrap the map.load() callback in a Promise for async/await compatibility
        await new Promise((resolve, reject) => {
            map.load((err) => {
                if (err) {
                    console.error(`Debug: Style loading failed: ${err.message}`);
                    reject(new Error(`Failed to load map style: ${err.message}`));
                } else {
                    console.log('Debug: Style successfully loaded');
                    resolve();
                }
            });
        });
        
        // Dump loaded layers for debugging
        console.log('Debug: Loaded style layers:');
        try {
            const layers = map.getStyle().layers || [];
            layers.forEach(layer => {
                console.log(`  - Layer: ${layer.id} (${layer.type})`);
            });
        } catch (e) {
            console.log(`Debug: Error getting style layers: ${e.message}`);
        }
        
        console.log('Debug: Starting map rendering');
        
        // Render the map
        const pixels = await new Promise((resolve, reject) => {
            try {
                map.render((err, buffer) => {
                    if (err) {
                        console.error(`Render error: ${err.message}`);
                        console.error(err.stack);
                        reject(err);
                    }
                    else resolve(buffer);
                });
            } catch (error) {
                console.error(`Unexpected render error: ${error.message}`);
                console.error(error.stack);
                reject(error);
            }
        });
        
        if (DEBUG) console.log('Debug: Map rendered, creating PNG');
        
        // Create a PNG from the pixels
        const png = new PNG({
            width,
            height,
            inputHasAlpha: true
        });
        
        // Copy the pixels to the PNG
        for (let i = 0; i < pixels.length; i++) {
            png.data[i] = pixels[i];
        }
        
        if (DEBUG) console.log('Debug: Saving PNG to', outputPath);
        
        // Write the PNG to a file
        await new Promise((resolve, reject) => {
            png.pack()
                .pipe(fs.createWriteStream(outputPath))
                .on('finish', resolve)
                .on('error', reject);
        });
        
        console.log(`Map rendered successfully: ${outputPath}`);
        return true;
    } catch (error) {
        // More detailed error handling
        console.error(`Error rendering map: ${error.message}`);
        console.error(error.stack);
        
        // Try to inspect the style file
        try {
            console.log('Debug: Trying to check style file structure');
            const styleData = await fs.readJson(stylePath);
            console.log(`Style version: ${styleData.version}`);
            console.log(`Style has ${Object.keys(styleData.sources || {}).length} sources`);
            console.log(`Style has ${(styleData.layers || []).length} layers`);
            
            // Check critical paths
            const styleDir = path.dirname(stylePath);
            const absoluteStylePath = path.resolve(styleDir);
            console.log(`Absolute style directory: ${absoluteStylePath}`);
            
            // Check if sprite files exist
            if (styleData.sprite) {
                const spritePath = path.resolve(styleDir, 'sprites');
                const spriteJsonPath = `${spritePath}.json`;
                const spritePngPath = `${spritePath}.png`;
                console.log(`Checking sprite files: 
                  - JSON: ${spriteJsonPath} (exists: ${fs.existsSync(spriteJsonPath)})
                  - PNG: ${spritePngPath} (exists: ${fs.existsSync(spritePngPath)})`);
                
                // Check @2x versions
                const sprite2xJsonPath = `${spritePath}@2x.json`;
                const sprite2xPngPath = `${spritePath}@2x.png`;
                console.log(`Checking @2x sprite files: 
                  - JSON: ${sprite2xJsonPath} (exists: ${fs.existsSync(sprite2xJsonPath)})
                  - PNG: ${sprite2xPngPath} (exists: ${fs.existsSync(sprite2xPngPath)})`);
            }
            
            // Check if glyphs directory exists
            if (styleData.glyphs) {
                const glyphsDir = path.resolve(styleDir, 'glyphs');
                console.log(`Checking glyphs directory: ${glyphsDir} (exists: ${fs.existsSync(glyphsDir)})`);
                if (fs.existsSync(glyphsDir)) {
                    const fontDirs = fs.readdirSync(glyphsDir);
                    console.log(`Available font stacks: ${fontDirs.join(', ')}`);
                }
            }
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
