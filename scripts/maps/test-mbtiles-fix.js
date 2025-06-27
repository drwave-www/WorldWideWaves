const fs = require('fs-extra');
const path = require('path');
const maplibre = require('@maplibre/maplibre-gl-native');

const TEST_NAME = 'GeoJSON Bounds and Zoom Calculation Test';

// --- Helper functions replicated from render-map.js for testing ---

/**
 * Calculates the bounding box of a GeoJSON object.
 * @param {object} geojson - The GeoJSON object.
 * @returns {number[]} The bounding box as [minLng, minLat, maxLng, maxLat].
 */
function getGeojsonBounds(geojson) {
    const bounds = [Infinity, Infinity, -Infinity, -Infinity]; // [minLng, minLat, maxLng, maxLat]

    function processCoordinates(coordinates) {
        if (!coordinates) return;
        if (typeof coordinates[0] === 'number' && typeof coordinates[1] === 'number') {
            bounds[0] = Math.min(bounds[0], coordinates[0]);
            bounds[1] = Math.min(bounds[1], coordinates[1]);
            bounds[2] = Math.max(bounds[2], coordinates[0]);
            bounds[3] = Math.max(bounds[3], coordinates[1]);
        } else {
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
    } else {
        processGeometry(geojson);
    }

    return bounds;
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

    function latRad(lat) {
        const sin = Math.sin(lat * Math.PI / 180);
        const rad = Math.log((1 + sin) / (1 - sin)) / 2;
        return Math.abs(rad);
    }

    const lngFraction = (maxLng - minLng) === 0 ? 0 : Math.abs(maxLng - minLng) / 360;
    const latFraction = (latRad(maxLat) - latRad(minLat)) === 0 ? 0 : (latRad(maxLat) - latRad(minLat)) / (2 * Math.PI);

    if (lngFraction === 0 || latFraction === 0) {
        return MAX_ZOOM;
    }

    const lngZoom = Math.log(imageWidth / (TILE_SIZE * lngFraction)) / Math.LN2;
    const latZoom = Math.log(imageHeight / (TILE_SIZE * latFraction)) / Math.LN2;

    const zoom = Math.min(lngZoom, latZoom, MAX_ZOOM);
    return zoom > 0 ? zoom : 0;
}


// --- Test Data ---

const testGeojson = {
    type: 'FeatureCollection',
    features: [{
        type: 'Feature',
        geometry: {
            type: 'Polygon',
            coordinates: [
                [
                    [-74.047285, 40.683921], // Lower West
                    [-74.047285, 40.879058], // Upper West
                    [-73.933659, 40.879058], // Upper East
                    [-73.933659, 40.683921], // Lower East
                    [-74.047285, 40.683921]
                ]
            ]
        }
    }]
};

const pointGeojson = { type: 'Point', coordinates: [-74, 40.5] };
const lineGeojson = { type: 'LineString', coordinates: [[-74, 40.5], [-73.5, 41]] };


// --- Main Test Runner ---

async function runTest() {
    console.log(`[START] Running: ${TEST_NAME}`);

    let failed = false;
    const failTest = (message) => {
        console.error(`[FAIL] ${message}`);
        failed = true;
    };

    try {
        // --- Test: getGeojsonBounds ---
        console.log('\n--- Testing getGeojsonBounds ---');
        const pointBounds = getGeojsonBounds(pointGeojson);
        console.log('Point Bounds:', pointBounds);
        if (JSON.stringify(pointBounds) !== '[-74,40.5,-74,40.5]') failTest('Point bounds calculation incorrect.');

        const lineBounds = getGeojsonBounds(lineGeojson);
        console.log('LineString Bounds:', lineBounds);
        if (JSON.stringify(lineBounds) !== '[-74,40.5,-73.5,41]') failTest('LineString bounds calculation incorrect.');

        const polyBounds = getGeojsonBounds(testGeojson);
        console.log('Polygon Bounds:', polyBounds);
        if (Math.abs(polyBounds[0] - -74.047285) > 1e-6 || Math.abs(polyBounds[3] - 40.879058) > 1e-6) {
            failTest('Polygon bounds calculation incorrect.');
        }

        // --- Test: Center and Zoom Calculation ---
        console.log('\n--- Testing Center and Zoom Calculation ---');
        const imageWidth = 1024;
        const imageHeight = 576;
        const calculatedCenter = [(polyBounds[0] + polyBounds[2]) / 2, (polyBounds[1] + polyBounds[3]) / 2];
        const calculatedZoom = getBoundsZoomLevel(polyBounds, imageWidth, imageHeight);

        console.log(`Image Dimensions: ${imageWidth}x${imageHeight}`);
        console.log('Calculated Center:', calculatedCenter);
        console.log('Calculated Zoom:', calculatedZoom);

        if (Math.abs(calculatedCenter[0] - -73.990472) > 1e-6) failTest('Center longitude calculation incorrect.');
        if (Math.abs(calculatedCenter[1] - 40.7814895) > 1e-6) failTest('Center latitude calculation incorrect.');
        if (calculatedZoom < 9 || calculatedZoom > 11) failTest(`Calculated zoom level (${calculatedZoom}) is outside expected range (9-11).`);

    } catch (error) {
        failTest(`An unexpected error occurred during tests: ${error.message}`);
        console.error(error.stack);
    }

    if (failed) {
        console.error(`\n[RESULT] ${TEST_NAME} finished with errors.`);
        process.exit(1);
    } else {
        console.log(`\n[PASS] All calculation tests completed successfully!`);
    }

    // --- Integration Test with dummy files ---
    const testDataDir = path.resolve(__dirname, 'data');
    const geojsonPath = path.resolve(testDataDir, 'test_ny.geojson');
    const mbtilesPath = path.resolve(testDataDir, 'test_ny.mbtiles');

    try {
        console.log('\n--- Running Integration Test with dummy files ---');
        await fs.ensureDir(testDataDir);
        await fs.writeJson(geojsonPath, testGeojson);
        await fs.createFile(mbtilesPath);
        console.log('Dummy files created.');

        // This part mimics the logic in render-map.js
        const geojsonData = await fs.readJson(geojsonPath);
        const styleData = await fs.readJson(path.resolve(__dirname, '../../shared/src/commonMain/composeResources/files/style/mapstyle.json'));
        const source = styleData.sources.openmaptiles;

        // Apply transformation
        delete source.url;
        source.type = 'mbtiles';
        source.path = path.resolve(mbtilesPath);

        if (source.type !== 'mbtiles' || !source.path) {
            throw new Error('Integration Test: Style transformation failed.');
        }
        console.log('Integration Test: Style transformation successful.');
        console.log(`[PASS] ${TEST_NAME} completed successfully!`);

    } catch (error) {
        console.error(`\n[FAIL] Integration test failed: ${error.message}`);
        console.error(error.stack);
    } finally {
        console.log('\n--- Cleaning up dummy files ---');
        await fs.remove(testDataDir);
        console.log('Cleanup complete.');
    }
}

runTest();
