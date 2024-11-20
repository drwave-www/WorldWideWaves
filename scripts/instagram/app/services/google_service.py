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

import random
import logging
from googleapiclient.discovery import build
from app.config import Config

def fetch_google_image(query):
    try:
        service = build("customsearch", "v1", developerKey=Config.GOOGLE_API_KEY)
        response = service.cse().list(
            q=query,
            cx=Config.GOOGLE_CX,
            searchType="image",
            imgSize="LARGE",
            num=10
        ).execute()

        items = response.get("items", [])
        if items:
            return random.choice(items)["link"]
    except Exception as e:
        logging.error(f"Error fetching Google image: {e}")
    return None

