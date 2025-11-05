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
Create Android adaptive launcher icons with lighting effects.
Generates foreground, background, and legacy icons for all densities.
"""

from PIL import Image, ImageDraw, ImageFilter, ImageEnhance
import math
import os

# Android icon densities and sizes
DENSITIES = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

# Adaptive icon sizes (foreground/background are 108dp vs 48dp for legacy)
ADAPTIVE_MULTIPLIER = 2.25  # 108dp / 48dp

def add_lighting_effects(img):
    """
    Add subtle Android Material Design lighting effects.
    """
    size = img.size[0]

    if img.mode != 'RGBA':
        img = img.convert('RGBA')

    # Top shine gradient (more subtle than iOS)
    shine = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    draw = ImageDraw.Draw(shine)

    for y in range(size // 2):
        alpha = int(35 * (1 - y / (size // 2)) ** 2)
        draw.rectangle([0, y, size, y + 1], fill=(255, 255, 255, alpha))

    img = Image.alpha_composite(img, shine)

    # Center radial glow
    radial = Image.new('RGBA', (size, size), (255, 255, 255, 0))
    center = size // 2

    for x in range(0, size, 2):
        for y in range(0, size, 2):
            distance = math.sqrt((x - center) ** 2 + (y - center) ** 2)
            if distance < center * 0.6:
                alpha = int(15 * (1 - distance / (center * 0.6)) ** 2)
                for dx in range(2):
                    for dy in range(2):
                        if x + dx < size and y + dy < size:
                            radial.putpixel((x + dx, y + dy), (255, 255, 255, alpha))

    radial = radial.filter(ImageFilter.GaussianBlur(30))
    img = Image.alpha_composite(img, radial)

    # Material Design enhancement: slightly more contrast and saturation
    img = ImageEnhance.Contrast(img).enhance(1.15)
    img = ImageEnhance.Color(img).enhance(1.1)
    img = ImageEnhance.Brightness(img).enhance(1.03)

    return img

def create_foreground(source_img, size):
    """
    Create adaptive icon foreground layer.
    The icon content should be in the center 66% (safe zone).
    """
    # Adaptive icons are 108dp, content fits in 66% circle (standard Android safe zone)
    foreground = Image.new('RGBA', (size, size), (0, 0, 0, 0))

    # Scale source to fit in safe zone (66% of size)
    safe_zone = int(size * 0.66)
    source_resized = source_img.resize((safe_zone, safe_zone), Image.LANCZOS)

    # Center the icon
    offset = (size - safe_zone) // 2
    foreground.paste(source_resized, (offset, offset), source_resized)

    return foreground

def create_background(source_img, size):
    """
    Create adaptive icon background layer.
    Use the dominant dark color from the source.
    """
    # Extract the background color from source (top-left corner area)
    sample = source_img.crop((0, 0, 50, 50))
    pixels = list(sample.getdata())

    # Get average color
    r = sum(p[0] for p in pixels) // len(pixels)
    g = sum(p[1] for p in pixels) // len(pixels)
    b = sum(p[2] for p in pixels) // len(pixels)

    # Create solid background
    background = Image.new('RGB', (size, size), (r, g, b))

    return background

def create_legacy_icon(source_img, size):
    """
    Create legacy launcher icon (pre-Oreo).
    Used by some launchers for home screen shortcuts.
    """
    # Legacy icons use 90% of available space for better visibility on home screen
    icon_size = int(size * 0.90)
    icon = source_img.resize((icon_size, icon_size), Image.LANCZOS)

    # Create with transparent background
    legacy = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    offset = (size - icon_size) // 2
    legacy.paste(icon, (offset, offset), icon)

    return legacy

def create_monochrome(source_img, size):
    """
    Create monochrome icon for themed icons (Android 13+).
    """
    # Convert to grayscale then to pure white on transparent
    gray = source_img.convert('L')
    monochrome = Image.new('RGBA', gray.size, (255, 255, 255, 0))

    for x in range(gray.size[0]):
        for y in range(gray.size[1]):
            alpha = gray.getpixel((x, y))
            if alpha > 50:  # Threshold
                monochrome.putpixel((x, y), (255, 255, 255, alpha))

    return monochrome.resize((size, size), Image.LANCZOS)

def generate_android_icons(input_path, output_base_dir):
    """
    Generate all Android launcher icons.
    """
    print(f"📱 Loading source icon from {input_path}")

    source = Image.open(input_path)
    size = min(source.size)
    source = source.crop((0, 0, size, size)).convert('RGBA')

    print(f"✨ Applying lighting effects...")
    source_enhanced = add_lighting_effects(source)

    print(f"🎨 Generating icons for all densities...")

    for density, base_size in DENSITIES.items():
        print(f"  - {density} ({base_size}dp)")

        # Adaptive icon sizes (108dp)
        adaptive_size = int(base_size * ADAPTIVE_MULTIPLIER)

        # Create output directory
        output_dir = os.path.join(output_base_dir, f'mipmap-{density}')
        os.makedirs(output_dir, exist_ok=True)

        # Generate foreground
        foreground = create_foreground(source_enhanced, adaptive_size)
        foreground.save(os.path.join(output_dir, 'ic_launcher_foreground.png'), 'PNG')

        # Generate background
        background = create_background(source, adaptive_size)
        background.save(os.path.join(output_dir, 'ic_launcher_background.png'), 'PNG')

        # Generate monochrome
        monochrome = create_monochrome(source_enhanced, adaptive_size)
        monochrome.save(os.path.join(output_dir, 'ic_launcher_monochrome.png'), 'PNG')

        # Generate legacy icon
        legacy = create_legacy_icon(source_enhanced, base_size)
        legacy.save(os.path.join(output_dir, 'ic_launcher.png'), 'PNG')

    print(f"✅ Android icons generated successfully!")
    print(f"📁 Output directory: {output_base_dir}")

if __name__ == '__main__':
    import sys

    input_path = 'misc/planet-square-1024.png'
    output_dir = 'composeApp/src/androidMain/res'

    if len(sys.argv) > 1:
        input_path = sys.argv[1]
    if len(sys.argv) > 2:
        output_dir = sys.argv[2]

    generate_android_icons(input_path, output_dir)
    print(f"\n🎉 All Android launcher icons ready!")
    print(f"\n📝 Icon types generated:")
    print(f"   - ic_launcher_foreground.png (adaptive layer)")
    print(f"   - ic_launcher_background.png (adaptive layer)")
    print(f"   - ic_launcher_monochrome.png (themed icons)")
    print(f"   - ic_launcher.png (legacy pre-Oreo)")
