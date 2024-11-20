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
from flask import Blueprint, request, jsonify
from app.services.openai_service import get_openai_extract
from app.services.image_service import create_images

generate = Blueprint("generate", __name__)

@generate.route("/generate", methods=["POST"])
def __generate():
    data = request.json
    language = data["language"]
    article = data.get("article")
    format = data.get("format", "SQUARE")
    cover_url = None

    if article:
        cover_url = article.get("cover_url")
        cover_url = None if cover_url and cover_url.strip() == "" else cover_url

    max_retries = 2
    return_article = not article

    for attempt in range(max_retries + 1):
        try:
            logging.info(f"Attempt {attempt + 1} to extract data for language: {language}")
            if not article:
                article = get_openai_extract(language)
        except Exception as e:
            logging.error(f"Error on attempt {attempt + 1}: {e}")
            if attempt < max_retries:
                logging.info(f"Retrying...")
            else:
                logging.error("All retry attempts failed.")
                return jsonify({"openai_error": str(e)}), 500

    try:
        images = create_images(format, language, article, cover_url)
        response = {"images": images}

        if return_article:
            response["article"] = article

        return jsonify(response)
    except Exception as e:
        return jsonify({"error": str(e)}), 500
