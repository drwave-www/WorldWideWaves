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
Enhance iOS app icon with lighting effects.
Adds subtle shine and depth while keeping full square format.
iOS applies rounded corners automatically.
"""

from PIL import Image, ImageDraw, ImageFilter, ImageEnhance
import math

def add_lighting_effects(img):
    """
    Add subtle iOS-style lighting effects:
    - Top shine/highlight
    - Subtle radial glow from center
    - Enhanced contrast and vibrancy
    """
    size = img.size[0]

    # Ensure RGBA mode
    if img.mode != 'RGBA':
        img = img.convert('RGBA')

    # Create top shine gradient (subtle white from top)
    shine = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    draw = ImageDraw.Draw(shine)

    # Gentle top-to-middle gradient
    for y in range(size // 2):
        # Softer quadratic falloff
        alpha = int(35 * (1 - y / (size // 2)) ** 1.5)
        draw.rectangle([0, y, size, y + 1], fill=(255, 255, 255, alpha))

    img = Image.alpha_composite(img, shine)

    # Add subtle radial highlight from center
    radial = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    center = size // 2
    max_distance = math.sqrt(2) * center

    for x in range(0, size, 2):  # Step by 2 for performance
        for y in range(0, size, 2):
            distance = math.sqrt((x - center) ** 2 + (y - center) ** 2)
            # Subtle glow in center area
            if distance < center * 0.6:
                alpha = int(15 * (1 - distance / (center * 0.6)) ** 2)
                radial.putpixel((x, y), (255, 255, 255, alpha))
                if x + 1 < size:
                    radial.putpixel((x + 1, y), (255, 255, 255, alpha))
                if y + 1 < size:
                    radial.putpixel((x, y + 1), (255, 255, 255, alpha))
                if x + 1 < size and y + 1 < size:
                    radial.putpixel((x + 1, y + 1), (255, 255, 255, alpha))

    radial = radial.filter(ImageFilter.GaussianBlur(30))
    img = Image.alpha_composite(img, radial)

    # Very subtle edge darkening for depth
    vignette = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(vignette)

    # Only darken the very edges
    edge_size = size // 20
    for i in range(edge_size):
        alpha = int(8 * (edge_size - i) / edge_size)
        # Top/bottom edges
        draw.rectangle([0, i, size, i + 1], fill=(0, 0, 0, alpha))
        draw.rectangle([0, size - i - 1, size, size - i], fill=(0, 0, 0, alpha))
        # Left/right edges
        draw.rectangle([i, 0, i + 1, size], fill=(0, 0, 0, alpha))
        draw.rectangle([size - i - 1, 0, size - i, size], fill=(0, 0, 0, alpha))

    vignette = vignette.filter(ImageFilter.GaussianBlur(15))
    img = Image.alpha_composite(img, vignette)

    # Enhance contrast moderately
    enhancer = ImageEnhance.Contrast(img)
    img = enhancer.enhance(1.15)

    # Enhance color saturation
    enhancer = ImageEnhance.Color(img)
    img = enhancer.enhance(1.1)

    # Slight brightness boost
    enhancer = ImageEnhance.Brightness(img)
    img = enhancer.enhance(1.05)

    return img

def enhance_icon(input_path, output_path):
    """
    Main function to enhance the icon with iOS styling.
    Keeps full square format - iOS applies rounded corners.
    """
    print(f"📱 Loading icon from {input_path}")

    # Load the original image
    img = Image.open(input_path)

    # Ensure square
    size = min(img.size)
    img = img.crop((0, 0, size, size))
    img = img.convert('RGBA')

    print(f"✨ Applying lighting effects...")
    img = add_lighting_effects(img)

    print(f"💾 Saving enhanced icon to {output_path}")
    img.save(output_path, 'PNG')
    print(f"✅ Icon enhanced successfully!")

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
    print(f"\n🎉 Enhanced icon ready at: {output_path}")
