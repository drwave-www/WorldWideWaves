#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import base64
import urllib.request
from openai import OpenAI
import time
from pathlib import Path

client = OpenAI()  # uses OPENAI_API_KEY

# Output directory
output_dir = Path("generated_images")
output_dir.mkdir(exist_ok=True)

# List of city suffixes (drawable-style)
cities = [
    "beijing_china", "berlin_germany", "bogota_colombia", "buenos_aires_argentina", "cairo_egypt",
    "chicago_usa", "delhi_india", "dubai_united_arab_emirates", "hong_kong_china", "istanbul_turkey",
    "jakarta_indonesia", "johannesburg_south_africa", "kinshasa_democratic_republic_of_the_congo",
    "lagos_nigeria", "lima_peru", "london_england", "los_angeles_usa", "madrid_spain",
    "manila_philippines", "melbourne_australia", "mexico_city_mexico", "moscow_russia", "mumbai_india",
    "nairobi_kenya", "new_york_usa", "paris_france", "rome_italy", "san_francisco_usa", "santiago_chile",
    "sao_paulo_brazil", "seoul_south_korea", "shanghai_china", "sydney_australia", "tehran_iran",
    "tokyo_japan", "toronto_canada", "vancouver_canada"
]

# Template prompt
def build_prompt(city):
    return (
        f"""
Create a 1024×1024 realistic-cartoon illustration.
Subject: a peaceful city-scale human wave (stadium “ola”) rippling through {city.replace('_', ', ')}. Thousands of joyful, diverse people raise and lower arms in sequence along main avenues, bridges, and plazas. The scene celebrates unity, empathy, and shared joy. No impression of dystopic regime, people do not raise one arm only for instance. No flags.
View: high, wide, readable bird’s-eye that clearly shows the wave’s path and recognizable landmarks of {city.replace('_', ', ')} without copying any copyrighted designs.
Style: fine cartoon realism, clean lines, vivid colors, gentle gradients, soft global lighting, subtle depth cues, lively but not chaotic. Faces simplified, no celebrities. Clothing varied and modern. No legible text or logos.
Environment: add local topography and textures (waterfronts, parks, hills, skylines) matching the city’s character. Natural elements feel welcoming.
Mood: optimistic, inclusive, festive, non-political, non-commercial.

Variations across renders (to avoid sameness):
- Vantage: aerial wide shot / riverside angle / hilltop panorama.
- Time: golden hour / daytime / blue hour with city lights.
- Weather: clear sky / light clouds / gentle haze.
- Composition: curve the wave along different axes; alternate foreground focus (crowd close-up vs. sweeping cityscape).
- Framing: keep the wave readable at city scale; ensure iconic sites remain identifiable; avoid tight cropping.

Safety and coherence:
- No violence, no emergency imagery.
- No national flags dominating the frame.
- Avoid legible signage; if unavoidable, keep it generic.

Deliver: one square image, 1024×1024, matching all instructions above.
        """
    )

# Generate and save image
def generate_image(city):
    filename = f"e_location_{city}.png"
    output_path = output_dir / filename

    print(f"Generating: {filename}")

    prompt = build_prompt(city)

    try:
        response = client.images.generate(
            model=os.getenv("OPENAI_IMAGE_MODEL", "dall-e-3"),
            prompt=prompt,
            size="1024x1024",
            quality="hd",
            n=1,
        )
        item = response.data[0]
        if getattr(item, "b64_json", None):
            image_data = base64.b64decode(item.b64_json)
        elif getattr(item, "url", None):
            image_data = urllib.request.urlopen(item.url).read()
        else:
            raise RuntimeError("No image payload returned")

        with open(output_path, "wb") as f:
            f.write(image_data)

        print(f"✅ Saved: {output_path}")

    except Exception as e:
        print(f"❌ Error generating {city}: {e}")

# Main execution
for city in cities:
    generate_image(city)
    time.sleep(1.5)  # Be nice to the API

print("🎉 All images generated.")

