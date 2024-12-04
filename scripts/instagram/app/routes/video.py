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
from app.services.video_service import generate_video
from app.services.video_utils import generate_voice_for_text

video = Blueprint("video", __name__)

@video.route("/video", methods=["POST"])
def __video():
    data = request.json
    language = data["language"]
    page1 = data["page1"]
    page2 = data["page2"]
    bold_parts = data["bold_parts"]
    cover_link = data["cover_link"]
    audio_page1_path = data.get("audio_page1_path")
    audio_page2_path = data.get("audio_page2_path")

    format = data.get("format", "SQUARE")

    try:
        video_link = generate_video(format, language, page1, page2, bold_parts, cover_link, audio_page1_path, audio_page2_path)
        response = {"video": video_link}
        return jsonify(response)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@video.route("/audio", methods=["POST"])
def __audio():
    data = request.json
    language = data["language"]
    pages = {"page1": data.get("page1"), "page2": data.get("page2")}

    output = { "audio": {} }
    try:
        # Process each page
        for page_key, page_content in pages.items():
            if page_content and page_content.strip():
                logging.info(f"Generate audio for page {page_key}")
                audio_path = generate_voice_for_text(language, page_content.strip())
                output["audio"][f"{page_key}"] = audio_path.replace("app/", "", 1)

        if not output:
            raise ValueError("At least one of page1 and page2 must be provided")

    except Exception as e:
        return jsonify({"error": str(e)}), 500

    return jsonify(output)
