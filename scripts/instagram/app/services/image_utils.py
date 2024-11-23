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
import re
from app.config import Config
from PIL import ImageFont

def split_text_into_lines(text, font, max_width):
    words = text.split()
    lines = []
    current_line = ""

    for word in words:
        test_line = f"{current_line} {word}".strip()
        line_width = font.getbbox(test_line)[2] - font.getbbox(test_line)[0]

        if line_width <= max_width:
            current_line = test_line
        else:
            if current_line:
                lines.append(current_line)
            current_line = word

    if current_line:
        lines.append(current_line)

    return lines

def draw_bounded_title(language, draw, text, font_name, y_start):
    initial_font_size = Config.MAX_FONT_SIZE
    min_font_size = Config.MIN_FONT_SIZE
    max_font_size = initial_font_size  # Start with the initial font size

    max_width = Config.TEXT_RECT_SIZE_W  # Maximum allowed width for the text
    max_lines = 2  # Limit the text to two lines

    best_font = None
    best_lines = []

    # Use binary search to find the optimal font size
    while min_font_size <= max_font_size:
        font_size = (min_font_size + max_font_size) // 2
        font = font_name(language, font_size)
        lines = split_text_into_lines(text, font, max_width)

        if len(lines) <= max_lines:
            # Text fits within the constraints; try a larger font size
            best_font = font
            best_lines = lines
            min_font_size = font_size + 1
        else:
            # Text doesn't fit; reduce the font size
            max_font_size = font_size - 1

    # If no suitable font size was found, use the minimum font size
    if best_font is None:
        best_font = ImageFont.truetype(font_name, min_font_size)
        best_lines = split_text_into_lines(text, best_font, max_width)

    # Draw the text on the image
    line_height = best_font.getbbox("Ay")[3] + 10
    for i, line in enumerate(best_lines):
        bbox = best_font.getbbox(line)
        line_width = bbox[2] - bbox[0]
        x = (Config.IMAGE_SIZE - line_width) // 2  # Center the text horizontally
        y = y_start + i * line_height  # Position the text vertically
        draw.text((x, y), line, fill="white", font=best_font)

    # Calculate total height of the drawn text
    total_height = line_height * len(best_lines)
    return total_height


def draw_bounded_text(language, draw, text, bold_parts):
    rect_x = (Config.IMAGE_SIZE - Config.TEXT_RECT_SIZE_W) // 2

    # Load the text and fonts
    font_size = Config.MAX_FONT_SIZE
    font = Config.normal_font(language, font_size)

    # Split text into parts, tagging bold sections
    styled_parts = []
    remaining_text = text
    for bold_part in bold_parts:
        if bold_part in remaining_text:
            # Split around the bold part
            before, bold, remaining_text = remaining_text.partition(bold_part)
            if before.strip():
                styled_parts.append((before.strip(), Config.normal_font))
            if bold.strip():
                styled_parts.append((bold.strip(), Config.bold_font))
    # Add any remaining text
    if remaining_text.strip():
        styled_parts.append((remaining_text.strip(), Config.normal_font))

    # Adjust font size to fit within the rectangle
    total_height = 0
    lines = []
    while font_size >= Config.MIN_FONT_SIZE:
        # Simulate the layout with current font size
        lines, total_height = layout_text(language, font_size, styled_parts)

        # Check if the total height fits within the rectangle
        if total_height <= Config.TEXT_RECT_SIZE_H:
            break

        # Reduce the font size
        logging.debug(f"Reduce the font size from {font_size} as total height is {total_height} vs {Config.TEXT_RECT_SIZE_H}")
        font_size -= 1

    # If text doesn't fit, raise an error
    if font_size < Config.MIN_FONT_SIZE:
        raise ValueError("The text is too large to fit within the bounds.")

    # Calculate starting position to center the text within the rectangle
    y = ((Config.IMAGE_SIZE - total_height) // 2) - font.getbbox("Ay")[3]

    # Draw each line of justified text
    for i, line in enumerate(lines):
        x = rect_x
        justify_line(draw, line, x, y, is_last_line=(i == len(lines) - 1))
        y += font.getbbox("Ay")[3]  # Line height

def layout_text(language, font_size, styled_parts):
    lines = []
    current_line = []
    current_width = 0
    total_height = 0

    current_font = Config.normal_font(language, font_size)
    for part, current_font in styled_parts:
        current_font = current_font(language, font_size)
        space_width = current_font.getbbox(" ")[2]

        # Tokenize words and handle punctuation
        words = re.findall(r"[^\s,]+|[,.]", part)  # Split into words and punctuation
        for word in words:
            if word == ",":
                current_width += current_font.getbbox(",")[2]
                current_line[-1][0] += ","  # Append comma to the previous word
                continue
            elif word == ".":
                current_width += current_font.getbbox(".")[2]
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
    if len(line) == 0:
        return

    total_width = sum((font.getbbox(word)[2] for word, font in line))
    space_count = len(line) - 1

    if space_count > 0 and not is_last_line:
        space_width = (Config.TEXT_RECT_SIZE_W - total_width) // space_count
    else:
        space_width = line[0][1].getbbox(" ")[2]

    for word, font in line:
        draw.text((x, y), word, font=font, fill="white")
        x += font.getbbox(word)[2] + space_width
