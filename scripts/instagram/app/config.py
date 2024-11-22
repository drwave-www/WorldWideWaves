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

import yaml
import logging

class Config:
    @staticmethod
    def load_config():
        try:
            with open("config.yaml", "r") as f:
                return yaml.safe_load(f)
        except Exception as e:
            logging.error(f"Error loading config: {e}")
            return None

    CONFIG = load_config()
    if not CONFIG:
        raise RuntimeError("Configuration file could not be loaded. Exiting.")

    GOOGLE_API_KEY = CONFIG["google_search_api_key"]
    GOOGLE_CX = CONFIG["google_cx"]
    THEMES = CONFIG["themes"]
    LANGUAGES = CONFIG["languages"]
    INSTAGRAM = CONFIG["instagram"]
    OPENAI_API_KEY = CONFIG["openai_api_key"]

    # -------------------------------------------------------------------------

    TEMPLATE_FOLDER = "template"
    USED_TEXTS_FILE = "used_texts.json"
    OUTPUT_FOLDER = "app/static/output"

    FONT_NORMAL = "app/fonts/noto.ttf"
    FONT_BOLD = "app/fonts/noto-bold.ttf"
    TEXT_RECT_SIZE_W = 900
    TEXT_RECT_SIZE_H = 800
    MIN_FONT_SIZE = 18
    MAX_FONT_SIZE = 58
    IMAGE_SIZE = 1080
