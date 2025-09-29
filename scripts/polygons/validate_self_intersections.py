# Copyright 2025 DrWave
#
# WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
# countries. The project aims to transcend physical and cultural
# boundaries, fostering unity, community, and shared human experience by leveraging real-time
# coordination and location-based services.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from __future__ import annotations

import argparse
import json
import math
import sys
import time
from pathlib import Path

EPS = 1e-12

def orient(a, b, c):
    return (b[0]-a[0])*(c[1]-a[1]) - (b[1]-a[1])*(c[0]-a[0])

def on_seg(a, b, p):
    if abs(orient(a, b, p)) > EPS:
        return False
    return (min(a[0], b[0]) - EPS <= p[0] <= max(a[0], b[0]) + EPS and min(a[1], b[1]) - EPS <= p[1] <= max(a[1], b[1]) + EPS)

def seg_intersect(a, b, c, d):
    o1 = orient(a, b, c)
    o2 = orient(a, b, d)
    o3 = orient(c, d, a)
    o4 = orient(c, d, b)
    if ((o1 > 0) != (o2 > 0)) and ((o3 > 0) != (o4 > 0)):
        return True
    if (abs(o1) <= EPS and on_seg(a, b, c)):
        return True
    if (abs(o2) <= EPS and on_seg(a, b, d)):
        return True
    if (abs(o3) <= EPS and on_seg(c, d, a)):
        return True
    if (abs(o4) <= EPS and on_seg(c, d, b)):
        return True
    return False

def deep_self_intersections(ring):
    if ring[0] != ring[-1]:
        ring = ring + [ring[0]]
    n = len(ring)
    m = n - 1
    if m <= 3:
        return [], 0.0, 0
    edges = []
    xs, ys = [], []
    for i in range(m):
        a = (float(ring[i][0]), float(ring[i][1]))
        b = (float(ring[i+1][0]), float(ring[i+1][1]))
        edges.append((a, b))
        xs += [a[0], b[0]]
        ys += [a[1], b[1]]
    minx, maxx = min(xs), max(xs)
    miny, maxy = min(ys), max(ys)
    spanx = max(maxx - minx, 1e-9)
    spany = max(maxy - miny, 1e-9)
    gdim = max(16, min(256, int(math.sqrt(m))))
    cell_w = spanx / gdim
    cell_h = spany / gdim
    grid = {}
    def cell_indices_for_edge(a, b):
        x0 = min(a[0], b[0]); x1 = max(a[0], b[0])
        y0 = min(a[1], b[1]); y1 = max(a[1], b[1])
        ix0 = int((x0 - minx) / cell_w)
        ix1 = int((x1 - minx) / cell_w)
        iy0 = int((y0 - miny) / cell_h)
        iy1 = int((y1 - miny) / cell_h)
        ix0 = max(0, min(gdim - 1, ix0))
        ix1 = max(0, min(gdim - 1, ix1))
        iy0 = max(0, min(gdim - 1, iy0))
        iy1 = max(0, min(gdim - 1, iy1))
        for ix in range(ix0, ix1 + 1):
            for iy in range(iy0, iy1 + 1):
                yield (ix, iy)
    for i, (a, b) in enumerate(edges):
        for key in cell_indices_for_edge(a, b):
            grid.setdefault(key, []).append(i)
    seen_pairs = set()
    intersections = []
    start = time.time()
    for key, idxs in grid.items():
        L = len(idxs)
        if L < 2:
            continue
        for ii in range(L):
            i = idxs[ii]
            for jj in range(ii + 1, L):
                j = idxs[jj]
                if j == i or j == i + 1 or i == j + 1 or (i == 0 and j == m - 1):
                    continue
                a, b = edges[i]
                c, d = edges[j]
                keypair = (i, j) if i < j else (j, i)
                if keypair in seen_pairs:
                    continue
                seen_pairs.add(keypair)
                if seg_intersect(a, b, c, d):
                    intersections.append((i, j, a, b, c, d))
    dur = time.time() - start
    return intersections, dur, len(seen_pairs)

def resolve_geojson_path(event: str, maps_root: str, base: Path) -> Path:
    return base / maps_root / event / "src/main/assets" / f"{event}.geojson"

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("-e", "--event", required=True)
    ap.add_argument("--maps-root", default="maps/android")
    ap.add_argument("--limit", type=int, default=20)
    args = ap.parse_args()
    script_dir = Path(__file__).resolve().parent
    repo_root = script_dir.parent.parent
    gj_path = resolve_geojson_path(args.event, args.maps_root, repo_root)
    if not gj_path.exists():
        print(f"GeoJSON not found: {gj_path}", file=sys.stderr)
        sys.exit(2)
    data = json.loads(gj_path.read_text())
    features = data.get("features", [])
    if not features:
        print("No features found")
        sys.exit(0)
    geom = features[0].get("geometry", {})
    gtype = geom.get("type")
    if gtype == "Polygon":
        polys = [geom.get("coordinates", [])]
    elif gtype == "MultiPolygon":
        polys = geom.get("coordinates", [])
    else:
        print(f"Unsupported geometry type: {gtype}")
        sys.exit(2)
    any_intersections = False
    for pi, poly in enumerate(polys):
        for ri, ring in enumerate(poly):
            n = len(ring)
            print(f"Analyzing polygon {pi} ring {ri} with {n} points...")
            inters, dur, tested = deep_self_intersections(ring)
            print(f" - candidate pairs tested: {tested}, time: {dur:.2f}s")
            if inters:
                any_intersections = True
                print(f" - self-intersections found: {len(inters)} (showing first {args.limit})")
                for k, (i, j, a, b, c, d) in enumerate(inters[: args.limit]):
                    print(f"   * edges {i}-{i+1} and {j}-{j+1}")
                    print(f"     ({a})-({b}) X ({c})-({d})")
            else:
                print(" - no self-intersections found")
    sys.exit(1 if any_intersections else 0)

if __name__ == "__main__":
    main()
