import os
import openai
import time
from pathlib import Path

openai.api_key = os.getenv("OPENAI_API_KEY") 

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
        f"A large joyful crowd forming a synchronized human wave in a famous public location of "
        f"{city.replace('_', ' ')}, daylight, vivid colors, no text, no city name visible, "
        "celebrating unity and connection."
    )

# Generate and save image
def generate_image(city):
    filename = f"e_location_{city}.png"
    output_path = output_dir / filename

    print(f"Generating: {filename}")

    prompt = build_prompt(city)

    try:
        response = openai.images.generate(
            model="dall-e-3",
            prompt=prompt,
            size="1024x1024",
            quality="standard",
            n=1,
        )

        image_url = response.data[0].url
        image_data = openai._httpx_client.get(image_url).content

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

