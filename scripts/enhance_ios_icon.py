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
Enhance iOS app icon with rounded borders and lighting effects.
Creates an iOS-style icon with squircle shape and subtle shine.
"""

from PIL import Image, ImageDraw, ImageFilter, ImageEnhance
import math

def create_squircle_mask(size, corner_radius_ratio=0.225):
    """
    Create a squircle (superellipse) mask for iOS-style rounded corners.
    iOS uses a specific curve that's smoother than a simple rounded rectangle.
    """
    mask = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(mask)

    # Use a high-quality approximation of iOS squircle
    # Draw multiple layers of increasingly small rounded rectangles
    for i in range(100):
        ratio = i / 100.0
        current_radius = int(size * corner_radius_ratio * (1 - ratio * 0.3))
        offset = int(ratio * size * 0.02)
        alpha = int(255 * (1 - ratio))

        draw.rounded_rectangle(
            [offset, offset, size - offset, size - offset],
            radius=current_radius,
            fill=alpha
        )

    # Final solid center
    final_radius = int(size * corner_radius_ratio * 0.7)
    draw.rounded_rectangle(
        [size//10, size//10, size - size//10, size - size//10],
        radius=final_radius,
        fill=255
    )

    return mask.filter(ImageFilter.GaussianBlur(2))

def add_lighting_effects(img):
    """
    Add subtle iOS-style lighting effects:
    - Top shine/highlight
    - Subtle vignette
    - Enhanced contrast
    """
    size = img.size[0]

    # Create a subtle top-to-bottom light gradient
    gradient = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    draw = ImageDraw.Draw(gradient)

    # Top shine (subtle white gradient from top)
    for y in range(size // 3):
        alpha = int(50 * (1 - y / (size // 3)) ** 2)  # Quadratic falloff
        draw.rectangle([0, y, size, y + 1], fill=(255, 255, 255, alpha))

    # Apply gradient overlay
    img = Image.alpha_composite(img, gradient)

    # Subtle vignette (darker edges)
    vignette = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(vignette)

    center = size // 2
    max_distance = math.sqrt(2) * center

    for x in range(size):
        for y in range(size):
            distance = math.sqrt((x - center) ** 2 + (y - center) ** 2)
            if distance > center * 0.6:
                alpha = int(30 * ((distance - center * 0.6) / (max_distance - center * 0.6)))
                vignette.putpixel((x, y), (0, 0, 0, min(alpha, 40)))

    vignette = vignette.filter(ImageFilter.GaussianBlur(20))
    img = Image.alpha_composite(img, vignette)

    # Enhance contrast slightly
    enhancer = ImageEnhance.Contrast(img)
    img = enhancer.enhance(1.1)

    # Enhance color saturation slightly
    enhancer = ImageEnhance.Color(img)
    img = enhancer.enhance(1.05)

    return img

def enhance_icon(input_path, output_path):
    """
    Main function to enhance the icon with iOS styling.
    """
    print(f"📱 Loading icon from {input_path}")

    # Load the original image
    img = Image.open(input_path)

    # Ensure square and RGBA
    size = min(img.size)
    img = img.crop((0, 0, size, size))
    img = img.convert('RGBA')

    print(f"✨ Applying lighting effects...")
    # Apply lighting effects
    img = add_lighting_effects(img)

    print(f"🎨 Creating iOS-style rounded corners...")
    # Create squircle mask
    mask = create_squircle_mask(size)

    # Apply mask to create rounded corners
    output = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    output.paste(img, (0, 0))
    output.putalpha(mask)

    # Add subtle inner shadow for depth
    shadow = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow)
    inner_radius = int(size * 0.225)

    # Draw subtle inner border shadow
    for i in range(5):
        offset = i * 2
        alpha = 15 - i * 3
        shadow_draw.rounded_rectangle(
            [offset, offset, size - offset, size - offset],
            radius=inner_radius - offset,
            outline=(0, 0, 0, alpha),
            width=2
        )

    shadow = shadow.filter(ImageFilter.GaussianBlur(3))
    shadow.putalpha(ImageEnhance.Brightness(shadow.split()[3]).enhance(0.5))
    output = Image.alpha_composite(output, shadow)

    print(f"💾 Saving enhanced icon to {output_path}")
    output.save(output_path, 'PNG')
    print(f"✅ Icon enhanced successfully!")

    return output

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
