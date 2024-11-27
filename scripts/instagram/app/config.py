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
import yaml
import logging
from PIL import ImageFont

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

    VIDEO_USE_FINAL_FONT_SIZE = True # If true, directly fit the text at the target size in the video

    TEMPLATE_FOLDER = "template"
    USED_TEXTS_FILE = "used_texts.json"
    OUTPUT_FOLDER = "app/static/output"

    COVER_MARGIN_WITH_TEXT = 40

    MIN_FONT_SIZE = 15
    MAX_FONT_SIZE = 90

    FORMATS = {
        "SQUARE": {
            "FOLDER": "SQUARE",
            "IMAGE": {
                "WIDTH": 1080,
                "HEIGHT": 1080,
            },
            "AREA": {
                "WIDTH": 850,
                "HEIGHT": 850,
            },
            "POS": {
                "H" : {
                    "TITLE": 120,
                    "AUTHOR": 750,
                },
                "V" : {
                    "TITLE": 200,
                    "AUTHOR": 950
                }
            },
            "MAGICS": {
                "ja": 10
            }
        },
        "RECT_POST": {
            "FOLDER": "RECT_POST",
            "IMAGE": {
                "WIDTH": 1080,
                "HEIGHT": 1350,
            },
            "AREA": {
                "WIDTH": 850,
                "HEIGHT": 1120,
            },
            "POS": {
                "H": {
                    "TITLE": 150,
                    "AUTHOR": 1000,
                },
                "V": {
                    "TITLE": 200,
                    "AUTHOR": 950
                }
            },
            "MAGICS": {
                "ja": 13
            }
        },
        "RECT_REEL": {
            "FOLDER": "RECT_REEL",
            "IMAGE": {
                "WIDTH": 1080,
                "HEIGHT": 1920,
            },
            "AREA": {
                "WIDTH": 850,
                "HEIGHT": 1690,
            },
            "POS": {
                "H": {
                    "TITLE": 250,
                    "AUTHOR": 1450,
                },
                "V": {
                    "TITLE": 200,
                    "AUTHOR": 950
                }
            },
            "MAGICS": {
                "ja": 20
            }
        }
    }

    TPL_FONT_NORMAL = "app/fonts/noto"
    TPL_FONT_BOLD = "app/fonts/noto-bold"

    font_cache = {}

    @staticmethod
    def update_token(language, account, access_token):
        logging.info(f"Updating token for {language} in configuration")
        Config.LANGUAGES[language]["accounts"][account]["access_token"] = access_token
        try:
            with open("config.yaml", "w") as f:
                yaml.safe_dump(Config.CONFIG, f)
            logging.info(f"Access token for account '{account}' in language '{language}' updated successfully.")
        except Exception as e:
            logging.error(f"Failed to update token: {e}")
            raise

    @staticmethod
    def get_font_file(base_font, language):
        specific_font = f"{base_font}-{language}.ttf"
        default_font = f"{base_font}.ttf"
        return specific_font if os.path.exists(specific_font) else default_font

    @staticmethod
    def load_font(font_name, size):
        cache_key = (font_name, size)
        if cache_key not in Config.font_cache:
            Config.font_cache[cache_key] = ImageFont.truetype(font_name, size=size)
        return Config.font_cache[cache_key]

    @staticmethod
    def normal_font(language, size):
        font_name = Config.get_font_file(Config.TPL_FONT_NORMAL, language)
        return Config.load_font(font_name, size)

    @staticmethod
    def bold_font(language, size):
        font_name = Config.get_font_file(Config.TPL_FONT_BOLD, language)
        return Config.load_font(font_name, size)

    @classmethod
    def get_layout(cls, language):
        layout_parts = Config.LANGUAGES[language]["layout"].split('-')
        orientation = layout_parts[0]
        direction = layout_parts[1]
        assert orientation in ("H", "V"), f"Invalid orientation '{orientation}'. Must be 'H' or 'V'."
        assert direction in ("RL", "LR"), f"Invalid direction '{direction}'. Must be 'RL' or 'LR'."

        if orientation == "V" and direction == "LR":
            raise "layout V-LR is not supported"

        return orientation, direction
