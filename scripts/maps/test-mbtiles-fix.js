const fs = require('fs-extra');
const path = require('path');
const maplibre = require('@maplibre/maplibre-gl-native');

const TEST_NAME = 'MBTiles Style Fix Test';

async function runTest() {
    console.log(`[TEST] Starting: ${TEST_NAME}`);

    // Define paths
    const baseDir = __dirname;
    const stylePath = path.resolve(baseDir, '../../shared/src/commonMain/composeResources/files/style/mapstyle.json');
    const testDataDir = path.resolve(baseDir, 'data');
    const geojsonPath = path.resolve(testDataDir, 'new_york_usa.geojson');
    const mbtilesPath = path.resolve(testDataDir, 'new_york_usa.mbtiles');

    // 1. Create dummy files for the test
    try {
        console.log('[TEST] Creating dummy data files...');
        await fs.ensureDir(testDataDir);
        await fs.writeJson(geojsonPath, { type: 'FeatureCollection', features: [] });
        await fs.createFile(mbtilesPath); // Just need an empty file to exist
        console.log('[TEST] Dummy files created successfully.');
    } catch (error) {
        console.error(`[FAIL] Could not create dummy files: ${error.message}`);
        return;
    }

    try {
        // 2. Read and process the style file, mimicking render-map.js
        console.log('[TEST] Reading and processing style file...');
        const styleData = await fs.readJson(stylePath);
        const geojsonData = await fs.readJson(geojsonPath);

        if (!styleData.sources || !styleData.sources.openmaptiles) {
            throw new Error('Style file does not contain a source named "openmaptiles"');
        }

        const source = styleData.sources.openmaptiles;
        console.log(`[TEST] Original source 'openmaptiles' type: ${source.type}`);
        if (source.type !== 'vector') {
            console.warn(`[WARN] Expected original source type to be 'vector', but found '${source.type}'.`);
        }

        // Apply the transformation
        delete source.url;
        source.type = 'mbtiles';
        source.path = path.resolve(mbtilesPath);

        console.log(`[TEST] Transformed source 'openmaptiles' type: ${source.type}`);
        console.log(`[TEST] Transformed source 'openmaptiles' path: ${source.path}`);

        // 3. Validate the transformation
        if (source.type !== 'mbtiles' || !source.path) {
            throw new Error('Style transformation failed. Source type should be "mbtiles" and have a "path".');
        }
        console.log('[TEST] Style transformation logic validated.');

        // 4. Check if MapLibre can load the style
        console.log('[TEST] Attempting to load modified style into MapLibre...');
        const map = new maplibre.Map({
            request: (req, callback) => {
                // For this test, we only need to handle file requests for sprites/glyphs
                if (req.url.startsWith('file://')) {
                    const filePath = req.url.replace('file://', '');
                    fs.readFile(filePath, (err, data) => {
                        if (err) return callback(err);
                        return callback(null, { data });
                    });
                } else {
                    // We don't expect other requests as we won't render
                    callback(new Error(`Unsupported protocol in test: ${req.url}`));
                }
            },
        });

        map.load(styleData);
        map.release();
        console.log('[TEST] MapLibre loaded the style without errors.');

        console.log(`\n[PASS] ${TEST_NAME} completed successfully!`);

    } catch (error) {
        console.error(`\n[FAIL] ${TEST_NAME} failed: ${error.message}`);
        console.error(error.stack);
    } finally {
        // 5. Clean up dummy files
        console.log('\n[TEST] Cleaning up dummy files...');
        await fs.remove(geojsonPath);
        await fs.remove(mbtilesPath);
        console.log('[TEST] Cleanup complete.');
    }
}

runTest();
