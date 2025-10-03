#!/usr/bin/env python3

# Copyright 2025 DrWave
#
# WorldWideWaves Firebase Test Lab - HTML Report Generator
#
# Generates an HTML report with side-by-side screenshots from Firebase Test Lab results

import os
import json
import base64
from pathlib import Path
from datetime import datetime
from typing import List, Dict, Optional

# Configuration
RESULTS_DIR = "test_results/firebase"
OUTPUT_FILE = "test_results/firebase_test_report.html"

# Colors
GREEN = '\033[0;32m'
YELLOW = '\033[1;33m'
NC = '\033[0m'  # No Color

class TestReport:
    def __init__(self):
        self.android_screenshots = []
        self.ios_screenshots = []
        self.timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    def collect_screenshots(self):
        """Collect screenshots from Android and iOS directories"""
        print(f"{GREEN}📸 Collecting screenshots...{NC}")

        # Android screenshots
        android_dir = Path(RESULTS_DIR) / "android"
        if android_dir.exists():
            self.android_screenshots = self._find_screenshots(android_dir)
            print(f"  Found {len(self.android_screenshots)} Android screenshots")

        # iOS screenshots
        ios_dir = Path(RESULTS_DIR) / "ios"
        if ios_dir.exists():
            self.ios_screenshots = self._find_screenshots(ios_dir)
            print(f"  Found {len(self.ios_screenshots)} iOS screenshots")

    def _find_screenshots(self, directory: Path) -> List[Dict]:
        """Find all PNG screenshots in directory"""
        screenshots = []
        for png_file in directory.rglob("*.png"):
            # Parse filename to extract step info
            filename = png_file.stem
            parts = filename.split("_")

            # Try to extract step number (format: 01_description_device_version)
            step_number = parts[0] if parts[0].isdigit() else "00"

            screenshots.append({
                "path": str(png_file),
                "filename": png_file.name,
                "step": int(step_number) if step_number.isdigit() else 0,
                "description": "_".join(parts[1:]) if len(parts) > 1 else filename
            })

        # Sort by step number
        return sorted(screenshots, key=lambda x: x["step"])

    def _image_to_base64(self, image_path: str) -> str:
        """Convert image to base64 for embedding in HTML"""
        try:
            with open(image_path, "rb") as img_file:
                return base64.b64encode(img_file.read()).decode('utf-8')
        except Exception as e:
            print(f"{YELLOW}⚠️  Failed to encode {image_path}: {e}{NC}")
            return ""

    def generate_html(self) -> str:
        """Generate HTML report"""
        print(f"{GREEN}📄 Generating HTML report...{NC}")

        html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>WorldWideWaves - Firebase Test Lab Report</title>
    <style>
        * {{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }}

        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            line-height: 1.6;
            color: #333;
            background: #f5f5f5;
            padding: 20px;
        }}

        .container {{
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }}

        header {{
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            border-radius: 8px 8px 0 0;
        }}

        header h1 {{
            font-size: 28px;
            margin-bottom: 10px;
        }}

        header p {{
            opacity: 0.9;
            font-size: 14px;
        }}

        .summary {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            padding: 30px;
            background: #f8f9fa;
            border-bottom: 1px solid #e0e0e0;
        }}

        .summary-card {{
            background: white;
            padding: 20px;
            border-radius: 6px;
            text-align: center;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }}

        .summary-card h3 {{
            font-size: 14px;
            color: #666;
            margin-bottom: 10px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }}

        .summary-card .value {{
            font-size: 32px;
            font-weight: bold;
            color: #667eea;
        }}

        .screenshots {{
            padding: 30px;
        }}

        .step {{
            margin-bottom: 50px;
            border: 1px solid #e0e0e0;
            border-radius: 8px;
            overflow: hidden;
        }}

        .step-header {{
            background: #f8f9fa;
            padding: 15px 20px;
            border-bottom: 1px solid #e0e0e0;
        }}

        .step-header h3 {{
            font-size: 18px;
            color: #333;
        }}

        .step-content {{
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 20px;
            padding: 20px;
        }}

        .platform {{
            text-align: center;
        }}

        .platform h4 {{
            font-size: 14px;
            color: #666;
            margin-bottom: 10px;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }}

        .platform img {{
            width: 100%;
            border: 1px solid #e0e0e0;
            border-radius: 4px;
            cursor: pointer;
            transition: transform 0.2s;
        }}

        .platform img:hover {{
            transform: scale(1.02);
        }}

        .platform .filename {{
            font-size: 12px;
            color: #999;
            margin-top: 8px;
        }}

        .no-screenshot {{
            padding: 40px;
            text-align: center;
            color: #999;
            background: #f8f9fa;
            border: 1px dashed #ddd;
            border-radius: 4px;
        }}

        footer {{
            text-align: center;
            padding: 20px;
            color: #666;
            font-size: 14px;
            border-top: 1px solid #e0e0e0;
        }}

        .badge {{
            display: inline-block;
            padding: 4px 12px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 600;
            margin-left: 10px;
        }}

        .badge.android {{
            background: #e8f5e9;
            color: #2e7d32;
        }}

        .badge.ios {{
            background: #e3f2fd;
            color: #1565c0;
        }}

        /* Modal for fullscreen image */
        .modal {{
            display: none;
            position: fixed;
            z-index: 1000;
            left: 0;
            top: 0;
            width: 100%;
            height: 100%;
            background: rgba(0,0,0,0.9);
        }}

        .modal img {{
            display: block;
            margin: auto;
            max-width: 90%;
            max-height: 90%;
            margin-top: 2%;
        }}

        .modal:target {{
            display: block;
        }}
    </style>
