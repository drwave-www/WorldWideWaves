#
# Copyright 2024 DrWave
#
# WorldWideWaves is an ephemeral mobile app designed to orchestrate human waves through cities and
# countries, culminating in a global wave. The project aims to transcend physical and cultural
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
#

import os
import requests
import logging
from PIL import Image, ImageDraw
from app.config import Config
from app.services.utils import u_num
from app.services.image_utils import draw_bounded_title, draw_bounded_text
from app.services.google_service import fetch_google_image

def get_cover(language, author, title):
    image_path = os.path.join(Config.TEMPLATE_FOLDER, "3.jpg")

    logging.info(f"Open template {image_path}")
    cover_template = Image.open(image_path)
    draw = ImageDraw.Draw(cover_template)

    logging.info(f"Create texts")
    draw_bounded_title(language, draw, title, Config.bold_font, y_start=120)
    draw_bounded_title(language, draw, author, Config.bold_font, y_start=900)

    logging.info(f"Search for author image on Google")
    author_image_url = ""
    try:
        author_image_url = fetch_google_image(f"portrait of {author} ({title}) -site:gettyimages.*")
    except Exception as e:
        logging.error(f"Exception while fetching author image: {e}")

    logging.info(f"Image found: {author_image_url}")
    if author_image_url:
        logging.info(f"Retrieve image raw")
        response = requests.get(author_image_url, stream=True, headers={"User-Agent": "Mozilla/5.0"})
        if response.headers["Content-Type"].startswith("image"):
            author_image = Image.open(response.raw)

            logging.info(f"Open and resize it")
            fixed_height = 560
            aspect_ratio = author_image.width / author_image.height
            new_width = int(fixed_height * aspect_ratio)
            author_image = author_image.resize((new_width, fixed_height))
            cover_template.paste(author_image, ((Config.IMAGE_SIZE - new_width) // 2, (Config.IMAGE_SIZE - fixed_height) // 2))
        else:
            logging.error("URL does not point to a valid image.")

    logging.info(f"Save cover image")
    cover_path = os.path.join(Config.OUTPUT_FOLDER, f"{u_num()}_1.jpg")
    cover_template.save(cover_path)
    return cover_path.replace("app/", "", 1)

# -----------------------------------------------------------------------------

def create_images(language, json_data):
    image_paths = []

    # 1. Page 1 and 2
    logging.info(f"Generate text pages")
    for idx, page_key in enumerate(["page1", "page2"], start=1):
        img_path = os.path.join(Config.TEMPLATE_FOLDER, f"{idx}.jpg")

        logging.info(f"Open file {img_path}")
        text_template = Image.open(img_path)
        draw = ImageDraw.Draw(text_template)

        logging.info(f"Draw the text")
        draw_bounded_text(language, draw, json_data[page_key], json_data["bold_parts"])

        logging.info(f"Save image")
        page_path = os.path.join(Config.OUTPUT_FOLDER, f"{u_num()}_{idx}.jpg")
        text_template.save(page_path)
        image_paths.append((idx, page_path))

    # 2. Cover Image
    logging.info(f"Create cover image")
    cover_path = get_cover(language, json_data['author'], json_data['title'])
    image_paths.append((3, cover_path))

    # 3. 2026 Page
    year_path = os.path.join(Config.TEMPLATE_FOLDER, "4.jpg")
    final_path = os.path.join(Config.OUTPUT_FOLDER, f"{u_num()}_4.jpg")
    Image.open(year_path).save(final_path)
    image_paths.append((4, final_path))

    # 4. Logo Page
    logo_path = os.path.join(Config.TEMPLATE_FOLDER, "5.jpg")
    final_path = os.path.join(Config.OUTPUT_FOLDER, f"{u_num()}_5.jpg")
    Image.open(logo_path).save(final_path)
    image_paths.append((5, final_path))

    return [(idx, path.replace("app/", "", 1)) for idx, path in image_paths]