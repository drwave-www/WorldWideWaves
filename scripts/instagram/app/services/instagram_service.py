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

import requests
import logging
import time
from app.config import Config

def is_token_valid(access_token):
    try:
        url = "https://graph.facebook.com/debug_token"
        params = {
            "input_token": access_token,
            "access_token": f"{Config.INSTAGRAM['app_id']}|{Config.INSTAGRAM['app_secret']}"  # App Access Token
        }
        response = requests.get(url, params=params)
        response_data = response.json()

        if "data" in response_data and response_data["data"].get("is_valid"):
            expires_at = response_data["data"].get("expires_at", None)
            return True, expires_at
        return False, None
    except Exception as e:
        logging.error(f"Error validating token: {e}")
        return False, None

def refresh_token(language, access_token):
    try:
        # Check if token is valid and not expired
        is_valid, expires_at = is_token_valid(access_token)
        if is_valid:
            # If expiration time exists and is more than 24 hours away, skip refresh
            if expires_at and (expires_at - time.time()) > 24 * 60 * 60:
                logging.info("Token is still valid and does not need to be refreshed.")
                return access_token

        # Refresh the token
        logging.info("Refreshing the token...")
        url = "https://graph.instagram.com/refresh_access_token"
        params = {
            "grant_type": "ig_refresh_token",
            "access_token": access_token
        }
        response = requests.get(url, params=params)
        response_data = response.json()

        if "access_token" not in response_data:
            raise Exception(f"Failed to refresh token: {response_data}")

        # Cache the token and expiry time
        refreshed_token = response_data["access_token"]
        Config.update_token(language, refreshed_token)

        logging.info("Token refreshed successfully.")
        return refreshed_token
    except Exception as e:
        logging.error(f"Error refreshing token: {e}")
        raise

# -----------------------------------------------------------------------------

def create_media_container(image_url, caption, is_carousel_item, ig_user_id, access_token):
    url = f"https://graph.instagram.com/{ig_user_id}/media"
    params = {
        "image_url": image_url,
        "caption": caption,
        "is_carousel_item": is_carousel_item,
        "access_token": access_token
    }
    response = requests.post(url, params=params)
    response_data = response.json()

    if "id" not in response_data:
        raise Exception(f"Failed to create media container: {response_data}")

    return response_data["id"]

def create_carousel_container(children_ids, caption, ig_user_id, access_token):
    url = f"https://graph.instagram.com/{ig_user_id}/media"
    params = {
        "media_type": "CAROUSEL",
        "children": ",".join(children_ids),
        "caption": caption,
        "access_token": access_token
    }
    response = requests.post(url, params=params)
    response_data = response.json()

    if "id" not in response_data:
        raise Exception(f"Failed to create carousel container: {response_data}")

    return response_data["id"]

def publish_carousel(container_id, ig_user_id, access_token):
    url = f"https://graph.instagram.com/{ig_user_id}/media_publish"
    params = {
        "creation_id": container_id,
        "access_token": access_token
    }
    response = requests.post(url, params=params)
    response_data = response.json()

    if "id" not in response_data:
        raise Exception(f"Failed to publish carousel: {response_data}")

    return response_data

# -----------------------------------------------------------------------------

def create_and_publish_carousel(language, image_urls, caption, account):

    # Get API connection informations
    ig_user_id = Config.LANGUAGES[language]["accounts"][account]["account_id"]
    access_token = Config.LANGUAGES[language]["accounts"][account]["access_token"]

    # Refresh the token if needed
    access_token = refresh_token(language, access_token)

    try:
        # Step 1: Create individual item containers
        container_ids = []
        for image_url in image_urls:
            container_id = create_media_container(
                image_url=image_url,
                caption="",
                is_carousel_item=True,
                ig_user_id=ig_user_id,
                access_token=access_token
            )
            container_ids.append(container_id)
            logging.info(f"Created container for image {image_url}: {container_id}")

        # Step 2: Create carousel container
        carousel_container_id = create_carousel_container(
            children_ids=container_ids,
            caption=caption,
            ig_user_id=ig_user_id,
            access_token=access_token
        )
        logging.info(f"Created carousel container: {carousel_container_id}")

        # Step 3: Publish the carousel
        publish_response = publish_carousel(
            container_id=carousel_container_id,
            ig_user_id=ig_user_id,
            access_token=access_token
        )
        logging.info(f"Published carousel successfully: {publish_response['id']}")
        return publish_response

    except Exception as e:
        logging.error(f"Error during carousel publishing workflow: {e}")
        raise