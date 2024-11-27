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
from PIL import ImageFont, features

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

def draw_bounded_title(format, language, title_type, draw, text, font_name):
    min_font_size = Config.MIN_FONT_SIZE
    max_font_size = Config.MAX_FONT_SIZE
    initial_font_size = max_font_size # Start with the initial font size

    max_width = format["AREA"]["WIDTH"]  # Maximum allowed width for the text
    max_lines = 2  # Limit the text to two lines

    best_font = initial_font_size
    best_lines = []

    orientation, direction = Config.get_layout(language)
    pos_start = format["POS"][orientation][title_type]

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
        if orientation == "H":
            x = (format["IMAGE"]["WIDTH"] - line_width) // 2  # Center the text horizontally
            y = pos_start + i * line_height  # Position the text vertically
        else: # V
            x = pos_start - i * line_height
            y = (format["IMAGE"]["HEIGHT"] - line_width) // 2  # Center the text vertically

        write_pillow(draw, x, y, line, best_font, orientation, direction)

    # Calculate total height of the drawn text
    total_height = line_height * len(best_lines)
    return total_height

# -----------------------------------------------------------------------------

def split_japanese_text_vertically(text):
    text = text.replace('\n', '').replace('\r', '')
    punctuation = '、。！？…「」（）【】『』・ー'
    pattern = re.compile(r'(.)([' + re.escape(punctuation) + r']*)')
    split_text = [match.group(1) + match.group(2) for match in pattern.finditer(text)]
    return split_text

def split_by_words(language, text):
    if language == 'ja':  # Japanese specifics
        words = split_japanese_text_vertically(text)
    else:
        words = re.findall(r"[^\s,]+|[,.]", text)  # Split into words and punctuation
    return words

def layout_text(format, language, font_size, styled_parts, orientation, direction):
    lines = []
    current_line = []
    current_width = 0
    total_height = 0

    current_font = Config.normal_font(language, font_size)
    bbox = current_font.getbbox("Ay")
    line_height = bbox[3] if orientation == "H" else bbox[2]
    width_indice = 2 if orientation == "H" else 3
    max_width = format["AREA"]["WIDTH"] if orientation == "H" else format["AREA"]["HEIGHT"]

    for part, current_font in styled_parts:
        current_font = current_font(language, font_size)
        space_width = current_font.getbbox(" ")[width_indice]

        if language == 'ja': # Japanese specifics
            space_width = 0

        # Tokenize words and handle punctuation
        words = split_by_words(language, part)

        for word in words:
            if word in {",", "."}:
                current_width += current_font.getbbox(".")[width_indice]
                if current_line:
                    current_line[-1][0] += word  # Append punctuation to the previous word
                continue
            else:
                if orientation == "H":
                    word_width = current_font.getbbox(word)[width_indice] - current_font.getbbox(word)[width_indice - 2]
                else: # V
                    word_width = sum(current_font.getbbox(char)[width_indice] - current_font.getbbox(char)[width_indice - 2] + 12 for char in word) # +12 is secret

            if current_width + word_width > max_width:
                # Finish the current line
                lines.append(current_line)
                total_height += line_height
                current_line = []
                current_width = 0

            current_line.append([word, current_font])
            current_width += word_width + space_width

    if current_line:
        lines.append(current_line)
        total_height += line_height

    return lines, line_height, total_height

def get_styled_parts(text, bold_parts):
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

    return styled_parts

def split_text_in_lines(format, language, orientation, direction, styled_parts, font_size = Config.MAX_FONT_SIZE):
    total_height = 0
    lines = []
    line_height = 0

    max_height = format["AREA"]["HEIGHT"] if orientation == "H" else format["AREA"]["WIDTH"]

    while font_size >= Config.MIN_FONT_SIZE:
        # Simulate the layout with current font size
        lines, line_height, total_height = layout_text(format, language, font_size, styled_parts, orientation, direction)

        # Check if the total height fits within the rectangle
        if total_height <= max_height:
            logging.debug(f"Selected font size : {font_size} for height : {total_height}")
            break

        # Reduce the font size
        logging.debug(f"Reduce the font size from {font_size} as total height is {total_height} vs {format['AREA']['HEIGHT']}")
        font_size -= 1

    # If text doesn't fit, raise an error
    if font_size < Config.MIN_FONT_SIZE:
        raise ValueError("The text is too large to fit within the bounds.")

    return font_size, total_height, lines, line_height

def draw_bounded_text(format, language, draw, text, bold_parts, font_size = Config.MAX_FONT_SIZE):
    logging.debug(f"LibRAQM available : {features.check_feature(feature='raqm')}")

    # Config
    orientation, direction = Config.get_layout(language)

    # Split with bold
    styled_parts = get_styled_parts(text, bold_parts)

    # Adjust font size to fit within the rectangle
    _, total_height, lines, line_height = split_text_in_lines(format, language, orientation, direction, styled_parts, font_size)

    # Calculate starting position to center the text within the rectangle
    rect_x = (format["IMAGE"]["WIDTH"] - format["AREA"]["WIDTH"]) // 2
    y = (format["IMAGE"]["HEIGHT"] - total_height) // 2

    # debug
    #draw.rectangle((rect_x, y, rect_x + format["AREA"]["WIDTH"], y + total_height), fill='gray')

    if orientation == "V":
        if direction == "RL":
            rect_x = format["IMAGE"]["WIDTH"] - (format["IMAGE"]["WIDTH"] - total_height) // 2
        y = (format["IMAGE"]["HEIGHT"] - format["AREA"]["HEIGHT"]) // 2

    # Draw each line of justified text
    x = rect_x
    for i, line in enumerate(lines):
        write_line(format, language, draw, line, x, y, orientation, direction, is_last_line=(i == len(lines) - 1))
        # debug
        # draw.rectangle((x, y, x + format["AREA"]["WIDTH"], y + line_height), outline='yellow')
        if orientation == "H":
            x = rect_x
            y += line_height
        else: # V
            if direction == "RL":
                x -= line_height
            else:
                x += line_height

def write_line(format, language, draw, line, x, y, orientation, direction, is_last_line=False):
    if len(line) == 0:
        return

    total_width = sum((font.getbbox(word)[2] for word, font in line))
    space_count = len(line) - 1

    if space_count > 0 and not is_last_line:
        space_width = (format["AREA"]["WIDTH"] - total_width) // space_count
    else:
        space_width = line[0][1].getbbox(" ")[2]
        if orientation == "H" and direction == "RL":
            x += format["AREA"]["WIDTH"] - total_width - space_width * space_count

    if language == "ja":
        space_width = 0

    if direction == 'RL' and orientation == "H":
        line.reverse()

    for word, font in line:
        write_pillow(draw, x, y, word, font, orientation, direction)
        step = font.getbbox(word)[2] + space_width
        if orientation == "H":
            x += step
        else: # V
            y += step

def write_pillow(draw, x, y, line, font, orientation, direction):
    anchor = "la"
    pillow_direction = "ltr"
    if orientation == "H" and direction == "RL":
            pillow_direction = "rtl"
    elif orientation == "V": #
        pillow_direction = "ttb"
        anchor = "rt"

    draw.text((x, y), line, font=font, fill="white", anchor=anchor, direction=pillow_direction)