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
import logging
import math
import os
import cv2
import numpy as np

from PIL import Image, ImageDraw

from app.config import Config
from app.services.image_utils import draw_bounded_text, split_by_words, get_styled_parts, split_text_in_lines
from app.services.utils import u_num

# Configuration
FPS = 30
STATIC_DISPLAY_TIME = 3  # Seconds to display static pages
WORD_DISPLAY_TIME = 0.2

# Paths
FONT_PATH = "app/fonts/montserrat.ttf"  # Update with your font path

def render_progressive_text(format, video_writer, image_size, language, text, bold_parts):
    background_path = os.path.join(Config.TEMPLATE_FOLDER, format["FOLDER"], "quote.jpg")
    bg_image = cv2.imread(background_path)
    bg_image = cv2.resize(bg_image, image_size)

    styled_parts = get_styled_parts(text, bold_parts)

    # Find the final font size
    orientation, direction, _ = Config.get_layout(language)
    font_size = Config.MAX_FONT_SIZE
    if Config.VIDEO_USE_FINAL_FONT_SIZE:
        font_size, _, _, _ = split_text_in_lines(format, language, styled_parts)

    words = split_by_words(language, text)
    last_frame = None
    for i in range(len(words) + 1):
        separator = " " if orientation == "H" or language == "ko" else "" # FIXME: ko specifics
        current_text = separator.join(words[:i])

        logging.debug(f"Draw the text")
        pil_image = Image.fromarray(cv2.cvtColor(bg_image, cv2.COLOR_BGR2RGB))  # Convert to PIL format
        draw = ImageDraw.Draw(pil_image)
        draw_bounded_text(format, language, draw, current_text, bold_parts, font_size)

        logging.debug(f"Convert PIL image back to OpenCV format")
        cv_frame = cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)
        last_frame = cv_frame
        for _ in range(math.ceil(FPS * WORD_DISPLAY_TIME)):
            video_writer.write(cv_frame)

    if last_frame is not None: # Add extra time at the end of the page
        for _ in range(math.ceil(FPS * STATIC_DISPLAY_TIME)):
            video_writer.write(last_frame)

def display_static_page(video_writer, image_size, page_path, timeunit=1):
    static_frame = cv2.imread(page_path)
    static_frame = cv2.resize(static_frame, image_size)

    for _ in range(FPS * STATIC_DISPLAY_TIME * timeunit):
        video_writer.write(static_frame)

def generate_video(format, language, page1, page2, bold_parts, cover_link):
    format = Config.FORMATS[format]

    fourcc = cv2.VideoWriter_fourcc(*"H264")
    output_video = os.path.join(Config.OUTPUT_FOLDER, f"{u_num()}_video.mp4")

    logging.info(f"Output file: {output_video}")
    image_size = (format["IMAGE"]["WIDTH"], format["IMAGE"]["HEIGHT"])
    video_writer = cv2.VideoWriter(output_video, fourcc, FPS, image_size)

    if not video_writer.isOpened():
        raise Exception("Failed to open VideoWriter")

    # Logo frame
    display_static_page(video_writer, image_size, Config.TEMPLATE_FOLDER + "/" + format["FOLDER"] + "/logo.jpg")

    logging.info(f"Create pages with progressive text rendering")
    render_progressive_text(format, video_writer, image_size, language, page1, bold_parts)
    render_progressive_text(format, video_writer, image_size, language, page2, bold_parts)

    display_static_page(video_writer, image_size, "app/" + cover_link)

    logging.info(f"Display static pages")
    for static_page in [ "2026.jpg", "logo.jpg" ]:
        static_page = os.path.join(Config.TEMPLATE_FOLDER, format["FOLDER"], static_page)
        display_static_page(video_writer, image_size, static_page)

    logging.info(f"Save and return video")
    video_writer.release()
    return output_video.replace("app/", "", 1)


