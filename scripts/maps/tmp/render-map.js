const fs = require('fs-extra');
const path = require('path');
const { PNG } = require('pngjs');
const maplibre = require('@maplibre/maplibre-gl-native');

// Function to render a map
async function renderMap(options) {
    const {
        geojsonPath,
        stylePath,
        outputPath,
        width,
        height,
        center,
        zoom
    } = options;

    try {
        // Read the GeoJSON file
        const geojsonData = await fs.readJson(geojsonPath);
        
        // Read the style file
        const styleData = await fs.readJson(stylePath);
        
        // Create a map
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
                    if (url.startsWith('file://')) {
                        const filePath = url.replace('file://', '');
                        if (fs.existsSync(filePath)) {
                            const data = fs.readFileSync(filePath);
                            callback(null, { data });
                        } else {
                            callback(new Error(`File not found: ${filePath}`));
                        }
                    } else {
                        callback(new Error(`Unsupported URL: ${url}`));
                    }
                } catch (error) {
                    callback(error);
                }
            }
        });
        
        // Add GeoJSON source and layer for the event area
        map.addSource('event-area', {
            type: 'geojson',
            data: geojsonData
        });
        
        map.addLayer({
            id: 'event-area-fill',
            type: 'fill',
            source: 'event-area',
            paint: {
                'fill-color': '#D33682',
                'fill-opacity': 0.5
            }
        });
        
        map.addLayer({
            id: 'event-area-line',
            type: 'line',
            source: 'event-area',
            paint: {
                'line-color': '#D33682',
                'line-width': 2
            }
        });
        
        // Render the map
        const pixels = await new Promise((resolve, reject) => {
            map.render((err, buffer) => {
                if (err) reject(err);
                else resolve(buffer);
            });
        });
        
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
        console.error(`Error rendering map: ${error.message}`);
        return false;
    }
}

// Parse command line arguments
const args = process.argv.slice(2);
if (args.length < 7) {
    console.error('Usage: node render-map.js <geojsonPath> <stylePath> <outputPath> <width> <height> <centerLat> <centerLng> [<zoom>]');
    process.exit(1);
}

const geojsonPath = args[0];
const stylePath = args[1];
const outputPath = args[2];
const width = parseInt(args[3], 10);
const height = parseInt(args[4], 10);
const centerLat = parseFloat(args[5]);
const centerLng = parseFloat(args[6]);
const zoom = args[7] ? parseFloat(args[7]) : 10;

renderMap({
    geojsonPath,
    stylePath,
    outputPath,
    width,
    height,
    center: [centerLng, centerLat],
    zoom
}).then(success => {
    process.exit(success ? 0 : 1);
});
