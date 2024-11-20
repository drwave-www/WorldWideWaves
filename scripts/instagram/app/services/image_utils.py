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
import logging
import re
from app.config import Config
from PIL import ImageFont
from app.services.utils import u_num

def draw_bounded_title(draw, text, font, y_start):
    # Split text into lines based on max width
    words = text.split()
    lines = []
    current_line = ""
    line_height = font.getbbox("Ay")[3] + 10

    for word in words:
        test_line = f"{current_line} {word}".strip()
        line_width = font.getbbox(test_line)[2] - font.getbbox(test_line)[0]

        if line_width <= Config.TEXT_RECT_SIZE_W:
            current_line = test_line
        else:
            lines.append(current_line)
            current_line = word

    if current_line:
        lines.append(current_line)

    # Draw each line, centered horizontally
    for i, line in enumerate(lines):
        bbox = font.getbbox(line)
        line_width = bbox[2] - bbox[0]
        x = (Config.IMAGE_SIZE - line_width) // 2  # Center horizontally
        y = y_start + i * line_height      # Increment vertically for each line
        draw.text((x, y), line, fill="white", font=font)

def draw_bounded_text(draw, idx, text, bold_parts):
    rect_x = (Config.IMAGE_SIZE - Config.TEXT_RECT_SIZE_W) // 2

    # Load the text and fonts
    font_size = Config.MAX_FONT_SIZE
    font = ImageFont.truetype(Config.FONT_NORMAL, size=font_size)

    # Split text into parts, tagging bold sections
    styled_parts = []
    remaining_text = text
    for bold_part in bold_parts:
        if bold_part in remaining_text:
            # Split around the bold part
            before, bold, remaining_text = remaining_text.partition(bold_part)
            if before.strip():
                styled_parts.append((before.strip(), Config.FONT_NORMAL))
            if bold.strip():
                styled_parts.append((bold.strip(), Config.FONT_BOLD))
    # Add any remaining text
    if remaining_text.strip():
        styled_parts.append((remaining_text.strip(), Config.FONT_NORMAL))

    # Adjust font size to fit within the rectangle
    total_height = 0
    lines = []
    while font_size >= Config.MIN_FONT_SIZE:
        # Simulate the layout with current font size
        lines, total_height = layout_text(font_size, styled_parts)

        # Check if the total height fits within the rectangle
        if total_height <= Config.TEXT_RECT_SIZE_H:
            break

        # Reduce the font size
        logging.info(f"Reduce the font size from {font_size} as total height is {total_height} vs {Config.TEXT_RECT_SIZE_H}")
        font_size -= 1

    # If text doesn't fit, raise an error
    if font_size < Config.MIN_FONT_SIZE:
        raise ValueError("The text is too large to fit within the bounds.")

    # Calculate starting position to center the text within the rectangle
    y = ((Config.IMAGE_SIZE - total_height) // 2) - font.getbbox("Ay")[3] // 2

    # Draw each line of justified text
    for i, line in enumerate(lines):
        x = rect_x
        justify_line(draw, line, x, y, is_last_line=(i == len(lines) - 1))
        y += font.getbbox("Ay")[3]  # Line height

    # Save the image
    page_path = os.path.join(Config.OUTPUT_FOLDER, f"{u_num()}_{idx}.jpg")
    return page_path

def layout_text(font_size, styled_parts):
    lines = []
    current_line = []
    current_width = 0
    total_height = 0

    current_font = ImageFont.truetype(Config.FONT_NORMAL, size=font_size)
    for part, current_font in styled_parts:
        current_font = ImageFont.truetype(current_font, size=font_size)
        space_width = current_font.getbbox(" ")[2]

        # Tokenize words and handle punctuation
        words = re.findall(r"[^\s,]+|[,.]", part)  # Split into words and punctuation
        for word in words:
            if word == ",":
                current_width +=  current_font.getbbox(",")[2]
                current_line[-1][0] += ","  # Append comma to the previous word
                continue
            elif word == ".":
                current_width +=  current_font.getbbox(".")[2]
                current_line[-1][0] += "."  # Append period to the previous word
                continue
            else:
                word_width = current_font.getbbox(word)[2]

            if current_width + word_width > Config.TEXT_RECT_SIZE_W:
                # Finish the current line
                lines.append(current_line)
                total_height += current_font.getbbox("Ay")[3]
                current_line = []
                current_width = 0

            current_line.append([word, current_font])
            current_width += word_width + space_width

    if current_line:
        lines.append(current_line)
        total_height += current_font.getbbox("Ay")[3]

    return lines, total_height


def justify_line(draw, line, x, y, is_last_line=False):
    total_width = sum((font.getbbox(word)[2] for word, font in line))
    space_count = len(line) - 1

    if space_count > 0 and not is_last_line:
        space_width = (Config.TEXT_RECT_SIZE_W - total_width) // space_count
    else:
        space_width = line[0][1].getbbox(" ")[2]

    for word, font in line:
        draw.text((x, y), word, font=font, fill="white")
        x += font.getbbox(word)[2] + space_width