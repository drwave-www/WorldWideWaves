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

#!/usr/bin/env python3
"""
Enhance iOS app icon with rounded corners and lighting effects.
Creates iOS squircle shape with the design filling to the edges.
"""

from PIL import Image, ImageDraw, ImageFilter, ImageEnhance
import math

def create_ios_squircle_mask(size):
    """
    Create iOS-style squircle (superellipse) mask.
    Uses the actual iOS corner radius formula.
    """
    mask = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(mask)

    # iOS uses approximately 22.5% corner radius
    corner_radius = int(size * 0.225)

    # Draw the rounded rectangle
    draw.rounded_rectangle(
        [0, 0, size, size],
        radius=corner_radius,
        fill=255
    )

    # Apply slight blur for anti-aliasing
    mask = mask.filter(ImageFilter.GaussianBlur(1))

    return mask

def add_lighting_effects(img):
    """
    Add subtle lighting effects: top shine, center glow, enhanced colors.
    """
    size = img.size[0]

    if img.mode != 'RGBA':
        img = img.convert('RGBA')

    # Top shine gradient
    shine = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    draw = ImageDraw.Draw(shine)

    for y in range(size // 2):
        alpha = int(40 * (1 - y / (size // 2)) ** 1.8)
        draw.rectangle([0, y, size, y + 1], fill=(255, 255, 255, alpha))

    img = Image.alpha_composite(img, shine)

    # Center radial glow
    radial = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    center = size // 2

    for x in range(0, size, 2):
        for y in range(0, size, 2):
            distance = math.sqrt((x - center) ** 2 + (y - center) ** 2)
            if distance < center * 0.5:
                alpha = int(18 * (1 - distance / (center * 0.5)) ** 2)
                for dx in range(2):
                    for dy in range(2):
                        if x + dx < size and y + dy < size:
                            radial.putpixel((x + dx, y + dy), (255, 255, 255, alpha))

    radial = radial.filter(ImageFilter.GaussianBlur(35))
    img = Image.alpha_composite(img, radial)

    # Enhance visual quality
    img = ImageEnhance.Contrast(img).enhance(1.18)
    img = ImageEnhance.Color(img).enhance(1.12)
    img = ImageEnhance.Brightness(img).enhance(1.05)

    return img

def enhance_icon(input_path, output_path):
    """
    Enhance icon with iOS squircle shape and lighting.
    """
    print(f"📱 Loading icon from {input_path}")

    img = Image.open(input_path)
    size = min(img.size)
    img = img.crop((0, 0, size, size)).convert('RGBA')

    print(f"✨ Applying lighting effects...")
    img = add_lighting_effects(img)

    print(f"🎨 Creating iOS squircle shape...")
    mask = create_ios_squircle_mask(size)

    # Apply mask for rounded corners
    img.putalpha(mask)

    print(f"💾 Saving to {output_path}")
    img.save(output_path, 'PNG')
    print(f"✅ Done!")

    return img

if __name__ == '__main__':
    import sys

    input_path = 'misc/planet-square-1024.png'
    output_path = 'misc/planet-square-1024-enhanced.png'

    if len(sys.argv) > 1:
        input_path = sys.argv[1]
    if len(sys.argv) > 2:
        output_path = sys.argv[2]

    enhance_icon(input_path, output_path)
    print(f"\n🎉 Enhanced icon ready: {output_path}")
