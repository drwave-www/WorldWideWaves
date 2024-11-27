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

from flask import Blueprint, request, jsonify
from app.services.video_service import generate_video

video = Blueprint("video", __name__)

@video.route("/video", methods=["POST"])
def __video():
    data = request.json
    language = data["language"]
    page1 = data["page1"]
    page2 = data["page2"]
    bold_parts = data["bold_parts"]
    cover_link = data["cover_link"]
    format = data.get("format", "SQUARE")

    try:
        video_link = generate_video(format, language, page1, page2, bold_parts, cover_link)
        response = {"video": video_link}
        return jsonify(response)
    except Exception as e:
        return jsonify({"error": str(e)}), 500
