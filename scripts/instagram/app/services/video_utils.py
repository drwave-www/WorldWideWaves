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
import wave

import cv2
import numpy as np

from PIL import Image, ImageDraw

from app.config import Config
from app.services.image_utils import draw_bounded_text, get_styled_parts, split_text_in_lines
from app.services.utils import u_num

def generate_voice_for_text(language, text):
    if language not in Config.TTS_CONFIG["languages"]:
        raise ValueError(f"Language '{language}' not supported.")

    engine = Config.TTS_CONFIG["languages"][language]["engine"]
    speaker = Config.TTS_CONFIG["languages"][language].get("default-voice", "default")
    output_file = os.path.join(Config.OUTPUT_FOLDER, f"{u_num()}_audio.wav")

    logging.info(f"Generate voice for language {language} - Output file: {output_file}")

    if engine != "xtts":
        raise ValueError("Only xtts is supported")

    if len(text) >= Config.TTS_CONFIG["languages"][language]["char-limit"]:
        logging.info("Text will not be split")
        split_sentences = True
    else:
        logging.info("Text will be split")
        split_sentences = False

    code = Config.TTS_CONFIG["languages"][language].get("code", language)

    try:
        text = text.replace(".", "|\n") # Bug in xtts-2, pronouncing the '.' in some languages
        Config.tts().tts_to_file(text, split_sentences=split_sentences, speaker=speaker, language=code, file_path=output_file)
    except Exception as e:
        logging.error(f"Error generating voice for language {language}: {e}")
        logging.debug(text)
        raise

    return output_file

def get_audio_length(audio_file_path):
    try:
        with wave.open(audio_file_path, 'rb') as wav_file:
            frames = wav_file.getnframes()
            rate = wav_file.getframerate()
            duration = frames / float(rate)
            logging.info(f"Audio duration: {duration} seconds")
        return duration
    except wave.Error as e:
        logging.error(f"Error reading WAV file: {e}")
        return None

def get_video_writer(image_size, video_path = None):
    if video_path is None:
        video_path = os.path.join(Config.OUTPUT_FOLDER, f"{u_num()}_text_video.mp4")

    logging.info(f"Output file: {video_path}")
    fourcc = cv2.VideoWriter_fourcc(*"H264")
    video_writer = cv2.VideoWriter(video_path, fourcc, Config.VIDEO_FPS, image_size)
    if not video_writer.isOpened():
        raise Exception("Failed to open VideoWriter")

    return video_writer, video_path

def render_progressive_text(format, video_writer, image_size, total_time, end_time, language, text, bold_parts):
    background_path = os.path.join(Config.TEMPLATE_FOLDER, format["FOLDER"], "quote.jpg")
    bg_image = cv2.imread(background_path)
    bg_image = cv2.resize(bg_image, image_size)

    styled_parts = get_styled_parts(text, bold_parts)

    # Find the final font size
    _, _, spaced = Config.get_layout(language)
    font_size = Config.MAX_FONT_SIZE
    if Config.VIDEO_USE_FINAL_FONT_SIZE:
        font_size, _, _, _ = split_text_in_lines(format, language, styled_parts)

    # Split text into individual letters
    letters = list(text)
    total_frames = math.ceil(Config.VIDEO_FPS * total_time)
    frames_per_letter = total_frames // len(letters)
    extra_frames = total_frames - frames_per_letter * len(letters)

    # Track total written frames
    frames_written = 0
    last_frame = None

    for i in range(len(letters) + 1):
        current_text = "".join(letters[:i])

        pil_image = Image.fromarray(cv2.cvtColor(bg_image, cv2.COLOR_BGR2RGB))
        draw = ImageDraw.Draw(pil_image)
        draw_bounded_text(format, language, draw, current_text, bold_parts, font_size)

        cv_frame = cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)
        last_frame = cv_frame

        # Write frames for the current letter
        for _ in range(frames_per_letter):
            video_writer.write(cv_frame)
            frames_written += 1

        # Distribute extra frames across letters
        if extra_frames > 0:
            video_writer.write(cv_frame)
            frames_written += 1
            extra_frames -= 1

    # Write remaining frames if total_time is not exactly matched
    while frames_written < total_frames:
        video_writer.write(last_frame)
        frames_written += 1

    # Add frames for the end time
    for _ in range(math.ceil(Config.VIDEO_FPS * end_time)):
        video_writer.write(last_frame)

def display_static_page(video_writer, image_size, page_path, frames):
    static_frame = cv2.imread(page_path)
    static_frame = cv2.resize(static_frame, image_size)

    for _ in range(frames):
        video_writer.write(static_frame)
