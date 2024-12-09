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
# limitations under the License.xx
#

import logging
from flask import Blueprint, request, jsonify
from app.services.instagram_service import create_and_publish_carousel, create_and_publish_video
from app.services.utils import add_used_text

post = Blueprint("post", __name__)

@post.route("/post", methods=["POST"])
def __post():
    data = request.json

    # Validate input data
    if not data or "language" not in data or "title" not in data or ("images" not in data and "video_url" not in data) or "caption" not in data or "accounts" not in data:
        return jsonify({"error": "Missing required fields: language, title, images, hashtags, accounts"}), 400

    language = data["language"]
    title = f"{data['title']} by {data['author']}"
    caption = data["caption"]
    accounts = data["accounts"]

    images = data.get("images")
    video_url = data.get("video_url")

    try:
        # Iterate over each account
        results = {}
        status = "complete"
        for account in accounts:
            try:
                logging.info(f"Prepare to post on account {account}")

                # Publish the carousel for this account
                if images:
                    try:
                        response = create_and_publish_carousel(language, images, caption, account)
                        logging.info(f"Carousel Post ID: {response['id']}")
                        results[account] = {"success": True}
                    except Exception as e:
                        logging.error(f"Failed to publish carousel: {e}")
                        results[account] = {"error": str(e)}
                        status = "error"

                elif video_url:
                    try:
                        response = create_and_publish_video(language, video_url, caption, account)
                        logging.info(f"Video Post ID: {response['id']}")
                        results[account] = {"success": True}
                    except Exception as e:
                        logging.error(f"Failed to publish video: {e}")
                        results[account] = {"error": str(e)}
                        status = "error"

                else:
                    logging.error(f"No carousel not video submitted")

            except Exception as account_error:
                logging.error(f"Error for account {account}: {account_error}")
                results[account] = {"error": str(account_error)}
                status = "error"

        # Save used text
        if status != "error":
            add_used_text(language, title)
        return jsonify({"status": status, "results": results}), (500 if (status == "error") else 200)

    except Exception as e:
        logging.error(f"Critical error: {e}")
        return jsonify({"error": str(e)}), 500