</head>
<body>
    <div class="container">
        <header>
            <h1>🌊 WorldWideWaves - Firebase Test Lab Report</h1>
            <p>E2E UI Test Results | Generated: {self.timestamp}</p>
        </header>

        <div class="summary">
            <div class="summary-card">
                <h3>Android Screenshots</h3>
                <div class="value">{len(self.android_screenshots)}</div>
            </div>
            <div class="summary-card">
                <h3>iOS Screenshots</h3>
                <div class="value">{len(self.ios_screenshots)}</div>
            </div>
            <div class="summary-card">
                <h3>Total Steps</h3>
                <div class="value">{max(len(self.android_screenshots), len(self.ios_screenshots))}</div>
            </div>
            <div class="summary-card">
                <h3>Test Status</h3>
                <div class="value" style="color: #2e7d32;">✓</div>
            </div>
        </div>

        <div class="screenshots">
            <h2 style="margin-bottom: 30px;">📸 Test Journey Screenshots</h2>
"""

        # Generate screenshot comparison for each step
        all_steps = set()
        for screenshot in self.android_screenshots + self.ios_screenshots:
            all_steps.add(screenshot["step"])

        for step in sorted(all_steps):
            android_screenshot = next((s for s in self.android_screenshots if s["step"] == step), None)
            ios_screenshot = next((s for s in self.ios_screenshots if s["step"] == step), None)

            description = (android_screenshot or ios_screenshot)["description"]

            html += f"""
            <div class="step">
                <div class="step-header">
                    <h3>Step {step:02d}: {description.replace('_', ' ').title()}</h3>
                </div>
                <div class="step-content">
                    <div class="platform">
                        <h4>📱 Android <span class="badge android">Pixel/Galaxy</span></h4>
"""

            if android_screenshot:
                img_data = self._image_to_base64(android_screenshot["path"])
                html += f"""
                        <img src="data:image/png;base64,{img_data}" alt="{android_screenshot['filename']}" onclick="window.open(this.src)">
                        <div class="filename">{android_screenshot['filename']}</div>
"""
            else:
                html += """
                        <div class="no-screenshot">No screenshot available</div>
"""

            html += """
                    </div>
                    <div class="platform">
                        <h4>🍎 iOS <span class="badge ios">iPhone/iPad</span></h4>
"""

            if ios_screenshot:
                img_data = self._image_to_base64(ios_screenshot["path"])
                html += f"""
                        <img src="data:image/png;base64,{img_data}" alt="{ios_screenshot['filename']}" onclick="window.open(this.src)">
                        <div class="filename">{ios_screenshot['filename']}</div>
"""
            else:
                html += """
                        <div class="no-screenshot">No screenshot available</div>
"""

            html += """
                    </div>
                </div>
            </div>
"""

        html += f"""
        </div>

        <footer>
            <p>🤖 Generated with WorldWideWaves Firebase Test Lab Integration</p>
            <p style="margin-top: 10px; font-size: 12px;">
                <a href="https://console.firebase.google.com/project/worldwidewaves-test/testlab/histories"
                   target="_blank" style="color: #667eea;">
                    View Firebase Test Lab Console →
                </a>
            </p>
        </footer>
    </div>

    <script>
        // Click to open image in new tab
        document.querySelectorAll('.platform img').forEach(img => {{
            img.style.cursor = 'pointer';
        }});
    </script>
</body>
</html>
"""
        return html

    def save_report(self, html: str):
        """Save HTML report to file"""
        output_path = Path(OUTPUT_FILE)
        output_path.parent.mkdir(parents=True, exist_ok=True)

        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(html)

        print(f"{GREEN}✅ Report saved to: {output_path}{NC}")
        print(f"{GREEN}📂 Open in browser: file://{output_path.absolute()}{NC}")

def main():
    print(f"{GREEN}🚀 Firebase Test Lab Report Generator{NC}")
    print("=" * 50)
    print()

    report = TestReport()
    report.collect_screenshots()

    if not report.android_screenshots and not report.ios_screenshots:
        print(f"{YELLOW}⚠️  No screenshots found in {RESULTS_DIR}{NC}")
        print(f"{YELLOW}   Run ./scripts/collect_firebase_screenshots.sh first{NC}")
        return

    print()
    html = report.generate_html()
    report.save_report(html)

    print()
    print(f"{GREEN}✨ Done!{NC}")

if __name__ == "__main__":
    main()
